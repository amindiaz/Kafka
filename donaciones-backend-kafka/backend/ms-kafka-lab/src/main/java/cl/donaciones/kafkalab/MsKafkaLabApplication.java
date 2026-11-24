package cl.donaciones.kafkalab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de laboratorio Apache Kafka (puerto 8888).
 *
 * <ul>
 *   <li>/api/kafka/** : guía autónoma (topic tutorial-particiones, grupo mi-grupo-educativo).</li>
 *   <li>/api/test/**  : lab 3.3.2 tuning y pruebas de carga (topic sensor-topic, grupo analytics-group).</li>
 * </ul>
 */
@SpringBootApplication
public class MsKafkaLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsKafkaLabApplication.class, args);
    }
}
