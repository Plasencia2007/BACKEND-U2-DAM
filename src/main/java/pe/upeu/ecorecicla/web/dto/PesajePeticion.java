package pe.upeu.ecorecicla.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Registro de un pesaje. El cliente NUNCA envia el peso neto: lo calcula el servicio (RN-03).
 * "claveCliente" es opcional y permite reenviar un pendiente sin duplicarlo.
 */
public record PesajePeticion(
        @NotNull(message = "La ruta es obligatoria") Long rutaId,
        @NotBlank(message = "El material es obligatorio") @Size(max = 40, message = "Maximo 40 caracteres") String material,
        @NotNull(message = "El peso bruto es obligatorio") @Positive(message = "El peso bruto debe ser mayor que cero")
        @DecimalMax(value = "100000", message = "El peso bruto es demasiado alto") Double pesoBruto,
        @NotNull(message = "La tara es obligatoria") @PositiveOrZero(message = "La tara no puede ser negativa") Double tara,
        @Size(max = 250, message = "Maximo 250 caracteres") String observacion,
        @DecimalMin(value = "0", message = "La humedad no puede ser negativa")
        @DecimalMax(value = "100", message = "La humedad debe estar entre 0 y 100 %") Double humedadEstimada,
        @Size(max = 64, message = "Maximo 64 caracteres") String claveCliente) {
}
