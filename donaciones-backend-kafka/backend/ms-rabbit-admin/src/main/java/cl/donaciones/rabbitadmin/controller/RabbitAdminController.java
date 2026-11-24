package cl.donaciones.rabbitadmin.controller;

import cl.donaciones.rabbitadmin.dto.RabbitAdminDTOs.*;
import cl.donaciones.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * API REST de administración de RabbitMQ (solo rol ADMIN).
 *
 *   POST   /api/rabbit/queues              crea una cola
 *   GET    /api/rabbit/queues/{name}       consulta mensajes/consumidores de una cola
 *   DELETE /api/rabbit/queues/{name}       elimina una cola
 *   POST   /api/rabbit/exchanges           crea un exchange
 *   DELETE /api/rabbit/exchanges/{name}    elimina un exchange
 *   POST   /api/rabbit/bindings            crea un binding exchange → cola
 *   DELETE /api/rabbit/bindings?queue=&exchange=&routingKey=   elimina un binding
 */
@RestController
@RequestMapping("/api/rabbit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RabbitAdminController {

    private final RabbitAdminService service;

    // ── Colas ─────────────────────────────────────────────────────────────────

    @PostMapping("/queues")
    public ResponseEntity<OperationResponse> crearCola(@Valid @RequestBody QueueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCola(request));
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<QueueInfoResponse> obtenerCola(@PathVariable String name) {
        return ResponseEntity.ok(service.obtenerCola(name));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @PostMapping("/exchanges")
    public ResponseEntity<OperationResponse> crearExchange(@Valid @RequestBody ExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearExchange(request));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @PostMapping("/bindings")
    public ResponseEntity<OperationResponse> crearBinding(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearBinding(request));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid BindingRequest request) {
        service.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}
