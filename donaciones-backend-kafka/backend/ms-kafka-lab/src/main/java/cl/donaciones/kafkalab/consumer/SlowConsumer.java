package cl.donaciones.kafkalab.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumidor lento del lab 3.3.2 (grupo analytics-group).
 *
 * <p>Duerme {@code lab.tuning.consumer-delay-ms} (10 ms) por mensaje para que el productor
 * le saque ventaja y aparezca Consumer Lag. Confirma el offset manualmente después de
 * procesar cada registro (commit por registro).</p>
 */
@Service
public class SlowConsumer {

    private static final Logger log = LoggerFactory.getLogger(SlowConsumer.class);

    private final long delayMs;
    private final AtomicLong procesados = new AtomicLong();

    public SlowConsumer(@Value("${lab.tuning.consumer-delay-ms}") long delayMs) {
        this.delayMs = delayMs;
    }

    @KafkaListener(id = "analytics-consumer",
                   topics = "${lab.tuning.topic}",
                   groupId = "${lab.tuning.group-id}",
                   containerFactory = "analyticsListenerFactory")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) throws InterruptedException {
        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }
        ack.acknowledge();

        long n = procesados.incrementAndGet();
        if (n % 1000 == 0) {
            log.info("analytics-group: {} mensajes procesados (ultimo: particion={} offset={} key={})",
                    n, record.partition(), record.offset(), record.key());
        }
    }

    public long getProcesados() {
        return procesados.get();
    }
}
