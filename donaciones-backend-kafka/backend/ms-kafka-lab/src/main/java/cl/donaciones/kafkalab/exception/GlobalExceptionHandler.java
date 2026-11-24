package cl.donaciones.kafkalab.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/** Traduce los fallos de publicación (broker caído, topic inexistente) a 503. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ExecutionException.class, TimeoutException.class})
    public ResponseEntity<String> kafkaNoDisponible(Exception e) {
        Throwable causa = e.getCause() != null ? e.getCause() : e;
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Kafka no confirmo el envio: " + causa.getClass().getSimpleName()
                        + " - " + causa.getMessage()
                        + ". Revisa 'docker compose ps' y los logs de kafka.\n");
    }
}
