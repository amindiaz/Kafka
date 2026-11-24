package cl.donaciones.donaciones.messaging;

import cl.donaciones.donaciones.messaging.config.MessagingProperties;
import cl.donaciones.donaciones.messaging.dto.DonacionEvent;
import cl.donaciones.donaciones.messaging.producer.DonacionEventPublisher;
import cl.donaciones.donaciones.model.Donacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DonacionEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    DonacionEventPublisher publisher;
    Donacion donacion;

    @BeforeEach
    void setUp() {
        MessagingProperties props = new MessagingProperties();
        props.setExchange("donaciones.exchange");
        props.getRoutingKeys().setDonacionCreada("donacion.created");
        props.getRoutingKeys().setDonacionEstado("donacion.estado.changed");
        publisher = new DonacionEventPublisher(rabbitTemplate, props);

        donacion = Donacion.builder().id(7L).tipo("Ropa").cantidad(10).unidad("prendas")
                .origen("Particular").centroAcopio("Santiago Norte")
                .estado(Donacion.Estado.Recibida).fecha(LocalDate.of(2025, 5, 1)).usuarioId(3L).build();
    }

    @Test
    void publicarCreada_envia_evento_al_exchange_con_routing_key_configurada() {
        publisher.publicarCreada(donacion);

        ArgumentCaptor<DonacionEvent> captor = ArgumentCaptor.forClass(DonacionEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("donaciones.exchange"), eq("donacion.created"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(DonacionEvent.CREADA);
        assertThat(captor.getValue().donacionId()).isEqualTo(7L);
        assertThat(captor.getValue().estadoAnterior()).isNull();
    }

    @Test
    void publicarEstadoCambiado_incluye_estado_anterior() {
        donacion.setEstado(Donacion.Estado.Distribuida);

        publisher.publicarEstadoCambiado(donacion, Donacion.Estado.Recibida);

        ArgumentCaptor<DonacionEvent> captor = ArgumentCaptor.forClass(DonacionEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("donaciones.exchange"), eq("donacion.estado.changed"), captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo("Distribuida");
        assertThat(captor.getValue().estadoAnterior()).isEqualTo("Recibida");
    }

    @Test
    void si_rabbit_no_esta_disponible_no_propaga_la_excepcion() {
        doThrow(new AmqpConnectException(new IOException("down")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertThatCode(() -> publisher.publicarCreada(donacion)).doesNotThrowAnyException();
    }
}
