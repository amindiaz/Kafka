package cl.donaciones.donaciones.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ del productor: exchange y conversor JSON.
 * Las colas y bindings los declara cada consumidor (ms-logistica, ms-necesidades).
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitConfig {

    /** Exchange topic durable compartido por todos los eventos de donaciones. */
    @Bean
    public TopicExchange donacionesExchange(MessagingProperties props) {
        return new TopicExchange(props.getExchange(), true, false);
    }

    /** Serializa los eventos como JSON (Spring Boot lo asocia al RabbitTemplate). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
