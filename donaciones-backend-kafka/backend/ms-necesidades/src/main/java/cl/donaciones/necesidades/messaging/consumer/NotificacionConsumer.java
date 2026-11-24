package cl.donaciones.necesidades.messaging.consumer;

import cl.donaciones.necesidades.messaging.dto.DonacionEvent;
import cl.donaciones.necesidades.messaging.support.MessageAckHandler;
import cl.donaciones.necesidades.service.NotificacionService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidores del dominio "notificaciones" de ms-necesidades (ACK manual). */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionConsumer {

    private final NotificacionService notificacionService;
    private final MessageAckHandler ackHandler;

    @RabbitListener(queues = "${app.messaging.queues.notificacion-donacion}")
    public void onDonacionCreada(DonacionEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            notificacionService.notificarNuevaDonacion(event);
            ackHandler.ack(channel, tag);
        } catch (Exception e) {
            ackHandler.fallo(message, channel, tag, e);
        }
    }
}
