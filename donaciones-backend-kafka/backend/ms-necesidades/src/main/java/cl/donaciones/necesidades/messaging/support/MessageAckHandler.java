package cl.donaciones.necesidades.messaging.support;

import cl.donaciones.necesidades.messaging.config.MessagingProperties;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Confirmación manual (ACK) y manejo de errores de los consumidores.
 *
 *  - Éxito                    → basicAck.
 *  - Error NO recuperable     → basicReject(requeue=false) → el broker lo envía al DLX/DLQ.
 *    (IllegalArgumentException: evento inválido, centro inexistente, etc.)
 *  - Error recuperable        → reintento (se republica con header x-retry-count);
 *    superado {@code app.messaging.max-retries} → DLQ.
 *  Todo mensaje enviado a la DLQ queda registrado en logs (nivel ERROR).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageAckHandler {

    public static final String RETRY_HEADER = "x-retry-count";

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public void ack(Channel channel, long deliveryTag) throws IOException {
        channel.basicAck(deliveryTag, false);
    }

    public void fallo(Message message, Channel channel, long deliveryTag, Exception error) throws IOException {
        String queue = message.getMessageProperties().getConsumerQueue();
        int intentos = retryCount(message);

        if (esRecuperable(error) && intentos < props.getMaxRetries()) {
            log.warn("Error recuperable en cola {} (intento {}/{}): {}. Se reintenta.",
                    queue, intentos + 1, props.getMaxRetries(), error.getMessage());
            if (reintentar(message, queue, intentos + 1)) {
                channel.basicAck(deliveryTag, false); // el reintento ya quedó encolado
                return;
            }
        }
        enviarADlq(message, channel, deliveryTag, queue, intentos, error);
    }

    // ── internos ─────────────────────────────────────────────────────────────

    private boolean esRecuperable(Exception e) {
        return !(e instanceof IllegalArgumentException);
    }

    private boolean reintentar(Message original, String queue, int nuevoIntento) {
        try {
            Message copia = MessageBuilder.fromMessage(original)
                    .setHeader(RETRY_HEADER, nuevoIntento)
                    .build();
            // exchange por defecto "" → enruta directo a la cola por su nombre
            rabbitTemplate.send("", queue, copia);
            return true;
        } catch (AmqpException e) {
            log.error("No se pudo republicar el mensaje para reintento en {}: {}", queue, e.getMessage());
            return false;
        }
    }

    private void enviarADlq(Message message, Channel channel, long tag, String queue,
                            int intentos, Exception error) throws IOException {
        log.error("MENSAJE A DLQ → cola={} dlq={} intentos={} causa='{}' messageId={} payload={}",
                queue, props.dlqOf(queue), intentos, error.getMessage(),
                message.getMessageProperties().getMessageId(),
                new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
        channel.basicReject(tag, false);
    }

    private int retryCount(Message message) {
        Object h = message.getMessageProperties().getHeader(RETRY_HEADER);
        return h instanceof Number n ? n.intValue() : 0;
    }
}
