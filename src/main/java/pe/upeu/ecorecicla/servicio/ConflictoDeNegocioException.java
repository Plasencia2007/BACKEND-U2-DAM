package pe.upeu.ecorecicla.servicio;

/** Regla de negocio del servidor incumplida (RN-01, RN-04...). Se traduce a 409 Conflict. */
public class ConflictoDeNegocioException extends RuntimeException {
    public ConflictoDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
