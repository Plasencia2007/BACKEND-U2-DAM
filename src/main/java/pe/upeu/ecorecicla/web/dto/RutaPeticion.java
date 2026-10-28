package pe.upeu.ecorecicla.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** Planificacion de una ruta: puntos en el orden de visita. */
public record RutaPeticion(
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotNull(message = "El operario es obligatorio") Long operarioId,
        Long supervisorId,
        @NotEmpty(message = "La ruta debe tener al menos un punto") List<Long> puntoIds) {
}
