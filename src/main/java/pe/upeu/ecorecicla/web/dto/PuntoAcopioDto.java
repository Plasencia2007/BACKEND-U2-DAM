package pe.upeu.ecorecicla.web.dto;

import pe.upeu.ecorecicla.dominio.PuntoAcopio;

public record PuntoAcopioDto(Long id, String razonSocial, String direccion, String zona, String materialHabitual) {

    public static PuntoAcopioDto de(PuntoAcopio p) {
        return new PuntoAcopioDto(p.getId(), p.getRazonSocial(), p.getDireccion(), p.getZona(), p.getMaterialHabitual());
    }
}
