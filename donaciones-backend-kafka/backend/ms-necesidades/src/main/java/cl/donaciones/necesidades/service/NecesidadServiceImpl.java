package cl.donaciones.necesidades.service;

import cl.donaciones.necesidades.dto.NecesidadDTOs.*;
import cl.donaciones.necesidades.model.Necesidad;
import cl.donaciones.necesidades.repository.NecesidadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NecesidadServiceImpl implements NecesidadService {

    private final NecesidadRepository necesidadRepository;

    @Override
    public List<NecesidadResponse> getAll() {
        log.info("Obteniendo todas las necesidades");
        return necesidadRepository.findAll().stream()
                .map(NecesidadResponse::from).toList();
    }

    @Override
    public List<NecesidadResponse> getByEstado(String estadoStr) {
        Necesidad.Estado estado = parseEstado(estadoStr);
        return necesidadRepository.findByEstado(estado).stream()
                .map(NecesidadResponse::from).toList();
    }

    @Override
    public List<NecesidadResponse> getByPrioridad(String prioridadStr) {
        Necesidad.Prioridad prioridad = parsePrioridad(prioridadStr);
        return necesidadRepository.findByPrioridad(prioridad).stream()
                .map(NecesidadResponse::from).toList();
    }

    @Override
    public List<NecesidadResponse> getByUsuario(Long usuarioId) {
        return necesidadRepository.findByUsuarioId(usuarioId).stream()
                .map(NecesidadResponse::from).toList();
    }

    @Override
    public NecesidadResponse getById(Long id) {
        return NecesidadResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional
    public NecesidadResponse crear(NecesidadRequest req) {
        log.info("Creando necesidad: recurso={}, ubicacion={}", req.getRecurso(), req.getUbicacion());

        Necesidad necesidad = Necesidad.builder()
                .recurso(req.getRecurso())
                .cantidad(req.getCantidad())
                .unidad(req.getUnidad() != null ? req.getUnidad() : "unidades")
                .descripcion(req.getDescripcion())
                .ubicacion(req.getUbicacion())
                .latitud(req.getLatitud())
                .longitud(req.getLongitud())
                .prioridad(req.getPrioridad() != null ? parsePrioridad(req.getPrioridad()) : Necesidad.Prioridad.MEDIA)
                .estado(req.getEstado() != null ? parseEstado(req.getEstado()) : Necesidad.Estado.PENDIENTE)
                .reportadoPor(req.getReportadoPor())
                .usuarioId(req.getUsuarioId())
                .build();

        Necesidad saved = necesidadRepository.save(necesidad);
        log.info("Necesidad creada id={}", saved.getId());
        return NecesidadResponse.from(saved);
    }

    @Override
    @Transactional
    public NecesidadResponse actualizar(Long id, NecesidadRequest req) {
        Necesidad n = findOrThrow(id);
        if (req.getRecurso()     != null) n.setRecurso(req.getRecurso());
        if (req.getCantidad()    != null) n.setCantidad(req.getCantidad());
        if (req.getUnidad()      != null) n.setUnidad(req.getUnidad());
        if (req.getDescripcion() != null) n.setDescripcion(req.getDescripcion());
        if (req.getUbicacion()   != null) n.setUbicacion(req.getUbicacion());
        if (req.getLatitud()     != null) n.setLatitud(req.getLatitud());
        if (req.getLongitud()    != null) n.setLongitud(req.getLongitud());
        if (req.getPrioridad()   != null) n.setPrioridad(parsePrioridad(req.getPrioridad()));
        if (req.getEstado()      != null) n.setEstado(parseEstado(req.getEstado()));
        if (req.getReportadoPor()!= null) n.setReportadoPor(req.getReportadoPor());
        return NecesidadResponse.from(necesidadRepository.save(n));
    }

    @Override
    @Transactional
    public NecesidadResponse actualizarEstado(Long id, String estadoStr) {
        Necesidad n = findOrThrow(id);
        n.setEstado(parseEstado(estadoStr));
        return NecesidadResponse.from(necesidadRepository.save(n));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!necesidadRepository.existsById(id)) {
            throw new IllegalArgumentException("Necesidad no encontrada: " + id);
        }
        necesidadRepository.deleteById(id);
        log.info("Necesidad eliminada id={}", id);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Necesidad findOrThrow(Long id) {
        return necesidadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Necesidad no encontrada: " + id));
    }

    private Necesidad.Estado parseEstado(String s) {
        try { return Necesidad.Estado.valueOf(s.toUpperCase().replace(" ", "_")); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + s +
                    ". Valores: PENDIENTE, EN_ATENCION, ATENDIDA, CANCELADA");
        }
    }

    private Necesidad.Prioridad parsePrioridad(String s) {
        try { return Necesidad.Prioridad.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Prioridad inválida: " + s +
                    ". Valores: ALTA, MEDIA, BAJA");
        }
    }
}
