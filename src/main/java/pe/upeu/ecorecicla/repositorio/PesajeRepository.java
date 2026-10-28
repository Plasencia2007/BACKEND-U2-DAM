package pe.upeu.ecorecicla.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.upeu.ecorecicla.dominio.Pesaje;

import java.util.Optional;
import java.util.Set;

public interface PesajeRepository extends JpaRepository<Pesaje, Long> {

    Page<Pesaje> findByPuntoIdOrderByFechaHoraDescIdDesc(Long puntoId, Pageable pageable);

    Page<Pesaje> findByRutaIdOrderByFechaHoraDescIdDesc(Long rutaId, Pageable pageable);

    Optional<Pesaje> findByClaveCliente(String claveCliente);

    /** Puntos de la ruta que tienen al menos un pesaje vigente (no anulado). */
    @Query("select distinct p.punto.id from Pesaje p where p.ruta.id = :rutaId and p.anulado = false")
    Set<Long> puntosAtendidos(@Param("rutaId") Long rutaId);
}
