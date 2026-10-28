package pe.upeu.ecorecicla.servicio;

import java.util.Map;

/** Validacion que solo el servicio puede hacer (RN-03). Se traduce a 400 con detalle por campo. */
public class ValidacionDeNegocioException extends RuntimeException {

    private final Map<String, String> errores;

    public ValidacionDeNegocioException(String campo, String mensaje) {
        super("Datos invalidos");
        this.errores = Map.of(campo, mensaje);
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
