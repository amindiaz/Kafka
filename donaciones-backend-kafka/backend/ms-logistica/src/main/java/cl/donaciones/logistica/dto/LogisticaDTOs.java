package cl.donaciones.logistica.dto;

import cl.donaciones.logistica.model.CentroAcopio;
import cl.donaciones.logistica.model.Envio;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LogisticaDTOs {

    // ── Centro Acopio ─────────────────────────────────────────────────────────

    @Data
    public static class CentroAcopioRequest {
        @NotBlank private String nombre;
        @NotBlank private String direccion;
        @NotBlank private String region;
        @NotNull @Min(0) private Integer capacidad;
        private String responsable;
        private String telefono;
    }

    @Data
    public static class CentroAcopioResponse {
        private Long   id;
        private String nombre;
        private String direccion;
        private String region;
        private Integer capacidad;
        private String responsable;
        private String telefono;
        private Boolean activo;
        private LocalDateTime createdAt;

        public static CentroAcopioResponse from(CentroAcopio c) {
            CentroAcopioResponse r = new CentroAcopioResponse();
            r.id          = c.getId();
            r.nombre      = c.getNombre();
            r.direccion   = c.getDireccion();
            r.region      = c.getRegion();
            r.capacidad   = c.getCapacidad();
            r.responsable = c.getResponsable();
            r.telefono    = c.getTelefono();
            r.activo      = c.getActivo();
            r.createdAt   = c.getCreatedAt();
            return r;
        }
    }

    // ── Envío ─────────────────────────────────────────────────────────────────

    @Data
    public static class EnvioRequest {
        @NotNull  private Long   centroOrigenId;
        @NotBlank private String destino;
        private String descripcion;
        private String estado = "PLANIFICADO";
        private String responsable;
        private LocalDate fechaSalida;
        private LocalDate fechaEntregaEst;
        private LocalDate fechaEntregaReal;
        private Long donacionId;
    }

    @Data
    public static class EstadoRequest {
        @NotBlank private String estado;
    }

    @Data
    public static class EnvioResponse {
        private Long   id;
        private Long   centroOrigenId;
        private String centroOrigenNombre;
        private String destino;
        private String descripcion;
        private String estado;
        private String responsable;
        private LocalDate fechaSalida;
        private LocalDate fechaEntregaEst;
        private LocalDate fechaEntregaReal;
        private Long donacionId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static EnvioResponse from(Envio e) {
            EnvioResponse r = new EnvioResponse();
            r.id                  = e.getId();
            r.centroOrigenId      = e.getCentroOrigen() != null ? e.getCentroOrigen().getId() : null;
            r.centroOrigenNombre  = e.getCentroOrigen() != null ? e.getCentroOrigen().getNombre() : null;
            r.destino             = e.getDestino();
            r.descripcion         = e.getDescripcion();
            r.estado              = e.getEstado().name();
            r.responsable         = e.getResponsable();
            r.fechaSalida         = e.getFechaSalida();
            r.fechaEntregaEst     = e.getFechaEntregaEst();
            r.fechaEntregaReal    = e.getFechaEntregaReal();
            r.donacionId          = e.getDonacionId();
            r.createdAt           = e.getCreatedAt();
            r.updatedAt           = e.getUpdatedAt();
            return r;
        }
    }
}
