package cl.donaciones.necesidades.controller;

import cl.donaciones.necesidades.dto.NecesidadDTOs.*;
import cl.donaciones.necesidades.service.NecesidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/necesidades")
@RequiredArgsConstructor
public class NecesidadController {

    private final NecesidadService necesidadService;

    /** GET /api/necesidades */
    @GetMapping
    public ResponseEntity<List<NecesidadResponse>> getAll(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String prioridad) {
        if (estado    != null) return ResponseEntity.ok(necesidadService.getByEstado(estado));
        if (prioridad != null) return ResponseEntity.ok(necesidadService.getByPrioridad(prioridad));
        return ResponseEntity.ok(necesidadService.getAll());
    }

    /** GET /api/necesidades/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<NecesidadResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(necesidadService.getById(id));
    }

    /** GET /api/necesidades/usuario/{usuarioId} */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<NecesidadResponse>> getByUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(necesidadService.getByUsuario(usuarioId));
    }

    /** POST /api/necesidades */
    @PostMapping
    public ResponseEntity<NecesidadResponse> crear(@Valid @RequestBody NecesidadRequest request) {
        return ResponseEntity.status(201).body(necesidadService.crear(request));
    }

    /** PUT /api/necesidades/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<NecesidadResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody NecesidadRequest request) {
        return ResponseEntity.ok(necesidadService.actualizar(id, request));
    }

    /** PATCH /api/necesidades/{id}/estado */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<NecesidadResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(necesidadService.actualizarEstado(id, request.getEstado()));
    }

    /** DELETE /api/necesidades/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        necesidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
