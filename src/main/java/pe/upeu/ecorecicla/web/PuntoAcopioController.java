package pe.upeu.ecorecicla.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pe.upeu.ecorecicla.config.SecurityConfig;
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado;
import pe.upeu.ecorecicla.servicio.PesajeService;
import pe.upeu.ecorecicla.servicio.PuntoAcopioService;
import pe.upeu.ecorecicla.web.dto.Pagina;
import pe.upeu.ecorecicla.web.dto.PesajeDto;
import pe.upeu.ecorecicla.web.dto.PesajePeticion;
import pe.upeu.ecorecicla.web.dto.PuntoAcopioDto;
import pe.upeu.ecorecicla.web.dto.PuntoAcopioPeticion;

/** RF-B03 (puntos de acopio) y RF-B04 (registro de pesaje sobre un punto). */
@RestController
@RequestMapping(SecurityConfig.API + "/puntos")
public class PuntoAcopioController {

    private final PuntoAcopioService puntoService;
    private final PesajeService pesajeService;

    public PuntoAcopioController(PuntoAcopioService puntoService, PesajeService pesajeService) {
        this.puntoService = puntoService;
        this.pesajeService = pesajeService;
    }

    @GetMapping
    public Pagina<PuntoAcopioDto> listar(@RequestParam(required = false) String zona,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         @RequestParam(defaultValue = "20") int tamanio) {
        return Pagina.de(puntoService.listar(zona, pagina, tamanio), PuntoAcopioDto::de);
    }

    @GetMapping("/{id}")
    public PuntoAcopioDto obtener(@PathVariable Long id) {
        return PuntoAcopioDto.de(puntoService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<PuntoAcopioDto> crear(@Valid @RequestBody PuntoAcopioPeticion peticion) {
        PuntoAcopioDto creado = PuntoAcopioDto.de(puntoService.crear(peticion));
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    /** Pesajes de un punto, del mas reciente al mas antiguo. */
    @GetMapping("/{id}/pesajes")
    public Pagina<PesajeDto> pesajes(@PathVariable Long id,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    @RequestParam(defaultValue = "20") int tamanio) {
        return Pagina.de(pesajeService.listarPorPunto(id, pagina, tamanio), dto -> dto);
    }

    /** 201 Created con el id definitivo y el neto calculado; 200 si la claveCliente ya se habia recibido. */
    @PostMapping("/{id}/pesajes")
    public ResponseEntity<PesajeDto> registrar(@PathVariable Long id,
                                               @Valid @RequestBody PesajePeticion peticion,
                                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        PesajeService.Registro registro = pesajeService.registrar(id, peticion, usuario);
        PesajeDto dto = registro.pesaje();
        if (!registro.creado()) {
            return ResponseEntity.ok(dto);
        }
        var ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(SecurityConfig.API + "/pesajes/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(ubicacion).body(dto);
    }
}
