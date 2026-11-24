package cl.donaciones.logistica.messaging;

import cl.donaciones.logistica.messaging.config.MessagingProperties;
import cl.donaciones.logistica.messaging.support.MessageAckHandler;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageAckHandlerTest {

    @Mock RabbitTemplate rabbitTemplate;
    @Mock Channel channel;
    MessageAckHandler handler;

    @BeforeEach
    void setUp() {
        MessagingProperties props = new MessagingProperties();
        props.setMaxRetries(2);
        handler = new MessageAckHandler(rabbitTemplate, props);
    }

    private Message mensaje(Integer retries) {
        MessageBuilder b = MessageBuilder.withBody("{}".getBytes());
        if (retries != null) b.setHeader(MessageAckHandler.RETRY_HEADER, retries);
        Message m = b.build();
        m.getMessageProperties().setConsumerQueue("cola.test");
        return m;
    }

    @Test
    void ack_confirma_el_mensaje() throws Exception {
        handler.ack(channel, 10L);
        verify(channel).basicAck(10L, false);
    }

    @Test
    void errorNoRecuperable_va_directo_a_dlq_sin_reintento() throws Exception {
        handler.fallo(mensaje(null), channel, 11L, new IllegalArgumentException("centro no existe"));

        verify(channel).basicReject(11L, false);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void errorRecuperable_reintenta_incrementando_header_y_hace_ack_del_original() throws Exception {
        handler.fallo(mensaje(null), channel, 12L, new RuntimeException("BD caída"));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(""), eq("cola.test"), captor.capture());
        assertThat((Integer) captor.getValue().getMessageProperties().getHeader(MessageAckHandler.RETRY_HEADER)).isEqualTo(1);
        verify(channel).basicAck(12L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    void errorRecuperable_superados_los_reintentos_va_a_dlq() throws Exception {
        handler.fallo(mensaje(2), channel, 13L, new RuntimeException("BD caída"));

        verify(channel).basicReject(13L, false);
        verify(rabbitTemplate, never()).send(any(String.class), any(String.class), any(Message.class));
    }
}
