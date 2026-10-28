package pe.upeu.ecorecicla;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.upeu.ecorecicla.config.JwtService;
import pe.upeu.ecorecicla.seguridad.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica el contrato del API contra la base H2 con los datos semilla. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EcoApiIntegracionTest {

    private static final String API = "/api/v1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarios;

    @Value("${ecorecicla.jwt.secreto}")
    private String secreto;

    private String token(String usuario) throws Exception {
        String cuerpo = mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + usuario + "\",\"password\":\"Eco2026*\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(cuerpo).get("token").asText();
    }

    private JsonNode rutaDelDia(String token) throws Exception {
        String cuerpo = mvc.perform(get(API + "/rutas/hoy").header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo);
    }

    @Test
    void loginDevuelveTokenYPrivilegios() throws Exception {
        mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"supervisor1\",\"password\":\"Eco2026*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiraEn").value(3600))
                .andExpect(jsonPath("$.rol").value("SUPERVISOR"))
                .andExpect(jsonPath("$.privilegios").isArray());
    }

    @Test
    void credencialesIncorrectasResponden401() throws Exception {
        mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operario1\",\"password\":\"otra\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(API + "/auth/login"));
    }

    @Test
    void sinTokenOAlteradoOVencidoResponde401() throws Exception {
        mvc.perform(get(API + "/rutas/hoy")).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/rutas/hoy").header("Authorization", token("operario1") + "x"))
                .andExpect(status().isUnauthorized());
        // Token firmado con la clave correcta pero vencido hace un minuto.
        JwtService vencido = new JwtService(secreto, -1);
        String t = vencido.generar(usuarios.findByUsername("operario1").orElseThrow());
        mvc.perform(get(API + "/rutas/hoy").header("Authorization", "Bearer " + t))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("El token ha vencido"));
    }

    @Test
    void sinPrivilegioResponde403() throws Exception {
        JsonNode ruta = rutaDelDia(token("operario1"));
        mvc.perform(post(API + "/rutas/" + ruta.get("id").asLong() + "/cierre")
                        .header("Authorization", token("operario1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(post(API + "/puntos/1/pesajes").header("Authorization", token("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rutaId\":1,\"material\":\"Papel\",\"pesoBruto\":10,\"tara\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listadoDePuntosUsaElEnvoltorioDePaginacion() throws Exception {
        mvc.perform(get(API + "/puntos?zona=Sur&pagina=0&tamanio=3").header("Authorization", token("operario1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(3))
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.ultima").value(false));
    }

    @Test
    void registrarCalculaElNetoYResponde201() throws Exception {
        String t = token("operario1");
        JsonNode ruta = rutaDelDia(t);
        long puntoSinPesar = ruta.get("puntos").get(4).get("id").asLong();
        mvc.perform(post(API + "/puntos/" + puntoSinPesar + "/pesajes").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rutaId\":" + ruta.get("id") + ",\"material\":\"Vidrio\",\"pesoBruto\":80.5,\"tara\":5.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pesoNeto").value(75.0))
                .andExpect(jsonPath("$.estado").value("REGISTRADO"));
    }

    @Test
    void netoNoPositivoResponde400ConElCampo() throws Exception {
        String t = token("operario1");
        JsonNode ruta = rutaDelDia(t);
        mvc.perform(post(API + "/puntos/" + ruta.get("puntos").get(0).get("id") + "/pesajes").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rutaId\":" + ruta.get("id") + ",\"material\":\"Papel\",\"pesoBruto\":10,\"tara\":12}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.tara").exists());
    }

    @Test
    void rutaCerradaResponde409() throws Exception {
        String t = token("operario1");
        String ayer = java.time.LocalDate.now().minusDays(1).toString();
        String cuerpo = mvc.perform(get(API + "/rutas/hoy?fecha=" + ayer).header("Authorization", t))
                .andReturn().getResponse().getContentAsString();
        JsonNode cerrada = json.readTree(cuerpo);
        assertThat(cerrada.get("estado").asText()).isEqualTo("CERRADA");
        mvc.perform(post(API + "/puntos/" + cerrada.get("puntos").get(0).get("id") + "/pesajes").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rutaId\":" + cerrada.get("id") + ",\"material\":\"Papel\",\"pesoBruto\":10,\"tara\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void operarioNoCorrigePesajeAjeno() throws Exception {
        String t = token("operario1");
        JsonNode ruta = rutaDelDia(t);
        String cuerpo = mvc.perform(get(API + "/rutas/" + ruta.get("id") + "/pesajes").header("Authorization", t))
                .andReturn().getResponse().getContentAsString();
        long ajeno = 0;
        for (JsonNode p : json.readTree(cuerpo).get("contenido")) {
            if (p.get("operario").asText().equals("supervisor1")) {
                ajeno = p.get("id").asLong();
            }
        }
        mvc.perform(put(API + "/pesajes/" + ajeno).header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"material\":\"Plastico\",\"pesoBruto\":50,\"tara\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cierreIndicaCuantosPuntosFaltanYSeCompletaCuandoNoFaltaNinguno() throws Exception {
        String op = token("operario1");
        String sup = token("supervisor1");
        JsonNode ruta = rutaDelDia(op);
        // Se pesan dos de los tres puntos pendientes (posiciones 3 y 4 de la ruta).
        for (int i = 3; i <= 4; i++) {
            mvc.perform(post(API + "/puntos/" + ruta.get("puntos").get(i).get("id") + "/pesajes").header("Authorization", op)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rutaId\":" + ruta.get("id") + ",\"material\":\"Carton\",\"pesoBruto\":20,\"tara\":2}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post(API + "/rutas/" + ruta.get("id") + "/cierre").header("Authorization", sup))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("falta 1 punto")));
        // Con el ultimo punto omitido (motivo registrado) la ruta ya puede cerrarse.
        mvc.perform(post(API + "/rutas/" + ruta.get("id") + "/puntos/" + ruta.get("puntos").get(5).get("id") + "/omision")
                        .header("Authorization", op).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Empresa cerrada\"}"))
                .andExpect(status().isOk());
        mvc.perform(post(API + "/rutas/" + ruta.get("id") + "/cierre").header("Authorization", sup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));
    }

    @Test
    void anularMarcaSinBorrarYCierreExigeTodosLosPuntos() throws Exception {
        String sup = token("supervisor1");
        JsonNode ruta = rutaDelDia(sup);
        mvc.perform(post(API + "/rutas/" + ruta.get("id") + "/cierre").header("Authorization", sup))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("faltan 3 puntos")));
        mvc.perform(delete(API + "/pesajes/1?motivo=Error de digitacion").header("Authorization", sup))
                .andExpect(status().isConflict()); // pesaje 1 pertenece a la ruta cerrada de ayer (RN-01)
        long pesajeDeHoy = 9;
        mvc.perform(delete(API + "/pesajes/" + pesajeDeHoy + "?motivo=Error").header("Authorization", sup))
                .andExpect(status().isNoContent());
        mvc.perform(get(API + "/pesajes/" + pesajeDeHoy).header("Authorization", sup))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));
    }
}
