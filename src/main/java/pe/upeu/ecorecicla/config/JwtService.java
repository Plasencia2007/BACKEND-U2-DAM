package pe.upeu.ecorecicla.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.upeu.ecorecicla.seguridad.Usuario;
import pe.upeu.ecorecicla.seguridad.UsuarioAutenticado;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Emite y valida los tokens JWT firmados con HMAC-SHA256.
 * El token transporta el identificador del usuario (sub) y sus privilegios.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration vigencia;

    public JwtService(@Value("${ecorecicla.jwt.secreto}") String secretoBase64,
                      @Value("${ecorecicla.jwt.vigencia-minutos}") long vigenciaMinutos) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.vigencia = Duration.ofMinutes(vigenciaMinutos);
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("usr", usuario.getUsername())
                .claim("nombre", usuario.getNombreCompleto())
                .claim("rol", usuario.getRol().getNombre())
                .claim("privilegios", List.copyOf(usuario.getRol().codigosDePrivilegios()))
                .issuer("ecoapi")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public long segundosDeVigencia() {
        return vigencia.toSeconds();
    }

    /**
     * Verifica la firma y la fecha de vencimiento. Si el token esta alterado o vencido
     * lanza una JwtException (ExpiredJwtException, SignatureException, MalformedJwtException...).
     */
    public UsuarioAutenticado validar(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .requireIssuer("ecoapi")
                .build()
                .parseSignedClaims(token)
                .getPayload();
        List<?> privilegios = claims.get("privilegios", List.class);
        LinkedHashSet<String> codigos = new LinkedHashSet<>();
        if (privilegios != null) {
            privilegios.forEach(p -> codigos.add(String.valueOf(p)));
        }
        return new UsuarioAutenticado(
                Long.valueOf(claims.getSubject()),
                claims.get("usr", String.class),
                claims.get("nombre", String.class),
                claims.get("rol", String.class),
                codigos);
    }
}
