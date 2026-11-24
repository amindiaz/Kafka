package cl.donaciones.donaciones.repository;

import cl.donaciones.donaciones.model.Donacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonacionRepository extends JpaRepository<Donacion, Long> {

    List<Donacion> findByEstado(Donacion.Estado estado);

    List<Donacion> findByUsuarioId(Long usuarioId);
}
