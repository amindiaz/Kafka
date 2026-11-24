package cl.donaciones.necesidades.dto;

import cl.donaciones.necesidades.model.Necesidad;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class NecesidadDTOs {

    @Data
    public static class NecesidadRequest {
        @NotBlank
        private String recurso;

        @NotNull @Min(1)
        private Integer cantidad;

        private String unidad = "unidades";

        private String descripcion;

        @NotBlank
        private String ubicacion;

        private BigDecimal latitud;
        private BigDecimal longitud;

        private String prioridad = "MEDIA";
        private String estado    = "PENDIENTE";
        private String reportadoPor;
        private Long   usuarioId;
    }

    @Data
    public static class EstadoRequest {
        @NotBlank
        private String estado;
    }

    @Data
    public static class NecesidadResponse {
        private Long   id;
        private String recurso;
        private Integer cantidad;
        private String unidad;
        private String descripcion;
        private String ubicacion;
        private BigDecimal latitud;
        private BigDecimal longitud;
        private String prioridad;
        private String estado;
        private String reportadoPor;
        private Long   usuarioId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static NecesidadResponse from(Necesidad n) {
            NecesidadResponse r = new NecesidadResponse();
            r.id          = n.getId();
            r.recurso     = n.getRecurso();
            r.cantidad    = n.getCantidad();
            r.unidad      = n.getUnidad();
            r.descripcion = n.getDescripcion();
            r.ubicacion   = n.getUbicacion();
            r.latitud     = n.getLatitud();
            r.longitud    = n.getLongitud();
            r.prioridad   = n.getPrioridad().name();
            r.estado      = n.getEstado().name();
            r.reportadoPor = n.getReportadoPor();
            r.usuarioId   = n.getUsuarioId();
            r.createdAt   = n.getCreatedAt();
            r.updatedAt   = n.getUpdatedAt();
            return r;
        }
    }
}
