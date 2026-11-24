package cl.donaciones.kafkalab.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración Kafka de los laboratorios.
 *
 * <p>Los topics se declaran como beans {@link NewTopic}: KafkaAdmin los crea al arrancar
 * si no existen (el broker tiene auto.create.topics.enable=false). Si ya existen, NO se
 * modifica su configuración, así que los cambios hechos con kafka-configs.sh (fase 7 del
 * lab 3.3.2) se respetan.</p>
 */
@Configuration
public class KafkaLabConfig {

    // ── Guía autónoma ────────────────────────────────────────────────────────────
    @Bean
    public NewTopic tutorialTopic(@Value("${lab.tutorial.topic}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ── Lab 3.3.2: 3 particiones, retención 5 min, 50 MiB por partición, segmentos 1 MiB ──
    @Bean
    public NewTopic sensorTopic(@Value("${lab.tuning.topic}") String topic,
                                @Value("${lab.tuning.partitions}") int partitions,
                                @Value("${lab.tuning.retention-ms}") long retentionMs,
                                @Value("${lab.tuning.retention-bytes}") long retentionBytes,
                                @Value("${lab.tuning.segment-bytes}") long segmentBytes) {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs))
                .config(TopicConfig.RETENTION_BYTES_CONFIG, String.valueOf(retentionBytes))
                .config(TopicConfig.SEGMENT_BYTES_CONFIG, String.valueOf(segmentBytes))
                .build();
    }

    /**
     * Fábrica de listeners del SlowConsumer (lab 3.3.2): commit MANUAL por registro.
     * Parte de la configuración de spring.kafka.consumer y solo cambia el auto-commit.
     * CONSUMER_CONCURRENCY (por defecto 1) permite probar más consumidores del mismo
     * grupo en la fase 8: el máximo útil es el número de particiones (3).
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> analyticsListenerFactory(
            ConsumerFactory<String, String> consumerFactory,
            @Value("${CONSUMER_CONCURRENCY:1}") int concurrency) {

        Map<String, Object> props = new HashMap<>(consumerFactory.getConfigurationProperties());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency);
        return factory;
    }
}
