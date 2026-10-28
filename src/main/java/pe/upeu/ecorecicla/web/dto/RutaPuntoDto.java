package pe.upeu.ecorecicla.web.dto;

/** Punto de acopio dentro de una ruta: "id" es el id del punto de acopio. */
public record RutaPuntoDto(
        Long id,
        String razonSocial,
        String direccion,
        String zona,
        String materialHabitual,
        int secuencia,
        boolean atendido,
        boolean omitido,
        String motivoOmision) {
}
