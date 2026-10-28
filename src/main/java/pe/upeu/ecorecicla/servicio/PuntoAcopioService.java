package pe.upeu.ecorecicla.servicio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upeu.ecorecicla.dominio.PuntoAcopio;
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository;
import pe.upeu.ecorecicla.web.dto.PuntoAcopioPeticion;

/** RF-B03: padron de puntos de acopio. */
@Service
public class PuntoAcopioService {

    private final PuntoAcopioRepository puntos;

    public PuntoAcopioService(PuntoAcopioRepository puntos) {
        this.puntos = puntos;
    }

    @Transactional(readOnly = true)
    public Page<PuntoAcopio> listar(String zona, int pagina, int tamanio) {
        var pageable = Paginacion.de(pagina, tamanio, Sort.by("zona", "razonSocial"));
        if (zona == null || zona.isBlank()) {
            return puntos.findAll(pageable);
        }
        return puntos.findByZonaIgnoreCase(zona.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public PuntoAcopio obtener(Long id) {
        return puntos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el punto de acopio " + id));
    }

    @Transactional
    public PuntoAcopio crear(PuntoAcopioPeticion p) {
        return puntos.save(new PuntoAcopio(p.razonSocial().trim(), p.direccion().trim(), p.zona().trim(),
                p.materialHabitual().trim()));
    }
}
