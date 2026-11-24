package cl.donaciones.logistica.service;

import cl.donaciones.logistica.dto.LogisticaDTOs.*;
import cl.donaciones.logistica.model.CentroAcopio;
import cl.donaciones.logistica.model.Envio;
import cl.donaciones.logistica.repository.CentroAcopioRepository;
import cl.donaciones.logistica.repository.EnvioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticaServiceImpl implements LogisticaService {

    private final CentroAcopioRepository centroRepo;
    private final EnvioRepository        envioRepo;

    // ── Centros de acopio ────────────────────────────────────────────────────

    @Override
    public List<CentroAcopioResponse> getAllCentros() {
        return centroRepo.findAll().stream().map(CentroAcopioResponse::from).toList();
    }

    @Override
    public List<CentroAcopioResponse> getCentrosActivos() {
        return centroRepo.findByActivoTrue().stream().map(CentroAcopioResponse::from).toList();
    }

    @Override
    public CentroAcopioResponse getCentroById(Long id) {
        return CentroAcopioResponse.from(findCentroOrThrow(id));
    }

    @Override
    @Transactional
    public CentroAcopioResponse crearCentro(CentroAcopioRequest req) {
        CentroAcopio centro = CentroAcopio.builder()
                .nombre(req.getNombre())
                .direccion(req.getDireccion())
                .region(req.getRegion())
                .capacidad(req.getCapacidad())
                .responsable(req.getResponsable())
                .telefono(req.getTelefono())
                .build();
        CentroAcopio saved = centroRepo.save(centro);
        log.info("Centro de acopio creado id={} nombre={}", saved.getId(), saved.getNombre());
        return CentroAcopioResponse.from(saved);
    }

    @Override
    @Transactional
    public CentroAcopioResponse actualizarCentro(Long id, CentroAcopioRequest req) {
        CentroAcopio c = findCentroOrThrow(id);
        if (req.getNombre()      != null) c.setNombre(req.getNombre());
        if (req.getDireccion()   != null) c.setDireccion(req.getDireccion());
        if (req.getRegion()      != null) c.setRegion(req.getRegion());
        if (req.getCapacidad()   != null) c.setCapacidad(req.getCapacidad());
        if (req.getResponsable() != null) c.setResponsable(req.getResponsable());
        if (req.getTelefono()    != null) c.setTelefono(req.getTelefono());
        return CentroAcopioResponse.from(centroRepo.save(c));
    }

    @Override
    @Transactional
    public void eliminarCentro(Long id) {
        if (!centroRepo.existsById(id)) throw new IllegalArgumentException("Centro no encontrado: " + id);
        centroRepo.deleteById(id);
        log.info("Centro de acopio eliminado id={}", id);
    }

    // ── Envíos ────────────────────────────────────────────────────────────────

    @Override
    public List<EnvioResponse> getAllEnvios() {
        return envioRepo.findAll().stream().map(EnvioResponse::from).toList();
    }

    @Override
    public List<EnvioResponse> getEnviosByEstado(String estadoStr) {
        return envioRepo.findByEstado(parseEstado(estadoStr)).stream()
                .map(EnvioResponse::from).toList();
    }

    @Override
    public List<EnvioResponse> getEnviosByCentro(Long centroId) {
        return envioRepo.findByCentroOrigenId(centroId).stream()
                .map(EnvioResponse::from).toList();
    }

    @Override
    public EnvioResponse getEnvioById(Long id) {
        return EnvioResponse.from(findEnvioOrThrow(id));
    }

    @Override
    @Transactional
    public EnvioResponse crearEnvio(EnvioRequest req) {
        CentroAcopio centro = findCentroOrThrow(req.getCentroOrigenId());

        Envio envio = Envio.builder()
                .centroOrigen(centro)
                .destino(req.getDestino())
                .descripcion(req.getDescripcion())
                .estado(req.getEstado() != null ? parseEstado(req.getEstado()) : Envio.Estado.PLANIFICADO)
                .responsable(req.getResponsable())
                .fechaSalida(req.getFechaSalida())
                .fechaEntregaEst(req.getFechaEntregaEst())
                .fechaEntregaReal(req.getFechaEntregaReal())
                .donacionId(req.getDonacionId())
                .build();

        Envio saved = envioRepo.save(envio);
        log.info("Envío creado id={} destino={}", saved.getId(), saved.getDestino());
        return EnvioResponse.from(saved);
    }

    @Override
    @Transactional
    public EnvioResponse actualizarEnvio(Long id, EnvioRequest req) {
        Envio e = findEnvioOrThrow(id);
        if (req.getCentroOrigenId()   != null) e.setCentroOrigen(findCentroOrThrow(req.getCentroOrigenId()));
        if (req.getDestino()          != null) e.setDestino(req.getDestino());
        if (req.getDescripcion()      != null) e.setDescripcion(req.getDescripcion());
        if (req.getEstado()           != null) e.setEstado(parseEstado(req.getEstado()));
        if (req.getResponsable()      != null) e.setResponsable(req.getResponsable());
        if (req.getFechaSalida()      != null) e.setFechaSalida(req.getFechaSalida());
        if (req.getFechaEntregaEst()  != null) e.setFechaEntregaEst(req.getFechaEntregaEst());
        if (req.getFechaEntregaReal() != null) e.setFechaEntregaReal(req.getFechaEntregaReal());
        if (req.getDonacionId()       != null) e.setDonacionId(req.getDonacionId());
        return EnvioResponse.from(envioRepo.save(e));
    }

    @Override
    @Transactional
    public EnvioResponse actualizarEstadoEnvio(Long id, String estadoStr) {
        Envio e = findEnvioOrThrow(id);
        e.setEstado(parseEstado(estadoStr));
        return EnvioResponse.from(envioRepo.save(e));
    }

    @Override
    @Transactional
    public void eliminarEnvio(Long id) {
        if (!envioRepo.existsById(id)) throw new IllegalArgumentException("Envío no encontrado: " + id);
        envioRepo.deleteById(id);
        log.info("Envío eliminado id={}", id);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CentroAcopio findCentroOrThrow(Long id) {
        return centroRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Centro de acopio no encontrado: " + id));
    }

    private Envio findEnvioOrThrow(Long id) {
        return envioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Envío no encontrado: " + id));
    }

    private Envio.Estado parseEstado(String s) {
        try { return Envio.Estado.valueOf(s.toUpperCase().replace(" ", "_")); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado inválido: " + s +
                    ". Valores: PLANIFICADO, EN_TRANSITO, ENTREGADO, CANCELADO");
        }
    }
}
