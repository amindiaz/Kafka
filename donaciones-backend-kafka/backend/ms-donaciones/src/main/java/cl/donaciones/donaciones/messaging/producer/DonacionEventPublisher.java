package cl.donaciones.donaciones.messaging.producer;

import cl.donaciones.donaciones.messaging.config.MessagingProperties;
import cl.donaciones.donaciones.messaging.dto.DonacionEvent;
import cl.donaciones.donaciones.model.Donacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor de eventos de donaciones.
 *
 * - Publica DESPUÉS del commit de la transacción (si hay una activa), para no
 *   emitir eventos de datos que luego se revierten.
 * - Si RabbitMQ no está disponible solo se registra el error: la lógica de
 *   negocio existente (crear/actualizar donaciones) nunca falla por mensajería.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DonacionEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public void publicarCreada(Donacion donacion) {
        publicar(props.getRoutingKeys().getDonacionCreada(),
                toEvent(DonacionEvent.CREADA, donacion, null));
    }

    public void publicarEstadoCambiado(Donacion donacion, Donacion.Estado estadoAnterior) {
        publicar(props.getRoutingKeys().getDonacionEstado(),
                toEvent(DonacionEvent.ESTADO_CAMBIADO, donacion, estadoAnterior));
    }

    private void publicar(String routingKey, DonacionEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(routingKey, event);
                }
            });
        } else {
            enviar(routingKey, event);
        }
    }

    private void enviar(String routingKey, DonacionEvent event) {
        try {
            rabbitTemplate.convertAndSend(props.getExchange(), routingKey, event);
            log.info("Evento {} publicado (donacionId={}, rk={}, eventId={})",
                    event.eventType(), event.donacionId(), routingKey, event.eventId());
        } catch (AmqpException e) {
            log.error("No se pudo publicar {} de donacion {} en RabbitMQ: {}",
                    event.eventType(), event.donacionId(), e.getMessage());
        }
    }

    private DonacionEvent toEvent(String type, Donacion d, Donacion.Estado anterior) {
        return new DonacionEvent(
                UUID.randomUUID(), type, Instant.now(),
                d.getId(), d.getTipo(), d.getCantidad(), d.getUnidad(), d.getOrigen(),
                d.getCentroAcopio(), d.getEstado().name(),
                anterior != null ? anterior.name() : null,
                d.getUsuarioId(), d.getFecha());
    }
}
