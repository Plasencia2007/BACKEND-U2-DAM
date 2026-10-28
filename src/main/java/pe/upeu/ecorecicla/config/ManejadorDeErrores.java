package pe.upeu.ecorecicla.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pe.upeu.ecorecicla.seguridad.CredencialesInvalidasException;
import pe.upeu.ecorecicla.servicio.ConflictoDeNegocioException;
import pe.upeu.ecorecicla.servicio.OperacionNoPermitidaException;
import pe.upeu.ecorecicla.servicio.RecursoNoEncontradoException;
import pe.upeu.ecorecicla.servicio.ValidacionDeNegocioException;
import pe.upeu.ecorecicla.web.dto.ErrorApi;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Punto unico que traduce cualquier fallo al formato de error comun (seccion 4.2):
 * excepciones de los controladores y servicios, y tambien los 401/403 del filtro de seguridad.
 */
@RestControllerAdvice
public class ManejadorDeErrores implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    private final ObjectMapper objectMapper;

    public ManejadorDeErrores(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ---------- 400 ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errores.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req, errores);
    }

    @ExceptionHandler(ValidacionDeNegocioException.class)
    public ResponseEntity<ErrorApi> validacionDeNegocio(ValidacionDeNegocioException e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), req, e.getErrores());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorApi> restriccion(ConstraintViolationException e, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> {
            String ruta = v.getPropertyPath().toString();
            errores.putIfAbsent(ruta.substring(ruta.lastIndexOf('.') + 1), v.getMessage());
        });
        return responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> cuerpoIlegible(HttpMessageNotReadableException e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApi> tipoDeParametro(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req,
                Map.of(e.getName(), "Valor con formato incorrecto"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorApi> parametroAusente(MissingServletRequestParameterException e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Datos invalidos", req,
                Map.of(e.getParameterName(), "Parametro obligatorio"));
    }

    // ---------- 401 / 403 ----------

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorApi> credenciales(CredencialesInvalidasException e, HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage(), req, null);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorApi> noPermitida(OperacionNoPermitidaException e, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, e.getMessage(), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> accesoDenegado(AccessDeniedException e, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "No tiene el privilegio necesario para esta operacion", req, null);
    }

    // ---------- 404 / 405 / 409 / 500 ----------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorApi> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorApi> rutaInexistente(NoResourceFoundException e, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorApi> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Metodo no permitido", req, null);
    }

    @ExceptionHandler(ConflictoDeNegocioException.class)
    public ResponseEntity<ErrorApi> conflicto(ConflictoDeNegocioException e, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> inesperado(Exception e, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
    }

    // ---------- Filtro de seguridad ----------

    /** 401: token ausente, alterado o vencido. */
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        String mensaje = req.getHeader("Authorization") == null ? "Se requiere un token de acceso" : e.getMessage();
        escribir(res, cuerpo(HttpStatus.UNAUTHORIZED, mensaje, req, null));
    }

    /** 403: token valido pero sin el privilegio exigido por SecurityConfig. */
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) throws IOException {
        escribir(res, cuerpo(HttpStatus.FORBIDDEN, "No tiene el privilegio necesario para esta operacion", req, null));
    }

    // ---------- utilidades ----------

    private ResponseEntity<ErrorApi> responder(HttpStatus status, String mensaje, HttpServletRequest req,
                                               Map<String, String> errores) {
        return ResponseEntity.status(status).body(cuerpo(status, mensaje, req, errores));
    }

    private ErrorApi cuerpo(HttpStatus status, String mensaje, HttpServletRequest req, Map<String, String> errores) {
        String ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new ErrorApi(ahora, status.value(), status.getReasonPhrase(), mensaje, req.getRequestURI(), errores);
    }

    private void escribir(HttpServletResponse res, ErrorApi error) throws IOException {
        res.setStatus(error.status());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getOutputStream(), error);
    }
}
