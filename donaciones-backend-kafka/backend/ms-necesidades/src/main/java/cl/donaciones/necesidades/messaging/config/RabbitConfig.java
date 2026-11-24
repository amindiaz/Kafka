package cl.donaciones.necesidades.messaging.config;

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
 * Topología RabbitMQ de ms-necesidades.
 *
 *   donaciones.exchange (topic) --donacion.created--> necesidades.notificacion.donacion.queue
 *   donaciones.dlx (direct)     --<cola>.dlq--------> <cola>.dlq
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitConfig {

    @Bean
    public TopicExchange donacionesExchange(MessagingProperties props) {
        return new TopicExchange(props.getExchange(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange(MessagingProperties props) {
        return new DirectExchange(props.getDlx(), true, false);
    }

    // ── Caso de uso: notificar nuevas donaciones al equipo de necesidades ─────

    @Bean
    public Queue notificacionDonacionQueue(MessagingProperties props) {
        String name = props.getQueues().getNotificacionDonacion();
        return QueueBuilder.durable(name)
                .deadLetterExchange(props.getDlx())
                .deadLetterRoutingKey(props.dlqOf(name))
                .build();
    }

    @Bean
    public Queue notificacionDonacionDlq(MessagingProperties props) {
        return QueueBuilder.durable(props.dlqOf(props.getQueues().getNotificacionDonacion())).build();
    }

    @Bean
    public Binding notificacionDonacionBinding(Queue notificacionDonacionQueue, TopicExchange donacionesExchange,
                                               MessagingProperties props) {
        return BindingBuilder.bind(notificacionDonacionQueue).to(donacionesExchange)
                .with(props.getRoutingKeys().getDonacionCreada());
    }

    @Bean
    public Binding notificacionDonacionDlqBinding(Queue notificacionDonacionDlq, DirectExchange deadLetterExchange,
                                                  MessagingProperties props) {
        return BindingBuilder.bind(notificacionDonacionDlq).to(deadLetterExchange)
                .with(props.dlqOf(props.getQueues().getNotificacionDonacion()));
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
