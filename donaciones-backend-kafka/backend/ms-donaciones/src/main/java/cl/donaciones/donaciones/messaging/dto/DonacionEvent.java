package cl.donaciones.donaciones.messaging.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento de dominio publicado en RabbitMQ cuando una donación cambia.
 *
 * @param eventType      DONACION_CREADA | DONACION_ESTADO_CAMBIADO
 * @param estadoAnterior null cuando la donación es nueva
 */
public record DonacionEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long donacionId,
        String tipo,
        Integer cantidad,
        String unidad,
        String origen,
        String centroAcopio,
        String estado,
        String estadoAnterior,
        Long usuarioId,
        LocalDate fecha) {

    public static final String CREADA = "DONACION_CREADA";
    public static final String ESTADO_CAMBIADO = "DONACION_ESTADO_CAMBIADO";
}
