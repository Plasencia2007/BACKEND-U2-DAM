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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SC-C: la humedad estimada viaja en el contrato, se valida (0 a 100) y viene en los datos semilla. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HumedadEstimadaTest {

    private static final String API = "/api/v1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String token() throws Exception {
        String cuerpo = mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operario1\",\"password\":\"Eco2026*\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(cuerpo).get("token").asText();
    }

    @Test
    void registraYCorrigeLaHumedad() throws Exception {
        String t = token();
        JsonNode ruta = json.readTree(mvc.perform(get(API + "/rutas/hoy").header("Authorization", t))
                .andReturn().getResponse().getContentAsString());
        long punto = ruta.get("puntos").get(5).get("id").asLong();
        String creado = mvc.perform(post(API + "/puntos/" + punto + "/pesajes").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rutaId\":" + ruta.get("id") + ",\"material\":\"Carton\",\"pesoBruto\":60,\"tara\":4,\"humedadEstimada\":14.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.humedadEstimada").value(14.5))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creado).get("id").asLong();
        mvc.perform(put(API + "/pesajes/" + id).header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"material\":\"Carton\",\"pesoBruto\":60,\"tara\":4,\"humedadEstimada\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.humedadEstimada").value(8.0));
    }

    @Test
    void humedadFueraDeRangoResponde400ConElCampo() throws Exception {
        mvc.perform(post(API + "/puntos/1/pesajes").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rutaId\":2,\"material\":\"Papel\",\"pesoBruto\":10,\"tara\":1,\"humedadEstimada\":120}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.humedadEstimada").exists());
    }

    @Test
    void losPesajesSemillaTraenHumedad() throws Exception {
        mvc.perform(get(API + "/pesajes/1").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.humedadEstimada").isNumber());
    }
}
