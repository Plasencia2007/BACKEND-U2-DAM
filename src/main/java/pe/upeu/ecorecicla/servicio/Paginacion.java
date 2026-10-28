package pe.upeu.ecorecicla.servicio;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Valida los parametros "pagina" y "tamanio" de los listados. */
public final class Paginacion {

    public static final int TAMANIO_MAXIMO = 100;

    private Paginacion() {
    }

    public static Pageable de(int pagina, int tamanio, Sort orden) {
        if (pagina < 0) {
            throw new ValidacionDeNegocioException("pagina", "La pagina no puede ser negativa");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new ValidacionDeNegocioException("tamanio", "El tamanio debe estar entre 1 y " + TAMANIO_MAXIMO);
        }
        return PageRequest.of(pagina, tamanio, orden);
    }

    public static Pageable de(int pagina, int tamanio) {
        return de(pagina, tamanio, Sort.unsorted());
    }
}
