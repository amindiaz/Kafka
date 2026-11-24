package cl.donaciones.necesidades.service;

import cl.donaciones.necesidades.messaging.dto.DonacionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Tarea asíncrona: notificación de nuevas donaciones al equipo de necesidades.
 * (En esta versión la notificación se registra en el log; el canal real —correo,
 * push— se enchufa aquí sin tocar el consumidor ni la topología.)
 */
@Service
@Slf4j
public class NotificacionService {

    public void notificarNuevaDonacion(DonacionEvent event) {
        if (event == null || event.donacionId() == null) {
            throw new IllegalArgumentException("Evento de donación inválido: falta donacionId");
        }
        log.info("NOTIFICACIÓN → Nueva donación #{}: {} {} de {} en centro '{}'. Revisar necesidades abiertas.",
                event.donacionId(), event.cantidad(), event.unidad(), event.tipo(), event.centroAcopio());
    }
}
