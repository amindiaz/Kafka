package cl.donaciones.logistica.controller;

import cl.donaciones.logistica.dto.LogisticaDTOs.*;
import cl.donaciones.logistica.service.LogisticaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistica/envios")
@RequiredArgsConstructor
public class EnvioController {

    private final LogisticaService logisticaService;

    /** GET /api/logistica/envios */
    @GetMapping
    public ResponseEntity<List<EnvioResponse>> getAll(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(estado != null
                ? logisticaService.getEnviosByEstado(estado)
                : logisticaService.getAllEnvios());
    }

    /** GET /api/logistica/envios/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<EnvioResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(logisticaService.getEnvioById(id));
    }

    /** GET /api/logistica/envios/centro/{centroId} */
    @GetMapping("/centro/{centroId}")
    public ResponseEntity<List<EnvioResponse>> getByCentro(@PathVariable Long centroId) {
        return ResponseEntity.ok(logisticaService.getEnviosByCentro(centroId));
    }

    /** POST /api/logistica/envios – OPERADOR o ADMIN */
    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public ResponseEntity<EnvioResponse> crear(@Valid @RequestBody EnvioRequest request) {
        return ResponseEntity.status(201).body(logisticaService.crearEnvio(request));
    }

    /** PUT /api/logistica/envios/{id} – OPERADOR o ADMIN */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public ResponseEntity<EnvioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EnvioRequest request) {
        return ResponseEntity.ok(logisticaService.actualizarEnvio(id, request));
    }

    /** PATCH /api/logistica/envios/{id}/estado – OPERADOR o ADMIN */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public ResponseEntity<EnvioResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(logisticaService.actualizarEstadoEnvio(id, request.getEstado()));
    }

    /** DELETE /api/logistica/envios/{id} – solo ADMIN */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        logisticaService.eliminarEnvio(id);
        return ResponseEntity.noContent().build();
    }
}
