package pe.upeu.ecorecicla.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.upeu.ecorecicla.config.SecurityConfig;
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado;
import pe.upeu.ecorecicla.servicio.PesajeService;
import pe.upeu.ecorecicla.web.dto.CorreccionPesajePeticion;
import pe.upeu.ecorecicla.web.dto.PesajeDto;

/** RF-B05: consulta, correccion y anulacion de un pesaje. */
@RestController
@RequestMapping(SecurityConfig.API + "/pesajes")
public class PesajeController {

    private final PesajeService pesajeService;

    public PesajeController(PesajeService pesajeService) {
        this.pesajeService = pesajeService;
    }

    @GetMapping("/{id}")
    public PesajeDto obtener(@PathVariable Long id) {
        return pesajeService.obtener(id);
    }

    @PutMapping("/{id}")
    public PesajeDto corregir(@PathVariable Long id,
                              @Valid @RequestBody CorreccionPesajePeticion peticion,
                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pesajeService.corregir(id, peticion, usuario);
    }

    /** Anulacion: marca el pesaje como ANULADO y responde 204 sin cuerpo. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Long id,
                                       @RequestParam String motivo,
                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        pesajeService.anular(id, motivo, usuario);
        return ResponseEntity.noContent().build();
    }
}
