package pe.upeu.ecorecicla.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrivilegioRepository extends JpaRepository<Privilegio, Long> {

    Optional<Privilegio> findByCodigo(String codigo);
}
