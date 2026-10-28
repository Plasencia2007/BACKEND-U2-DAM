package pe.upeu.ecorecicla.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo de omision de un punto o de anulacion de un pesaje. */
public record MotivoPeticion(
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 250, message = "Maximo 250 caracteres") String motivo) {
}
