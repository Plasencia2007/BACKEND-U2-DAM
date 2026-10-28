package pe.upeu.ecorecicla.seguridad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upeu.ecorecicla.config.JwtService;
import pe.upeu.ecorecicla.config.SecurityConfig;
import pe.upeu.ecorecicla.servicio.RecursoNoEncontradoException;

import java.util.Set;

/** RF-B01 (inicio de sesion) y RF-B07 (usuario actual). */
@RestController
@RequestMapping(SecurityConfig.API + "/auth")
public class AuthController {

    public record LoginPeticion(
            @NotBlank(message = "El usuario es obligatorio") String username,
            @NotBlank(message = "La contrasena es obligatoria") String password) {
    }

    public record LoginRespuesta(String token, String tipo, long expiraEn, Long id, String usuario,
                                 String nombre, String rol, Set<String> privilegios) {
    }

    public record UsuarioActual(Long id, String usuario, String nombre, String rol, Set<String> privilegios) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public LoginRespuesta login(@Valid @RequestBody LoginPeticion peticion) {
        // La contrasena se compara contra el hash BCrypt guardado en la base.
        Usuario usuario = usuarios.findByUsername(peticion.username().trim())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(peticion.password(), u.getPassword()))
                .orElseThrow(CredencialesInvalidasException::new);
        return new LoginRespuesta(jwtService.generar(usuario), "Bearer", jwtService.segundosDeVigencia(),
                usuario.getId(), usuario.getUsername(), usuario.getNombreCompleto(),
                usuario.getRol().getNombre(), usuario.getRol().codigosDePrivilegios());
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UsuarioActual actual(@AuthenticationPrincipal UsuarioAutenticado principal) {
        // Se relee de la base para reflejar el estado actual del usuario.
        Usuario usuario = usuarios.findById(principal.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario del token ya no existe"));
        return new UsuarioActual(usuario.getId(), usuario.getUsername(), usuario.getNombreCompleto(),
                usuario.getRol().getNombre(), usuario.getRol().codigosDePrivilegios());
    }
}
