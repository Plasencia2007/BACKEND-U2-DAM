package pe.upeu.ecorecicla.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upeu.ecorecicla.dominio.PuntoAcopio;
import pe.upeu.ecorecicla.dominio.Ruta;
import pe.upeu.ecorecicla.dominio.RutaPunto;
import pe.upeu.ecorecicla.repositorio.PesajeRepository;
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository;
import pe.upeu.ecorecicla.repositorio.RutaRepository;
import pe.upeu.ecorecicla.seguridad.Usuario;
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado;
import pe.upeu.ecorecicla.seguridad.UsuarioRepository;
import pe.upeu.ecorecicla.web.dto.RutaDto;
import pe.upeu.ecorecicla.web.dto.RutaPeticion;
import pe.upeu.ecorecicla.web.dto.RutaPuntoDto;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** RF-B02 (ruta del dia) y RF-B06 (cierre de ruta, regla RN-04). */
@Service
public class RutaService {

    private final RutaRepository rutas;
    private final PuntoAcopioRepository puntos;
    private final PesajeRepository pesajes;
    private final UsuarioRepository usuarios;

    public RutaService(RutaRepository rutas, PuntoAcopioRepository puntos, PesajeRepository pesajes,
                       UsuarioRepository usuarios) {
        this.rutas = rutas;
        this.puntos = puntos;
        this.pesajes = pesajes;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public RutaDto rutaDelDia(UsuarioAutenticado usuario, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        Ruta ruta = rutas.buscarAsignadas(dia, usuario.id()).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No tiene una ruta asignada para el " + dia));
        return aDto(ruta);
    }

    @Transactional
    public RutaDto crear(RutaPeticion peticion) {
        Usuario operario = usuarios.findById(peticion.operarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el operario " + peticion.operarioId()));
        Usuario supervisor = peticion.supervisorId() == null ? null : usuarios.findById(peticion.supervisorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el supervisor " + peticion.supervisorId()));
        if (new HashSet<>(peticion.puntoIds()).size() != peticion.puntoIds().size()) {
            throw new ValidacionDeNegocioException("puntoIds", "Un punto no puede repetirse en la ruta");
        }
        Ruta ruta = new Ruta(peticion.fecha(), operario, supervisor);
        int secuencia = 1;
        for (Long puntoId : peticion.puntoIds()) {
            PuntoAcopio punto = puntos.findById(puntoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el punto de acopio " + puntoId));
            ruta.agregarPunto(punto, secuencia++);
        }
        return aDto(rutas.save(ruta));
    }

    /** Registra el motivo por el que no se peso un punto (cuenta para RN-04). */
    @Transactional
    public RutaDto omitirPunto(Long rutaId, Long puntoId, String motivo, UsuarioAutenticado usuario) {
        Ruta ruta = buscar(rutaId);
        exigirRutaAbierta(ruta);
        exigirAsignacion(ruta, usuario);
        RutaPunto rp = ruta.getPuntos().stream()
                .filter(p -> p.getPunto().getId().equals(puntoId)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El punto " + puntoId + " no pertenece a la ruta"));
        rp.omitir(motivo.trim());
        return aDto(ruta);
    }

    /** RN-04: la ruta no se cierra mientras algun punto no tenga pesaje vigente ni motivo de omision. */
    @Transactional
    public RutaDto cerrar(Long rutaId) {
        Ruta ruta = buscar(rutaId);
        exigirRutaAbierta(ruta);
        Set<Long> atendidos = pesajes.puntosAtendidos(ruta.getId());
        long faltantes = ruta.getPuntos().stream()
                .filter(rp -> !atendidos.contains(rp.getPunto().getId()) && !rp.estaOmitido())
                .count();
        if (faltantes > 0) {
            throw new ConflictoDeNegocioException("No se puede cerrar la ruta: faltan " + faltantes
                    + (faltantes == 1 ? " punto" : " puntos") + " sin pesaje ni motivo de omision");
        }
        ruta.cerrar();
        return aDto(ruta);
    }

    Ruta buscar(Long rutaId) {
        return rutas.findById(rutaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la ruta " + rutaId));
    }

    /** RN-01: sobre una ruta cerrada no se registra, corrige ni anula. */
    static void exigirRutaAbierta(Ruta ruta) {
        if (ruta.isCerrada()) {
            throw new ConflictoDeNegocioException("La ruta del " + ruta.getFecha() + " ya esta cerrada");
        }
    }

    static void exigirAsignacion(Ruta ruta, UsuarioAutenticado usuario) {
        if (!ruta.estaAsignadaA(usuario.id())) {
            throw new OperacionNoPermitidaException("La ruta no esta asignada a este usuario");
        }
    }

    RutaDto aDto(Ruta ruta) {
        Set<Long> atendidos = ruta.getId() == null ? Set.of() : pesajes.puntosAtendidos(ruta.getId());
        List<RutaPuntoDto> lista = ruta.getPuntos().stream()
                .map(rp -> new RutaPuntoDto(
                        rp.getPunto().getId(),
                        rp.getPunto().getRazonSocial(),
                        rp.getPunto().getDireccion(),
                        rp.getPunto().getZona(),
                        rp.getPunto().getMaterialHabitual(),
                        rp.getSecuencia(),
                        atendidos.contains(rp.getPunto().getId()),
                        rp.estaOmitido(),
                        rp.getMotivoOmision()))
                .toList();
        int atendidosEnRuta = (int) lista.stream().filter(RutaPuntoDto::atendido).count();
        return new RutaDto(ruta.getId(), ruta.getFecha(),
                ruta.isCerrada() ? RutaDto.CERRADA : RutaDto.ABIERTA,
                ruta.getOperario().getUsername(),
                ruta.getSupervisor() == null ? null : ruta.getSupervisor().getUsername(),
                lista.size(), atendidosEnRuta, lista);
    }
}
