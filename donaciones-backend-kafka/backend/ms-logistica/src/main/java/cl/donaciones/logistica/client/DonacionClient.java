package cl.donaciones.logistica.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Cliente para comunicarse con ms-donaciones.
 *
 * Utiliza CircuitBreaker de Resilience4j para proteger la llamada:
 * si ms-donaciones está caído o lento, retorna un Optional vacío
 * en lugar de propagar el error (fallback).
 */
@Component
@Slf4j
public class DonacionClient {

    private final RestTemplate          restTemplate;
    private final CircuitBreakerFactory cbFactory;
    private final String                msUrl;

    public DonacionClient(
            CircuitBreakerFactory cbFactory,
            @Value("${ms.donaciones.url}") String msUrl) {
        this.restTemplate = new RestTemplate();
        this.cbFactory    = cbFactory;
        this.msUrl        = msUrl;
    }

    /**
     * Verifica si una donación existe en ms-donaciones.
     *
     * @param donacionId ID de la donación
     * @return Optional con los datos básicos, vacío si el circuito está abierto o falla
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getDonacion(Long donacionId) {
        return cbFactory.create("ms-donaciones").run(
                () -> {
                    String url = msUrl + "/api/donaciones/" + donacionId;
                    Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                    return Optional.ofNullable(resp);
                },
                throwable -> {
                    log.warn("Circuit breaker activo para ms-donaciones al consultar donacion id={}: {}",
                            donacionId, throwable.getMessage());
                    return Optional.empty();
                }
        );
    }
}
