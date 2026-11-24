package cl.donaciones.logistica.messaging.consumer;

import cl.donaciones.logistica.messaging.dto.DonacionEvent;
import cl.donaciones.logistica.messaging.support.MessageAckHandler;
import cl.donaciones.logistica.service.EnvioEventService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidores del dominio "logística": reaccionan a eventos de donaciones.
 * Solo reciben, delegan en {@link EnvioEventService} y confirman (ACK manual).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DonacionLogisticaConsumer {

    private final EnvioEventService envioEventService;
    private final MessageAckHandler ackHandler;

    @RabbitListener(queues = "${app.messaging.queues.donacion-creada}")
    public void onDonacionCreada(DonacionEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        log.info("Evento recibido {} donacionId={}", event.eventType(), event.donacionId());
        try {
            envioEventService.crearEnvioDesdeDonacion(event);
            ackHandler.ack(channel, tag);
        } catch (Exception e) {
            ackHandler.fallo(message, channel, tag, e);
        }
    }

    @RabbitListener(queues = "${app.messaging.queues.donacion-estado}")
    public void onDonacionEstadoCambiado(DonacionEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        log.info("Evento recibido {} donacionId={} {}→{}", event.eventType(), event.donacionId(),
                event.estadoAnterior(), event.estado());
        try {
            envioEventService.sincronizarEstadoEnvio(event);
            ackHandler.ack(channel, tag);
        } catch (Exception e) {
            ackHandler.fallo(message, channel, tag, e);
        }
    }
}
