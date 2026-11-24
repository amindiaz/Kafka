package cl.donaciones.necesidades.repository;

import cl.donaciones.necesidades.model.Necesidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NecesidadRepository extends JpaRepository<Necesidad, Long> {
    List<Necesidad> findByEstado(Necesidad.Estado estado);
    List<Necesidad> findByPrioridad(Necesidad.Prioridad prioridad);
    List<Necesidad> findByUsuarioId(Long usuarioId);
    List<Necesidad> findByUbicacionContainingIgnoreCase(String ubicacion);
}
