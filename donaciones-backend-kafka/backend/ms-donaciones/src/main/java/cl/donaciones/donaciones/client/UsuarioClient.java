package cl.donaciones.donaciones.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Cliente Feign hacia ms-usuarios.
 * Si el servicio cae, el fallback devuelve datos por defecto (Circuit Breaker).
 */
@FeignClient(
        name = "ms-usuarios",
        url = "${ms.usuarios.url:http://localhost:8081}",
        fallback = UsuarioClient.UsuarioClientFallback.class
)
public interface UsuarioClient {

    @GetMapping("/api/usuarios/{id}")
    Map<String, Object> getUsuarioById(@PathVariable("id") Long id);

    // ── Fallback (Circuit Breaker) ────────────────────────────────────────────
    @Component
    @Slf4j
    class UsuarioClientFallback implements UsuarioClient {

        @Override
        public Map<String, Object> getUsuarioById(Long id) {
            log.warn("[CircuitBreaker] ms-usuarios no disponible para el usuario id={}. " +
                     "Retornando datos de respaldo.", id);
            return Map.of(
                    "id",     id,
                    "nombre", "Usuario no disponible",
                    "email",  "sin-datos@fallback.cl",
                    "estado", "desconocido"
            );
        }
    }
}
