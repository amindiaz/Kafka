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
@RequestMapping("/api/logistica/centros")
@RequiredArgsConstructor
public class CentroAcopioController {

    private final LogisticaService logisticaService;

    /** GET /api/logistica/centros */
    @GetMapping
    public ResponseEntity<List<CentroAcopioResponse>> getAll(
            @RequestParam(defaultValue = "false") boolean soloActivos) {
        return ResponseEntity.ok(soloActivos
                ? logisticaService.getCentrosActivos()
                : logisticaService.getAllCentros());
    }

    /** GET /api/logistica/centros/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<CentroAcopioResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(logisticaService.getCentroById(id));
    }

    /** POST /api/logistica/centros  – OPERADOR o ADMIN */
    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public ResponseEntity<CentroAcopioResponse> crear(@Valid @RequestBody CentroAcopioRequest request) {
        return ResponseEntity.status(201).body(logisticaService.crearCentro(request));
    }

    /** PUT /api/logistica/centros/{id} – OPERADOR o ADMIN */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public ResponseEntity<CentroAcopioResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CentroAcopioRequest request) {
        return ResponseEntity.ok(logisticaService.actualizarCentro(id, request));
    }

    /** DELETE /api/logistica/centros/{id} – solo ADMIN */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        logisticaService.eliminarCentro(id);
        return ResponseEntity.noContent().build();
    }
}
