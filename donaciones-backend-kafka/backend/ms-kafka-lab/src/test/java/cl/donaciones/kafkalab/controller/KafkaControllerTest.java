package cl.donaciones.kafkalab.controller;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de KafkaController (Arrange → Act → Assert). */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaController - guía autónoma")
class KafkaControllerTest {

    private static final String TOPIC = "tutorial-particiones";

    @Mock
    private KafkaTemplate<String, String> template;

    private KafkaController controller;

    @BeforeEach
    void setUp() {
        controller = new KafkaController(template, TOPIC);
    }

    private CompletableFuture<SendResult<String, String>> resultado(String key, String msg, int particion, long offset) {
        var record = new ProducerRecord<>(TOPIC, key, msg);
        var metadata = new RecordMetadata(new TopicPartition(TOPIC, particion), offset, 0, 0L, 0, 0);
        return CompletableFuture.completedFuture(new SendResult<>(record, metadata));
    }

    @Test
    @DisplayName("health() responde que la API está operativa")
    void health() {
        assertEquals("API Kafka operativa", controller.health());
    }

    @Test
    @DisplayName("send() con clave publica con esa clave y devuelve partición y offset")
    void sendConClave() throws Exception {
        when(template.send(eq(TOPIC), eq("orden-555"), eq("Paso1-Creada")))
                .thenReturn(resultado("orden-555", "Paso1-Creada", 2, 7L));

        ResponseEntity<String> resp = controller.send("Paso1-Creada", "orden-555");

        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().contains("particion=2"));
        assertTrue(resp.getBody().contains("offset=7"));
        assertTrue(resp.getBody().contains("key=orden-555"));
    }

    @Test
    @DisplayName("send() sin clave publica con key nula")
    void sendSinClave() throws Exception {
        when(template.send(eq(TOPIC), isNull(), eq("Mensaje1")))
                .thenReturn(resultado(null, "Mensaje1", 0, 0L));

        ResponseEntity<String> resp = controller.send("Mensaje1", null);

        assertTrue(resp.getBody().contains("key=null"));
        verify(template).send(eq(TOPIC), isNull(), eq("Mensaje1"));
    }

    @Test
    @DisplayName("send() con clave en blanco se trata como sin clave")
    void sendClaveEnBlanco() throws Exception {
        when(template.send(eq(TOPIC), isNull(), eq("Mensaje2")))
                .thenReturn(resultado(null, "Mensaje2", 1, 3L));

        controller.send("Mensaje2", "  ");

        verify(template).send(eq(TOPIC), isNull(), eq("Mensaje2"));
    }
}
