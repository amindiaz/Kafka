package cl.donaciones.logistica.service;

import cl.donaciones.logistica.messaging.dto.DonacionEvent;

/** Casos de uso disparados por eventos de donaciones recibidos por RabbitMQ. */
public interface EnvioEventService {

    /** Crea un envío PLANIFICADO para la donación (idempotente por donacionId). */
    void crearEnvioDesdeDonacion(DonacionEvent event);

    /** Refleja en los envíos de la donación el nuevo estado de ésta. */
    void sincronizarEstadoEnvio(DonacionEvent event);
}
