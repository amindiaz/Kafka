package cl.donaciones.necesidades.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "necesidades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Necesidad {

    public enum Prioridad { ALTA, MEDIA, BAJA }
    public enum Estado    { PENDIENTE, EN_ATENCION, ATENDIDA, CANCELADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String recurso;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String unidad = "unidades";

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 300)
    private String ubicacion;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Prioridad prioridad = Prioridad.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "reportado_por", length = 150)
    private String reportadoPor;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
