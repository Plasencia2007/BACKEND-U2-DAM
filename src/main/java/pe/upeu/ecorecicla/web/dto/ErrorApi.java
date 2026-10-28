package pe.upeu.ecorecicla.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Formato de error comun de todo el servicio (seccion 4.2). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorApi(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors) {
}
