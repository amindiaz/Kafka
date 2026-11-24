package cl.donaciones.kafkalab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Pruebas unitarias del generador de carga del lab 3.3.2. */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoadTestService - lab 3.3.2")
class LoadTestServiceTest {

    private static final String TOPIC = "sensor-topic";

    @Mock
    private KafkaTemplate<String, String> template;
    @Mock
    private ProducerFactory<String, String> producerFactory;

    private LoadTestService service;

    @BeforeEach
    void setUp() {
        service = new LoadTestService(template, TOPIC, 500, 5);
        lenient().when(template.getProducerFactory()).thenReturn(producerFactory);
        lenient().when(producerFactory.getConfigurationProperties()).thenReturn(Map.of(
                "batch.size", 32768, "linger.ms", "20", "compression.type", "snappy", "acks", "all"));
    }

    @Test
    @DisplayName("run() envía N mensajes en ventanas y cuenta todos como confirmados")
    void todosConfirmados() {
        when(template.send(eq(TOPIC), anyString(), anyString()))
                .thenAnswer(inv -> CompletableFuture.completedFuture(mock(SendResult.class)));

        LoadTestResult r = service.run(1200);

        assertEquals(1200, r.solicitados());
        assertEquals(1200, r.confirmados());
        assertEquals(0, r.fallidos());
        assertTrue(r.segundos() >= 0);
        verify(template, times(1200)).send(eq(TOPIC), anyString(), anyString());
        assertTrue(r.resumen().startsWith("Solicitados=1200 Confirmados=1200 Fallidos=0"));
        assertTrue(r.resumen().contains("batch.size=32768 linger.ms=20 compression.type=snappy"));
    }

    @Test
    @DisplayName("run() reparte las claves entre sensor-0 y sensor-4")
    void clavesDeSensores() {
        when(template.send(eq(TOPIC), anyString(), anyString()))
                .thenAnswer(inv -> CompletableFuture.completedFuture(mock(SendResult.class)));
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);

        service.run(10);

        verify(template, times(10)).send(eq(TOPIC), keys.capture(), anyString());
        Set<String> distintas = new HashSet<>(keys.getAllValues());
        assertEquals(Set.of("sensor-0", "sensor-1", "sensor-2", "sensor-3", "sensor-4"), distintas);
    }

    @Test
    @DisplayName("run() cuenta como fallidos los envíos que el broker no confirma")
    void cuentaFallidos() {
        AtomicInteger n = new AtomicInteger();
        when(template.send(eq(TOPIC), anyString(), anyString())).thenAnswer(inv ->
                n.getAndIncrement() % 4 == 0
                        ? CompletableFuture.failedFuture(new RuntimeException("timeout"))
                        : CompletableFuture.completedFuture(mock(SendResult.class)));

        LoadTestResult r = service.run(100);

        assertEquals(75, r.confirmados());
        assertEquals(25, r.fallidos());
    }

    @Test
    @DisplayName("mensajesPorSegundo() usa solo los confirmados")
    void throughputConfirmado() {
        var r = new LoadTestResult(1000, 800, 200, 2.0, Map.of());
        assertEquals(400.0, r.mensajesPorSegundo(), 0.0001);
    }
}
