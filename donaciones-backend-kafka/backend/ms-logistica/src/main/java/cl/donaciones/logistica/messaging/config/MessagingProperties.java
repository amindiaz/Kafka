package cl.donaciones.logistica.messaging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de exchanges, colas y routing keys de ms-logistica.
 * Fuente única: application.yml → {@code app.messaging}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    /** Exchange topic de eventos de donaciones (declarado por el productor y aquí, idempotente). */
    private String exchange;

    /** Dead Letter Exchange (direct) al que van los mensajes rechazados. */
    private String dlx;

    /** Sufijo de las Dead Letter Queues, p. ej. ".dlq". */
    private String dlqSuffix = ".dlq";

    /** Reintentos máximos ante errores recuperables antes de enviar a la DLQ. */
    private int maxRetries = 3;

    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Getter
    @Setter
    public static class Queues {
        private String donacionCreada;
        private String donacionEstado;
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        private String donacionCreada;
        private String donacionEstado;
    }

    public String dlqOf(String queueName) {
        return queueName + dlqSuffix;
    }
}
