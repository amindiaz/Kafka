package cl.donaciones.logistica.repository;

import cl.donaciones.logistica.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    List<Envio> findByEstado(Envio.Estado estado);
    List<Envio> findByCentroOrigenId(Long centroId);
    List<Envio> findByDonacionId(Long donacionId);
}
