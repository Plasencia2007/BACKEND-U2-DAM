package pe.upeu.ecorecicla.servicio;

/** El usuario esta autenticado pero no puede ejecutar la operacion sobre ese recurso (RN-02). Se traduce a 403. */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
