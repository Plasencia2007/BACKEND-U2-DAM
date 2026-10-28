package pe.upeu.ecorecicla.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado;

import java.io.IOException;
import java.util.List;

/**
 * Lee la cabecera "Authorization: Bearer ...", verifica la firma del token y, si es valido,
 * registra al usuario en el contexto con sus privilegios como autoridades.
 * Un token alterado o vencido corta la cadena con 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final AuthenticationEntryPoint puntoDeEntrada;

    public JwtAuthenticationFilter(JwtService jwtService, ManejadorDeErrores puntoDeEntrada) {
        this.jwtService = jwtService;
        this.puntoDeEntrada = puntoDeEntrada;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            // Sin token: la regla "authenticated()" de SecurityConfig respondera 401.
            chain.doFilter(request, response);
            return;
        }
        String token = cabecera.substring(PREFIJO.length()).trim();
        try {
            UsuarioAutenticado usuario = jwtService.validar(token);
            List<SimpleGrantedAuthority> autoridades = usuario.privilegios().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null, autoridades);
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            puntoDeEntrada.commence(request, response, new BadCredentialsException("El token ha vencido"));
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            puntoDeEntrada.commence(request, response, new BadCredentialsException("Token invalido o alterado"));
        }
    }
}
