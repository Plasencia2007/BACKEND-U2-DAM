package pe.upeu.ecorecicla.seguridad;

/** Usuario o contrasena incorrectos. Se traduce a 401. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Usuario o contrasena incorrectos");
    }
}
