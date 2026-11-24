package cl.donaciones.donaciones.dto;

import cl.donaciones.donaciones.model.Donacion;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DonacionDTOs {

    @Data
    public static class DonacionResponse {
        private Long id;
        private String tipo;
        private Integer cantidad;
        private String unidad;
        private String origen;
        private String centroAcopio;
        private String estado;
        private LocalDate fecha;
        private Long usuarioId;
        private LocalDateTime createdAt;

        public static DonacionResponse from(Donacion d) {
            DonacionResponse dto = new DonacionResponse();
            dto.id           = d.getId();
            dto.tipo         = d.getTipo();
            dto.cantidad     = d.getCantidad();
            dto.unidad       = d.getUnidad();
            dto.origen       = d.getOrigen();
            dto.centroAcopio = d.getCentroAcopio();
            dto.estado       = d.getEstado().name().replace("_", " ");
            dto.fecha        = d.getFecha();
            dto.usuarioId    = d.getUsuarioId();
            dto.createdAt    = d.getCreatedAt();
            return dto;
        }
    }

    @Data
    public static class DonacionRequest {
        @NotBlank
        private String tipo;

        @NotNull @Min(1)
        private Integer cantidad;

        private String unidad = "unidades";

        @NotBlank
        private String origen;

        @NotBlank
        private String centroAcopio;

        private String estado = "Recibida";

        private LocalDate fecha;

        private Long usuarioId;
    }

    @Data
    public static class EstadoRequest {
        @NotBlank
        private String estado;
    }
}
