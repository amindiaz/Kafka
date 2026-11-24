package cl.donaciones.kafkalab.service;

import java.util.Locale;
import java.util.Map;

/** Resultado de una prueba de carga del lab 3.3.2. */
public record LoadTestResult(int solicitados,
                             long confirmados,
                             long fallidos,
                             double segundos,
                             Map<String, Object> producerConfig) {

    /** Throughput confirmado: mensajes confirmados por el broker / segundos. */
    public double mensajesPorSegundo() {
        return segundos > 0 ? confirmados / segundos : 0;
    }

    public String resumen() {
        return String.format(Locale.ROOT,
                "Solicitados=%d Confirmados=%d Fallidos=%d Segundos=%.3f msg/s=%.1f "
                        + "| batch.size=%s linger.ms=%s compression.type=%s acks=%s",
                solicitados, confirmados, fallidos, segundos, mensajesPorSegundo(),
                producerConfig.get("batch.size"), producerConfig.get("linger.ms"),
                producerConfig.get("compression.type"), producerConfig.get("acks"));
    }
}
