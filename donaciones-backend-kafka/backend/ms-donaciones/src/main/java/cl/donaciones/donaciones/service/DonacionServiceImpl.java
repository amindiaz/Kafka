package cl.donaciones.donaciones.service;

import cl.donaciones.donaciones.dto.DonacionDTOs.*;
import cl.donaciones.donaciones.messaging.producer.DonacionEventPublisher;
import cl.donaciones.donaciones.model.Donacion;
import cl.donaciones.donaciones.repository.DonacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonacionServiceImpl implements DonacionService {

    private final DonacionRepository donacionRepository;
    private final DonacionEventPublisher eventPublisher;

    @Override
    public List<DonacionResponse> getAll() {
        log.info("Obteniendo todas las donaciones");
        return donacionRepository.findAll()
                .stream()
                .map(DonacionResponse::from)
                .toList();
    }

    @Override
    public List<DonacionResponse> getByEstado(String estadoStr) {
        log.info("Filtrando donaciones por estado: {}", estadoStr);
        Donacion.Estado estado = parseEstado(estadoStr);
        return donacionRepository.findByEstado(estado)
                .stream()
                .map(DonacionResponse::from)
                .toList();
    }

    @Override
    public List<DonacionResponse> getByUsuario(Long usuarioId) {
        log.info("Obteniendo donaciones del usuario: {}", usuarioId);
        return donacionRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(DonacionResponse::from)
                .toList();
    }

    @Override
    public DonacionResponse getById(Long id) {
        log.info("Buscando donacion con id: {}", id);
        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada con id: " + id));
        return DonacionResponse.from(donacion);
    }

    @Override
    @Transactional
    public DonacionResponse crear(DonacionRequest request) {
        log.info("Creando nueva donacion de tipo: {}", request.getTipo());

        Donacion.Estado estado = Donacion.Estado.Recibida;
        if (request.getEstado() != null && !request.getEstado().isBlank()) {
            estado = parseEstado(request.getEstado());
        }

        Donacion donacion = Donacion.builder()
                .tipo(request.getTipo())
                .cantidad(request.getCantidad())
                .unidad(request.getUnidad() != null ? request.getUnidad() : "unidades")
                .origen(request.getOrigen())
                .centroAcopio(request.getCentroAcopio())
                .estado(estado)
                .fecha(request.getFecha() != null ? request.getFecha() : LocalDate.now())
                .usuarioId(request.getUsuarioId())
                .build();

        Donacion saved = donacionRepository.save(donacion);
        log.info("Donacion creada con id: {}", saved.getId());
        eventPublisher.publicarCreada(saved);
        return DonacionResponse.from(saved);
    }

    @Override
    @Transactional
    public DonacionResponse actualizar(Long id, DonacionRequest request) {
        log.info("Actualizando donacion con id: {}", id);

        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada con id: " + id));

        donacion.setTipo(request.getTipo());
        donacion.setCantidad(request.getCantidad());
        donacion.setUnidad(request.getUnidad() != null ? request.getUnidad() : donacion.getUnidad());
        donacion.setOrigen(request.getOrigen());
        donacion.setCentroAcopio(request.getCentroAcopio());
        if (request.getFecha() != null) donacion.setFecha(request.getFecha());
        Donacion.Estado estadoAnterior = donacion.getEstado();
        if (request.getEstado() != null) donacion.setEstado(parseEstado(request.getEstado()));

        Donacion updated = donacionRepository.save(donacion);
        if (updated.getEstado() != estadoAnterior) {
            eventPublisher.publicarEstadoCambiado(updated, estadoAnterior);
        }
        return DonacionResponse.from(updated);
    }

    @Override
    @Transactional
    public DonacionResponse actualizarEstado(Long id, String estadoStr) {
        log.info("Actualizando estado de donacion {} a {}", id, estadoStr);

        Donacion donacion = donacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donación no encontrada con id: " + id));

        Donacion.Estado estadoAnterior = donacion.getEstado();
        donacion.setEstado(parseEstado(estadoStr));
        Donacion updated = donacionRepository.save(donacion);
        if (updated.getEstado() != estadoAnterior) {
            eventPublisher.publicarEstadoCambiado(updated, estadoAnterior);
        }
        return DonacionResponse.from(updated);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando donacion con id: {}", id);
        if (!donacionRepository.existsById(id)) {
            throw new IllegalArgumentException("Donación no encontrada con id: " + id);
        }
        donacionRepository.deleteById(id);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private Donacion.Estado parseEstado(String estadoStr) {
        String normalizado = estadoStr.trim().replace(" ", "_");
        try {
            return Donacion.Estado.valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Estado inválido: '" + estadoStr + "'. Valores válidos: Recibida, En_transito, Distribuida, Cancelada");
        }
    }
}
