package pe.upeu.ecorecicla.web;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.upeu.ecorecicla.config.SecurityConfig;
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado;
import pe.upeu.ecorecicla.servicio.PesajeService;
import pe.upeu.ecorecicla.servicio.RutaService;
import pe.upeu.ecorecicla.web.dto.MotivoPeticion;
import pe.upeu.ecorecicla.web.dto.Pagina;
import pe.upeu.ecorecicla.web.dto.PesajeDto;
import pe.upeu.ecorecicla.web.dto.RutaDto;
import pe.upeu.ecorecicla.web.dto.RutaPeticion;

import java.time.LocalDate;

/** Rutas de recojo. Los privilegios de cada operacion estan en SecurityConfig. */
@RestController
@RequestMapping(SecurityConfig.API + "/rutas")
public class RutaController {

    private final RutaService rutaService;
    private final PesajeService pesajeService;

    public RutaController(RutaService rutaService, PesajeService pesajeService) {
        this.rutaService = rutaService;
        this.pesajeService = pesajeService;
    }

    /** RF-B02: ruta asignada al usuario autenticado para una fecha (hoy si no se indica). */
    @GetMapping("/hoy")
    public RutaDto rutaDelDia(@AuthenticationPrincipal UsuarioAutenticado usuario,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return rutaService.rutaDelDia(usuario, fecha);
    }

    /** Pesajes de toda la ruta, del mas reciente al mas antiguo (lo usa la sincronizacion). */
    @GetMapping("/{id}/pesajes")
    public Pagina<PesajeDto> pesajesDeLaRuta(@PathVariable Long id,
                                            @RequestParam(defaultValue = "0") int pagina,
                                            @RequestParam(defaultValue = "50") int tamanio) {
        return Pagina.de(pesajeService.listarPorRuta(id, pagina, tamanio), PesajeDto::de);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RutaDto crear(@Valid @RequestBody RutaPeticion peticion) {
        return rutaService.crear(peticion);
    }

    @PostMapping("/{rutaId}/puntos/{puntoId}/omision")
    public RutaDto omitirPunto(@PathVariable Long rutaId, @PathVariable Long puntoId,
                               @Valid @RequestBody MotivoPeticion peticion,
                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return rutaService.omitirPunto(rutaId, puntoId, peticion.motivo(), usuario);
    }

    /** RF-B06: cierre de la ruta (RN-04). */
    @PostMapping("/{id}/cierre")
    public RutaDto cerrar(@PathVariable Long id) {
        return rutaService.cerrar(id);
    }
}
