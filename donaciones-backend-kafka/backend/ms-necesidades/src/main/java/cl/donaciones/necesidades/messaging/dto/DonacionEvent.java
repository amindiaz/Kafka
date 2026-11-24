package cl.donaciones.necesidades.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Copia local del contrato del evento publicado por ms-donaciones (desacoplado por JSON). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DonacionEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long donacionId,
        String tipo,
        Integer cantidad,
        String unidad,
        String origen,
        String centroAcopio,
        String estado,
        String estadoAnterior,
        Long usuarioId,
        LocalDate fecha) {
}
