package cl.donaciones.logistica.repository;

import cl.donaciones.logistica.model.CentroAcopio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CentroAcopioRepository extends JpaRepository<CentroAcopio, Long> {
    List<CentroAcopio> findByActivoTrue();
    List<CentroAcopio> findByRegionIgnoreCase(String region);
    Optional<CentroAcopio> findFirstByNombreIgnoreCase(String nombre);
}
