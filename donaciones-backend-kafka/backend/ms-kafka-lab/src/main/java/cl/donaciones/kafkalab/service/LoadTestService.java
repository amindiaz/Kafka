package cl.donaciones.kafkalab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Generador de carga del lab 3.3.2.
 *
 * <p>Envía N eventos de sensores IoT en ventanas (por defecto 500) y espera la
 * confirmación del broker de cada envío antes de abrir la siguiente ventana. El
 * throughput informado es el de envíos CONFIRMADOS, no el tiempo de encolado asíncrono
 * (pregunta 4 del análisis).</p>
 */
@Service
public class LoadTestService {

    private static final Logger log = LoggerFactory.getLogger(LoadTestService.class);
    private static final long CONFIRM_TIMEOUT_SECONDS = 30;

    private final KafkaTemplate<String, String> template;
    private final String topic;
    private final int windowSize;
    private final int sensors;

    public LoadTestService(KafkaTemplate<String, String> template,
                           @Value("${lab.tuning.topic}") String topic,
                           @Value("${lab.tuning.window-size}") int windowSize,
                           @Value("${lab.tuning.sensors}") int sensors) {
        this.template = template;
        this.topic = topic;
        this.windowSize = windowSize;
        this.sensors = sensors;
    }

    public LoadTestResult run(int count) {
        long confirmados = 0;
        long fallidos = 0;
        long inicio = System.nanoTime();

        for (int base = 0; base < count; base += windowSize) {
            int fin = Math.min(base + windowSize, count);
            List<CompletableFuture<SendResult<String, String>>> ventana = new ArrayList<>(fin - base);

            for (int i = base; i < fin; i++) {
                String key = "sensor-" + (i % sensors);
                try {
                    ventana.add(template.send(topic, key, evento(key, i)));
                } catch (RuntimeException e) {          // p. ej. buffer lleno / metadata no disponible
                    fallidos++;
                    log.warn("Envío {} rechazado antes de llegar al broker: {}", i, e.getMessage());
                }
            }

            for (CompletableFuture<SendResult<String, String>> f : ventana) {
                try {
                    f.get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    confirmados++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fallidos++;
                } catch (Exception e) {
                    fallidos++;
                }
            }
        }

        double segundos = (System.nanoTime() - inicio) / 1_000_000_000.0;
        var result = new LoadTestResult(count, confirmados, fallidos, segundos, producerConfig());
        log.info("Prueba de carga: {}", result.resumen());
        return result;
    }

    /** Configuración efectiva del productor (para registrar el escenario A/B en la evidencia). */
    public Map<String, Object> producerConfig() {
        Map<String, Object> cfg = template.getProducerFactory().getConfigurationProperties();
        return Map.of(
                "batch.size", String.valueOf(cfg.getOrDefault("batch.size", "16384")),
                "linger.ms", String.valueOf(cfg.getOrDefault("linger.ms", "0")),
                "compression.type", String.valueOf(cfg.getOrDefault("compression.type", "none")),
                "acks", String.valueOf(cfg.getOrDefault("acks", "all")));
    }

    private static String evento(String sensorId, int seq) {
        double temperatura = 15 + ThreadLocalRandom.current().nextDouble() * 20;
        return String.format(Locale.ROOT,
                "{\"sensorId\":\"%s\",\"seq\":%d,\"temperatura\":%.2f,\"timestamp\":%d}",
                sensorId, seq, temperatura, System.currentTimeMillis());
    }
}
