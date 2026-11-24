package cl.donaciones.rabbitadmin.service;

import cl.donaciones.rabbitadmin.dto.RabbitAdminDTOs.*;
import cl.donaciones.rabbitadmin.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Toda la lógica de creación/eliminación vive aquí; los controladores solo
 * llaman a métodos de alto nivel y no conocen la librería de RabbitMQ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitAdminServiceImpl implements RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    // ── Colas ─────────────────────────────────────────────────────────────────

    @Override
    public OperationResponse crearCola(QueueRequest req) {
        validarNombreNoReservado(req.getName());

        QueueBuilder builder = req.isDurable()
                ? QueueBuilder.durable(req.getName())
                : QueueBuilder.nonDurable(req.getName());
        if (req.isAutoDelete()) builder.autoDelete();
        if (req.getDeadLetterExchange() != null && !req.getDeadLetterExchange().isBlank()) {
            builder.deadLetterExchange(req.getDeadLetterExchange());
            if (req.getDeadLetterRoutingKey() != null && !req.getDeadLetterRoutingKey().isBlank()) {
                builder.deadLetterRoutingKey(req.getDeadLetterRoutingKey());
            }
        }
        if (req.getMessageTtlMs() != null) builder.ttl(req.getMessageTtlMs().intValue());

        amqpAdmin.declareQueue(builder.build());
        log.info("Cola '{}' creada (durable={}, autoDelete={})", req.getName(), req.isDurable(), req.isAutoDelete());
        return new OperationResponse("queue", req.getName(), "CREATED", "Cola declarada correctamente");
    }

    @Override
    public QueueInfoResponse obtenerCola(String name) {
        validarNombre(name);
        Properties props = amqpAdmin.getQueueProperties(name);
        if (props == null) throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        return new QueueInfoResponse(name,
                ((Number) props.getOrDefault(RabbitAdmin.QUEUE_MESSAGE_COUNT, 0)).intValue(),
                ((Number) props.getOrDefault(RabbitAdmin.QUEUE_CONSUMER_COUNT, 0)).intValue());
    }

    @Override
    public void eliminarCola(String name) {
        validarNombreNoReservado(name);
        if (amqpAdmin.getQueueProperties(name) == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        amqpAdmin.deleteQueue(name);
        log.info("Cola '{}' eliminada", name);
    }

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Override
    public OperationResponse crearExchange(ExchangeRequest req) {
        validarNombreNoReservado(req.getName());

        Exchange exchange = switch (req.getType()) {
            case DIRECT  -> new DirectExchange(req.getName(), req.isDurable(), req.isAutoDelete());
            case TOPIC   -> new TopicExchange(req.getName(), req.isDurable(), req.isAutoDelete());
            case FANOUT  -> new FanoutExchange(req.getName(), req.isDurable(), req.isAutoDelete());
            case HEADERS -> new HeadersExchange(req.getName(), req.isDurable(), req.isAutoDelete());
        };
        amqpAdmin.declareExchange(exchange);
        log.info("Exchange '{}' ({}) creado", req.getName(), req.getType());
        return new OperationResponse("exchange", req.getName(), "CREATED", "Exchange " + req.getType() + " declarado correctamente");
    }

    @Override
    public void eliminarExchange(String name) {
        validarNombreNoReservado(name);
        // RabbitAdmin.deleteExchange devuelve true aunque no exista; es idempotente.
        amqpAdmin.deleteExchange(name);
        log.info("Exchange '{}' eliminado", name);
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @Override
    public OperationResponse crearBinding(BindingRequest req) {
        validarNombre(req.getQueue());
        validarNombre(req.getExchange());
        amqpAdmin.declareBinding(new Binding(req.getQueue(), Binding.DestinationType.QUEUE,
                req.getExchange(), nvl(req.getRoutingKey()), null));
        log.info("Binding creado: exchange='{}' → cola='{}' rk='{}'",
                req.getExchange(), req.getQueue(), nvl(req.getRoutingKey()));
        return new OperationResponse("binding", req.getExchange() + "→" + req.getQueue(), "CREATED",
                "routingKey='" + nvl(req.getRoutingKey()) + "'");
    }

    @Override
    public void eliminarBinding(BindingRequest req) {
        validarNombre(req.getQueue());
        validarNombre(req.getExchange());
        amqpAdmin.removeBinding(new Binding(req.getQueue(), Binding.DestinationType.QUEUE,
                req.getExchange(), nvl(req.getRoutingKey()), null));
        log.info("Binding eliminado: exchange='{}' → cola='{}' rk='{}'",
                req.getExchange(), req.getQueue(), nvl(req.getRoutingKey()));
    }

    // ── validaciones ──────────────────────────────────────────────────────────

    private void validarNombre(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (!name.matches(NAME_REGEX)) {
            throw new IllegalArgumentException("Nombre inválido '" + name + "': " + NAME_MSG);
        }
    }

    /** Impide tocar recursos internos del broker (amq.*). */
    private void validarNombreNoReservado(String name) {
        validarNombre(name);
        if (name.startsWith("amq.")) {
            throw new IllegalArgumentException("El prefijo 'amq.' está reservado por RabbitMQ");
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private static final String NAME_REGEX = "^[A-Za-z0-9._-]+$";
    private static final String NAME_MSG = "solo puede contener letras, números, '.', '_' y '-'";
}
