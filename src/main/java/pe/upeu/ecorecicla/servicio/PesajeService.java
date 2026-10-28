package pe.upeu.ecorecicla.servicio;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upeu.ecorecicla.dominio.Pesaje;
import pe.upeu.ecorecicla.dominio.PuntoAcopio;
import pe.upeu.ecorecicla.dominio.Ruta;
import pe.upeu.ecorecicla.repositorio.PesajeRepository;
import pe.upeu.ecorecicla.repositorio.PuntoAcopioRepository;
import pe.upeu.ecorecicla.seguridad.Privilegio;
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado;
import pe.upeu.ecorecicla.seguridad.UsuarioRepository;
import pe.upeu.ecorecicla.web.dto.CorreccionPesajePeticion;
import pe.upeu.ecorecicla.web.dto.PesajeDto;
import pe.upeu.ecorecicla.web.dto.PesajePeticion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Capa que coordina las operaciones sobre pesajes y hace cumplir RN-01, RN-02 y RN-03.
 * Los controladores solo delegan aqui; la conversion a DTO ocurre dentro de la transaccion.
 */
@Service
public class PesajeService {

    /** Resultado de registrar: indica si se creo o si ya existia (reenvio de un pendiente). */
    public record Registro(PesajeDto pesaje, boolean creado) {
    }

    private final PesajeRepository pesajes;
    private final PuntoAcopioRepository puntos;
    private final UsuarioRepository usuarios;
    private final RutaService rutaService;

    public PesajeService(PesajeRepository pesajes, PuntoAcopioRepository puntos, UsuarioRepository usuarios,
                         RutaService rutaService) {
        this.pesajes = pesajes;
        this.puntos = puntos;
        this.usuarios = usuarios;
        this.rutaService = rutaService;
    }

    @Transactional(readOnly = true)
    public Page<PesajeDto> listarPorPunto(Long puntoId, int pagina, int tamanio) {
        if (!puntos.existsById(puntoId)) {
            throw new RecursoNoEncontradoException("No existe el punto de acopio " + puntoId);
        }
        return pesajes.findByPuntoIdOrderByFechaHoraDescIdDesc(puntoId, Paginacion.de(pagina, tamanio))
                .map(PesajeDto::de);
    }

    @Transactional(readOnly = true)
    public Page<PesajeDto> listarPorRuta(Long rutaId, int pagina, int tamanio) {
        rutaService.buscar(rutaId);
        return pesajes.findByRutaIdOrderByFechaHoraDescIdDesc(rutaId, Paginacion.de(pagina, tamanio))
                .map(PesajeDto::de);
    }

    @Transactional(readOnly = true)
    public PesajeDto obtener(Long id) {
        return PesajeDto.de(buscar(id));
    }

    @Transactional
    public Registro registrar(Long puntoId, PesajePeticion peticion, UsuarioAutenticado usuario) {
        // Reenvio de un pendiente ya recibido: se devuelve el existente en lugar de duplicarlo.
        if (peticion.claveCliente() != null && !peticion.claveCliente().isBlank()) {
            Optional<Pesaje> existente = pesajes.findByClaveCliente(peticion.claveCliente());
            if (existente.isPresent()) {
                return new Registro(PesajeDto.de(existente.get()), false);
            }
        }
        PuntoAcopio punto = puntos.findById(puntoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el punto de acopio " + puntoId));
        Ruta ruta = rutaService.buscar(peticion.rutaId());
        if (!ruta.incluye(puntoId)) {
            throw new RecursoNoEncontradoException("El punto " + puntoId + " no pertenece a la ruta " + ruta.getId());
        }
        RutaService.exigirRutaAbierta(ruta);                       // RN-01
        RutaService.exigirAsignacion(ruta, usuario);
        exigirNetoPositivo(peticion.pesoBruto(), peticion.tara()); // RN-03

        Pesaje pesaje = new Pesaje(ruta, punto, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                peticion.material().trim(), peticion.pesoBruto(), peticion.tara(), limpiar(peticion.observacion()),
                usuarios.getReferenceById(usuario.id()), limpiar(peticion.claveCliente()));
        return new Registro(PesajeDto.de(pesajes.save(pesaje)), true);
    }

    @Transactional
    public PesajeDto corregir(Long id, CorreccionPesajePeticion peticion, UsuarioAutenticado usuario) {
        Pesaje pesaje = buscar(id);
        if (pesaje.isAnulado()) {
            throw new ConflictoDeNegocioException("El pesaje " + id + " esta anulado y no se puede corregir");
        }
        RutaService.exigirRutaAbierta(pesaje.getRuta());            // RN-01
        exigirPuedeCorregir(pesaje, usuario);                       // RN-02
        exigirNetoPositivo(peticion.pesoBruto(), peticion.tara());  // RN-03
        pesaje.corregir(peticion.material().trim(), peticion.pesoBruto(), peticion.tara(),
                limpiar(peticion.observacion()));
        return PesajeDto.de(pesaje);
    }

    /** La anulacion marca el pesaje; nunca lo elimina de la base. */
    @Transactional
    public void anular(Long id, String motivo, UsuarioAutenticado usuario) {
        Pesaje pesaje = buscar(id);
        if (pesaje.isAnulado()) {
            throw new ConflictoDeNegocioException("El pesaje " + id + " ya estaba anulado");
        }
        RutaService.exigirRutaAbierta(pesaje.getRuta());
        if (motivo == null || motivo.isBlank()) {
            throw new ValidacionDeNegocioException("motivo", "El motivo de anulacion es obligatorio");
        }
        pesaje.anular(motivo.trim());
    }

    /**
     * RN-02: quien solo tiene PESAJE_EDITAR corrige unicamente sus propios pesajes y dentro del mismo dia.
     * PESAJE_EDITAR_TODOS levanta esa restriccion. Se decide por privilegio, no por nombre de rol.
     */
    private void exigirPuedeCorregir(Pesaje pesaje, UsuarioAutenticado usuario) {
        if (usuario.tiene(Privilegio.PESAJE_EDITAR_TODOS)) {
            return;
        }
        if (!pesaje.getOperario().getId().equals(usuario.id())) {
            throw new OperacionNoPermitidaException("Solo puede corregir los pesajes que usted registro");
        }
        if (!pesaje.getFechaHora().toLocalDate().equals(LocalDate.now())) {
            throw new OperacionNoPermitidaException("Solo puede corregir pesajes registrados el dia de hoy");
        }
    }

    /** RN-03: el neto (bruto - tara) debe ser mayor que cero. */
    private static void exigirNetoPositivo(double bruto, double tara) {
        if (Pesaje.calcularNeto(bruto, tara) <= 0) {
            throw new ValidacionDeNegocioException("tara",
                    "La tara debe ser menor que el peso bruto: el peso neto debe ser mayor que cero");
        }
    }

    private Pesaje buscar(Long id) {
        return pesajes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pesaje " + id));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
