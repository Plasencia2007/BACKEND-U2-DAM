package pe.upeu.ecorecicla.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Ruta del dia con sus puntos ordenados por secuencia y su estado ABIERTA / CERRADA. */
public record RutaDto(
        Long id,
        LocalDate fecha,
        String estado,
        String operario,
        String supervisor,
        int totalPuntos,
        int puntosAtendidos,
        List<RutaPuntoDto> puntos) {

    public static final String ABIERTA = "ABIERTA";
    public static final String CERRADA = "CERRADA";
}
