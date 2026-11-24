package cl.donaciones.kafkalab.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumidor de la guía autónoma (sección 11).
 *
 * <p>No fija groupId en la anotación: usa spring.kafka.consumer.group-id como única fuente
 * (alternativa recomendada en la sección 16). Para la Prueba D basta con arrancar con
 * KAFKA_GROUP_ID=mi-grupo-educativo-2.</p>
 */
@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    // Sin "id": en Spring Kafka el id del listener reemplaza al group-id (idIsGroup=true).
    @KafkaListener(topics = "${lab.tutorial.topic}")
    public void listen(ConsumerRecord<String, String> record) {
        log.info("RECIBIDO | particion={} | offset={} | key={} | valor={}",
                record.partition(), record.offset(), record.key(), record.value());
    }
}
