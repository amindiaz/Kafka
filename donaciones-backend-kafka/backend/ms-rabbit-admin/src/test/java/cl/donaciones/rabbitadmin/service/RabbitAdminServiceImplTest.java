package cl.donaciones.rabbitadmin.service;

import cl.donaciones.rabbitadmin.dto.RabbitAdminDTOs.*;
import cl.donaciones.rabbitadmin.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceImplTest {

    @Mock AmqpAdmin amqpAdmin;
    @InjectMocks RabbitAdminServiceImpl service;

    @Test
    void crearCola_declara_cola_durable_con_dlx() {
        QueueRequest req = new QueueRequest();
        req.setName("pedidos.queue");
        req.setDeadLetterExchange("donaciones.dlx");
        req.setDeadLetterRoutingKey("pedidos.queue.dlq");

        OperationResponse res = service.crearCola(req);

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("pedidos.queue");
        assertThat(captor.getValue().isDurable()).isTrue();
        assertThat(captor.getValue().getArguments()).containsEntry("x-dead-letter-exchange", "donaciones.dlx");
        assertThat(res.getAction()).isEqualTo("CREATED");
    }

    @Test
    void crearCola_nombreInvalido_lanzaIllegalArgument() {
        QueueRequest req = new QueueRequest();
        req.setName("nombre con espacios");
        assertThatThrownBy(() -> service.crearCola(req)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(amqpAdmin);
    }

    @Test
    void crearCola_prefijoReservado_lanzaIllegalArgument() {
        QueueRequest req = new QueueRequest();
        req.setName("amq.gen-123");
        assertThatThrownBy(() -> service.crearCola(req)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eliminarCola_existente_laElimina() {
        when(amqpAdmin.getQueueProperties("q1")).thenReturn(new Properties());
        service.eliminarCola("q1");
        verify(amqpAdmin).deleteQueue("q1");
    }

    @Test
    void eliminarCola_inexistente_lanzaNotFound() {
        when(amqpAdmin.getQueueProperties("q1")).thenReturn(null);
        assertThatThrownBy(() -> service.eliminarCola("q1")).isInstanceOf(ResourceNotFoundException.class);
        verify(amqpAdmin, never()).deleteQueue(any());
    }

    @Test
    void obtenerCola_devuelve_contadores() {
        Properties p = new Properties();
        p.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 4);
        p.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1);
        when(amqpAdmin.getQueueProperties("q1")).thenReturn(p);

        QueueInfoResponse info = service.obtenerCola("q1");

        assertThat(info.getMessageCount()).isEqualTo(4);
        assertThat(info.getConsumerCount()).isEqualTo(1);
    }

    @Test
    void crearExchange_topic_declaraTopicExchange() {
        ExchangeRequest req = new ExchangeRequest();
        req.setName("eventos.exchange");
        req.setType(ExchangeKind.TOPIC);

        service.crearExchange(req);

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TopicExchange.class);
        assertThat(captor.getValue().getName()).isEqualTo("eventos.exchange");
    }

    @Test
    void eliminarExchange_llamaAlBroker() {
        service.eliminarExchange("eventos.exchange");
        verify(amqpAdmin).deleteExchange("eventos.exchange");
    }

    @Test
    void crearYEliminarBinding_usanLosMismosDatos() {
        BindingRequest req = new BindingRequest();
        req.setQueue("q1");
        req.setExchange("ex1");
        req.setRoutingKey("a.b");

        service.crearBinding(req);
        service.eliminarBinding(req);

        ArgumentCaptor<Binding> declared = ArgumentCaptor.forClass(Binding.class);
        ArgumentCaptor<Binding> removed = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(declared.capture());
        verify(amqpAdmin).removeBinding(removed.capture());
        assertThat(declared.getValue().getRoutingKey()).isEqualTo("a.b");
        assertThat(removed.getValue().getDestination()).isEqualTo("q1");
        assertThat(removed.getValue().getExchange()).isEqualTo("ex1");
    }
}
