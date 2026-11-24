package cl.donaciones.donaciones.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuración global del Circuit Breaker con Resilience4j.
 *
 * Estados:
 *  - CLOSED   → flujo normal, peticiones van al servicio remoto.
 *  - OPEN     → circuito abierto tras superar umbral de fallos;
 *               peticiones rechazadas de inmediato → ejecuta fallback.
 *  - HALF_OPEN → prueba un porcentaje de peticiones para verificar
 *               si el servicio se recuperó.
 */
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> globalCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id ->
                new Resilience4JConfigBuilder(id)
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                // Tiempo máximo de espera antes de considerar fallo
                                .timeoutDuration(Duration.ofSeconds(3))
                                .build())
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                // Porcentaje de fallos para abrir el circuito
                                .failureRateThreshold(50)
                                // Número mínimo de llamadas para calcular tasa de fallos
                                .minimumNumberOfCalls(5)
                                // Tiempo en estado OPEN antes de pasar a HALF_OPEN
                                .waitDurationInOpenState(Duration.ofSeconds(10))
                                // Llamadas permitidas en estado HALF_OPEN
                                .permittedNumberOfCallsInHalfOpenState(3)
                                // Porcentaje de fallos para volver a OPEN desde HALF_OPEN
                                .slowCallRateThreshold(80)
                                .slowCallDurationThreshold(Duration.ofSeconds(2))
                                .build())
                        .build()
        );
    }
}
