package pe.upeu.ecorecicla;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SC-B: el AUDITOR lee todo y no puede escribir nada. El 403 lo da SecurityConfig por privilegio. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditorTest {

    private static final String API = "/api/v1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String token() throws Exception {
        String cuerpo = mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"auditor1\",\"password\":\"Eco2026*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("AUDITOR"))
                .andExpect(jsonPath("$.privilegios.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(cuerpo).get("token").asText();
    }

    @Test
    void auditorLeeLaRutaLosPuntosYLosPesajes() throws Exception {
        String t = token();
        String cuerpo = mvc.perform(get(API + "/rutas/hoy").header("Authorization", t))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode ruta = json.readTree(cuerpo);
        mvc.perform(get(API + "/puntos").header("Authorization", t)).andExpect(status().isOk());
        mvc.perform(get(API + "/rutas/" + ruta.get("id") + "/pesajes").header("Authorization", t)).andExpect(status().isOk());
        mvc.perform(get(API + "/puntos/1/pesajes").header("Authorization", t)).andExpect(status().isOk());
    }

    @Test
    void auditorNoPuedeEscribirNada() throws Exception {
        String t = token();
        mvc.perform(post(API + "/puntos/1/pesajes").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rutaId\":2,\"material\":\"Papel\",\"pesoBruto\":10,\"tara\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(API + "/pesajes/9").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"material\":\"Papel\",\"pesoBruto\":10,\"tara\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(API + "/pesajes/9?motivo=x").header("Authorization", t)).andExpect(status().isForbidden());
        mvc.perform(post(API + "/rutas/2/cierre").header("Authorization", t)).andExpect(status().isForbidden());
        mvc.perform(post(API + "/puntos").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"X\",\"direccion\":\"Y\",\"zona\":\"Z\",\"materialHabitual\":\"Papel\"}"))
                .andExpect(status().isForbidden());
    }
}
