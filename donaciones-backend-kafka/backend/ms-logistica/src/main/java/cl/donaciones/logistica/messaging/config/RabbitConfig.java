package cl.donaciones.logistica.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ de ms-logistica (exchanges, colas, DLQ y bindings).
 *
 * Esta clase es la ÚNICA que conoce la configuración de mensajería; los
 * consumidores y servicios de negocio solo reciben mensajes ya configurados.
 *
 *   donaciones.exchange (topic) --donacion.created--------> logistica.donacion.creada.queue
 *   donaciones.exchange (topic) --donacion.estado.changed-> logistica.donacion.estado.queue
 *   donaciones.dlx (direct)     --<cola>.dlq---------------> <cola>.dlq
 *   (cada cola principal define x-dead-letter-exchange = donaciones.dlx)
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitConfig {

    // ── Exchanges ────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange donacionesExchange(MessagingProperties props) {
        return new TopicExchange(props.getExchange(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange(MessagingProperties props) {
        return new DirectExchange(props.getDlx(), true, false);
    }

    // ── Caso de uso 1: crear envío cuando se registra una donación ───────────

    @Bean
    public Queue donacionCreadaQueue(MessagingProperties props) {
        return durableQueueWithDlq(props, props.getQueues().getDonacionCreada());
    }

    @Bean
    public Queue donacionCreadaDlq(MessagingProperties props) {
        return QueueBuilder.durable(props.dlqOf(props.getQueues().getDonacionCreada())).build();
    }

    @Bean
    public Binding donacionCreadaBinding(Queue donacionCreadaQueue, TopicExchange donacionesExchange,
                                         MessagingProperties props) {
        return BindingBuilder.bind(donacionCreadaQueue).to(donacionesExchange)
                .with(props.getRoutingKeys().getDonacionCreada());
    }

    @Bean
    public Binding donacionCreadaDlqBinding(Queue donacionCreadaDlq, DirectExchange deadLetterExchange,
                                            MessagingProperties props) {
        return BindingBuilder.bind(donacionCreadaDlq).to(deadLetterExchange)
                .with(props.dlqOf(props.getQueues().getDonacionCreada()));
    }

    // ── Caso de uso 2: sincronizar estado del envío con el de la donación ────

    @Bean
    public Queue donacionEstadoQueue(MessagingProperties props) {
        return durableQueueWithDlq(props, props.getQueues().getDonacionEstado());
    }

    @Bean
    public Queue donacionEstadoDlq(MessagingProperties props) {
        return QueueBuilder.durable(props.dlqOf(props.getQueues().getDonacionEstado())).build();
    }

    @Bean
    public Binding donacionEstadoBinding(Queue donacionEstadoQueue, TopicExchange donacionesExchange,
                                         MessagingProperties props) {
        return BindingBuilder.bind(donacionEstadoQueue).to(donacionesExchange)
                .with(props.getRoutingKeys().getDonacionEstado());
    }

    @Bean
    public Binding donacionEstadoDlqBinding(Queue donacionEstadoDlq, DirectExchange deadLetterExchange,
                                            MessagingProperties props) {
        return BindingBuilder.bind(donacionEstadoDlq).to(deadLetterExchange)
                .with(props.dlqOf(props.getQueues().getDonacionEstado()));
    }

    // ── Conversor JSON ───────────────────────────────────────────────────────

    /**
     * El tipo del payload se infiere del parámetro del @RabbitListener (no del
     * header __TypeId__), así el consumidor no depende de las clases del productor.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private Queue durableQueueWithDlq(MessagingProperties props, String queueName) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(props.getDlx())
                .deadLetterRoutingKey(props.dlqOf(queueName))
                .build();
    }
}
