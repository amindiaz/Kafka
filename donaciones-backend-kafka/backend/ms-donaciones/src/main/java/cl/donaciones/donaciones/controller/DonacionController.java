package cl.donaciones.donaciones.controller;

import cl.donaciones.donaciones.dto.DonacionDTOs.*;
import cl.donaciones.donaciones.service.DonacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donaciones")
@RequiredArgsConstructor
public class DonacionController {

    private final DonacionService donacionService;

    /** GET /api/donaciones */
    @GetMapping
    public ResponseEntity<List<DonacionResponse>> getAll(
            @RequestParam(required = false) String estado) {
        if (estado != null) return ResponseEntity.ok(donacionService.getByEstado(estado));
        return ResponseEntity.ok(donacionService.getAll());
    }

    /** GET /api/donaciones/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<DonacionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(donacionService.getById(id));
    }

    /** GET /api/donaciones/usuario/{usuarioId} */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<DonacionResponse>> getByUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(donacionService.getByUsuario(usuarioId));
    }

    /** POST /api/donaciones */
    @PostMapping
    public ResponseEntity<DonacionResponse> crear(@Valid @RequestBody DonacionRequest request) {
        return ResponseEntity.status(201).body(donacionService.crear(request));
    }

    /** PUT /api/donaciones/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<DonacionResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DonacionRequest request) {
        return ResponseEntity.ok(donacionService.actualizar(id, request));
    }

    /** PATCH /api/donaciones/{id}/estado */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<DonacionResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(donacionService.actualizarEstado(id, request.getEstado()));
    }

    /** DELETE /api/donaciones/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        donacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
