package pe.upeu.ecorecicla.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Envoltorio de paginacion de todos los listados (seccion 4.2). */
public record Pagina<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean ultima) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> conversor) {
        return new Pagina<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
