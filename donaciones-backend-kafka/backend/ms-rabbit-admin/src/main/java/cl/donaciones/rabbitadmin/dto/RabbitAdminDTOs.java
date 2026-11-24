package cl.donaciones.rabbitadmin.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTOs del microservicio administrador. Las anotaciones evitan crear recursos inválidos. */
public class RabbitAdminDTOs {

    /** Nombre válido: letras, números, punto, guion y guion bajo (sin espacios). */
    public static final String NAME_REGEX = "^[A-Za-z0-9._-]+$";
    public static final String NAME_MSG = "solo puede contener letras, números, '.', '_' y '-'";

    @Data
    public static class QueueRequest {
        @NotBlank(message = "el nombre de la cola es obligatorio")
        @Size(max = 255, message = "máximo 255 caracteres")
        @Pattern(regexp = NAME_REGEX, message = NAME_MSG)
        private String name;

        private boolean durable = true;
        private boolean autoDelete = false;

        /** Exchange al que se envían los mensajes rechazados (opcional). */
        @Size(max = 255)
        @Pattern(regexp = "^$|" + NAME_REGEX, message = NAME_MSG)
        private String deadLetterExchange;

        /** Routing key con la que se reenvían los mensajes al DLX (opcional). */
        @Size(max = 255)
        private String deadLetterRoutingKey;

        /** TTL de los mensajes en milisegundos (opcional, > 0). */
        @Positive(message = "messageTtlMs debe ser mayor que 0")
        private Long messageTtlMs;
    }

    @Data
    public static class ExchangeRequest {
        @NotBlank(message = "el nombre del exchange es obligatorio")
        @Size(max = 255, message = "máximo 255 caracteres")
        @Pattern(regexp = NAME_REGEX, message = NAME_MSG)
        private String name;

        @NotNull(message = "el tipo es obligatorio: DIRECT, TOPIC, FANOUT o HEADERS")
        private ExchangeKind type;

        private boolean durable = true;
        private boolean autoDelete = false;
    }

    public enum ExchangeKind { DIRECT, TOPIC, FANOUT, HEADERS }

    /** Binding cola ← exchange. Se usa en el cuerpo (POST) y como query params (DELETE). */
    @Data
    public static class BindingRequest {
        @NotBlank(message = "el nombre de la cola es obligatorio")
        @Pattern(regexp = NAME_REGEX, message = NAME_MSG)
        private String queue;

        @NotBlank(message = "el nombre del exchange es obligatorio")
        @Pattern(regexp = NAME_REGEX, message = NAME_MSG)
        private String exchange;

        /** Puede ser vacía (fanout) o un patrón como "donacion.#". */
        @Size(max = 255, message = "máximo 255 caracteres")
        private String routingKey = "";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueInfoResponse {
        private String name;
        private int messageCount;
        private int consumerCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationResponse {
        private String resource;
        private String name;
        private String action;
        private String detail;
    }
}
