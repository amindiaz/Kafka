package cl.donaciones.logistica.service;

import cl.donaciones.logistica.messaging.dto.DonacionEvent;
import cl.donaciones.logistica.model.CentroAcopio;
import cl.donaciones.logistica.model.Envio;
import cl.donaciones.logistica.repository.CentroAcopioRepository;
import cl.donaciones.logistica.repository.EnvioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvioEventServiceImpl implements EnvioEventService {

    private final CentroAcopioRepository centroRepo;
    private final EnvioRepository envioRepo;

    @Override
    @Transactional
    public void crearEnvioDesdeDonacion(DonacionEvent event) {
        validar(event);

        // Idempotencia: RabbitMQ garantiza "al menos una vez"
        if (!envioRepo.findByDonacionId(event.donacionId()).isEmpty()) {
            log.info("Ya existe un envío para la donación {}; se ignora el evento duplicado", event.donacionId());
            return;
        }

        CentroAcopio centro = centroRepo.findFirstByNombreIgnoreCase(event.centroAcopio())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un centro de acopio llamado '" + event.centroAcopio() + "'"));

        Envio envio = Envio.builder()
                .centroOrigen(centro)
                .destino("Por asignar")
                .descripcion("Envío generado automáticamente desde la donación #" + event.donacionId()
                        + ": " + event.cantidad() + " " + event.unidad() + " de " + event.tipo())
                .estado(Envio.Estado.PLANIFICADO)
                .donacionId(event.donacionId())
                .build();

        Envio saved = envioRepo.save(envio);
        log.info("Envío {} creado automáticamente para la donación {}", saved.getId(), event.donacionId());
    }

    @Override
    @Transactional
    public void sincronizarEstadoEnvio(DonacionEvent event) {
        validar(event);

        Envio.Estado nuevo = mapear(event.estado());
        if (nuevo == null) {
            log.info("Estado de donación '{}' sin equivalente en envíos; no se hace nada", event.estado());
            return;
        }

        List<Envio> envios = envioRepo.findByDonacionId(event.donacionId());
        for (Envio e : envios) {
            e.setEstado(nuevo);
            if (nuevo == Envio.Estado.ENTREGADO && e.getFechaEntregaReal() == null) {
                e.setFechaEntregaReal(LocalDate.now());
            }
        }
        envioRepo.saveAll(envios);
        log.info("{} envío(s) de la donación {} actualizados a {}", envios.size(), event.donacionId(), nuevo);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validar(DonacionEvent event) {
        if (event == null || event.donacionId() == null || event.estado() == null) {
            throw new IllegalArgumentException("Evento de donación inválido: faltan donacionId o estado");
        }
    }

    private Envio.Estado mapear(String estadoDonacion) {
        return switch (estadoDonacion) {
            case "En_transito" -> Envio.Estado.EN_TRANSITO;
            case "Distribuida" -> Envio.Estado.ENTREGADO;
            case "Cancelada"   -> Envio.Estado.CANCELADO;
            default            -> null;
        };
    }
}
