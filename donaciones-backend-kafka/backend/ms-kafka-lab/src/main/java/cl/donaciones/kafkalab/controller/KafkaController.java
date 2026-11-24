package cl.donaciones.kafkalab.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Productor REST de la guía autónoma (secciones 10, 13 y 14).
 *
 * <p>Se usa POST porque publicar un evento produce un efecto. {@code get()} espera la
 * confirmación del broker para poder mostrar partición y offset; eso NO significa que un
 * consumidor ya haya procesado el mensaje. Bloquear el hilo HTTP solo se hace aquí para
 * facilitar la observación.</p>
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private static final Logger log = LoggerFactory.getLogger(KafkaController.class);

    private final KafkaTemplate<String, String> template;
    private final String topic;

    public KafkaController(KafkaTemplate<String, String> template,
                           @Value("${lab.tutorial.topic}") String topic) {
        this.template = template;
        this.topic = topic;
    }

    /** GET /api/kafka/health */
    @GetMapping("/health")
    public String health() {
        return "API Kafka operativa";
    }

    /** POST /api/kafka/send?message=...&key=...  (key es opcional) */
    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestParam String message,
                                       @RequestParam(required = false) String key) throws Exception {
        String clave = (key == null || key.isBlank()) ? null : key;

        var result = template.send(topic, clave, message).get(15, TimeUnit.SECONDS);
        var md = result.getRecordMetadata();

        log.info("Publicado: particion={}, offset={}, key={}", md.partition(), md.offset(), clave);
        return ResponseEntity.ok("Publicado: particion=" + md.partition()
                + ", offset=" + md.offset() + ", key=" + clave
                + ", message=" + message);
    }
}
