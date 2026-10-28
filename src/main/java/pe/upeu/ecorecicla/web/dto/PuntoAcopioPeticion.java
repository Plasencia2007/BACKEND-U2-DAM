package pe.upeu.ecorecicla.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PuntoAcopioPeticion(
        @NotBlank(message = "La razon social es obligatoria") @Size(max = 150, message = "Maximo 150 caracteres") String razonSocial,
        @NotBlank(message = "La direccion es obligatoria") @Size(max = 200, message = "Maximo 200 caracteres") String direccion,
        @NotBlank(message = "La zona es obligatoria") @Size(max = 40, message = "Maximo 40 caracteres") String zona,
        @NotBlank(message = "El material habitual es obligatorio") @Size(max = 40, message = "Maximo 40 caracteres") String materialHabitual) {
}
