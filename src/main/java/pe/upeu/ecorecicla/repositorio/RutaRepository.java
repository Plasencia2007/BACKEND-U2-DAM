package pe.upeu.ecorecicla.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.upeu.ecorecicla.dominio.Ruta;

import java.time.LocalDate;
import java.util.List;

public interface RutaRepository extends JpaRepository<Ruta, Long> {

    /** Rutas de una fecha en las que el usuario es el operario o el supervisor asignado. */
    @Query("select r from Ruta r left join r.supervisor s "
            + "where r.fecha = :fecha and (r.operario.id = :usuarioId or s.id = :usuarioId) order by r.id")
    List<Ruta> buscarAsignadas(@Param("fecha") LocalDate fecha, @Param("usuarioId") Long usuarioId);
}
