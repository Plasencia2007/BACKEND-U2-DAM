package pe.upeu.ecorecicla.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_ANULAR;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_CREAR;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_EDITAR;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PESAJE_LEER;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PUNTO_GESTIONAR;
import static pe.upeu.ecorecicla.seguridad.Privilegio.PUNTO_LEER;
import static pe.upeu.ecorecicla.seguridad.Privilegio.RUTA_CERRAR;

/**
 * Matriz de autorizacion del caso: cada operacion exige un PRIVILEGIO (autoridad),
 * nunca un nombre de rol. Que rol tiene que privilegio vive en la base de datos.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String API = "/api/v1";

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http,
                                                 JwtAuthenticationFilter filtroJwt,
                                                 ManejadorDeErrores manejador) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(manejador)   // 401: token ausente, alterado o vencido
                        .accessDeniedHandler(manejador))       // 403: autenticado sin el privilegio
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Publico
                        .requestMatchers(HttpMethod.POST, API + "/auth/login").permitAll()
                        // Usuario actual: basta con estar autenticado
                        .requestMatchers(HttpMethod.GET, API + "/auth/me").authenticated()
                        // Rutas
                        .requestMatchers(HttpMethod.GET, API + "/rutas/hoy").hasAuthority(PUNTO_LEER)
                        .requestMatchers(HttpMethod.POST, API + "/rutas").hasAuthority(PUNTO_GESTIONAR)
                        .requestMatchers(HttpMethod.POST, API + "/rutas/*/cierre").hasAuthority(RUTA_CERRAR)
                        .requestMatchers(HttpMethod.POST, API + "/rutas/*/puntos/*/omision").hasAuthority(PESAJE_CREAR)
                        // Pesajes de un punto
                        .requestMatchers(HttpMethod.GET, API + "/puntos/*/pesajes").hasAuthority(PESAJE_LEER)
                        .requestMatchers(HttpMethod.POST, API + "/puntos/*/pesajes").hasAuthority(PESAJE_CREAR)
                        // Puntos de acopio
                        .requestMatchers(HttpMethod.GET, API + "/puntos", API + "/puntos/*").hasAuthority(PUNTO_LEER)
                        .requestMatchers(HttpMethod.POST, API + "/puntos").hasAuthority(PUNTO_GESTIONAR)
                        // Pesaje individual
                        .requestMatchers(HttpMethod.GET, API + "/pesajes/*").hasAuthority(PESAJE_LEER)
                        .requestMatchers(HttpMethod.PUT, API + "/pesajes/*").hasAuthority(PESAJE_EDITAR)
                        .requestMatchers(HttpMethod.DELETE, API + "/pesajes/*").hasAuthority(PESAJE_ANULAR)
                        // Todo lo demas exige token
                        .anyRequest().authenticated())
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
