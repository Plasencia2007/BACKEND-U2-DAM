package pe.upeu.ecorecicla.web.dto;

import pe.upeu.ecorecicla.dominio.Pesaje;

import java.time.LocalDateTime;

/** Pesaje tal como lo publica el servicio; incluye el neto calculado por el servidor. */
public record PesajeDto(
        Long id,
        Long puntoId,
        Long rutaId,
        LocalDateTime fechaHora,
        String material,
        double pesoBruto,
        double tara,
        double pesoNeto,
        String observacion,
        String operario,
        String estado,
        String motivoAnulacion,
        String claveCliente) {

    public static final String REGISTRADO = "REGISTRADO";
    public static final String ANULADO = "ANULADO";

    public static PesajeDto de(Pesaje p) {
        return new PesajeDto(p.getId(), p.getPunto().getId(), p.getRuta().getId(), p.getFechaHora(),
                p.getMaterial(), p.getPesoBruto(), p.getTara(), p.getPesoNeto(), p.getObservacion(),
                p.getOperario().getUsername(), p.isAnulado() ? ANULADO : REGISTRADO,
                p.getMotivoAnulacion(), p.getClaveCliente());
    }
}
