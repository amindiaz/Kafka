package cl.donaciones.logistica.service;

import cl.donaciones.logistica.dto.LogisticaDTOs.*;

import java.util.List;

public interface LogisticaService {
    // Centros de acopio
    List<CentroAcopioResponse> getAllCentros();
    List<CentroAcopioResponse> getCentrosActivos();
    CentroAcopioResponse getCentroById(Long id);
    CentroAcopioResponse crearCentro(CentroAcopioRequest request);
    CentroAcopioResponse actualizarCentro(Long id, CentroAcopioRequest request);
    void eliminarCentro(Long id);

    // Envíos
    List<EnvioResponse> getAllEnvios();
    List<EnvioResponse> getEnviosByEstado(String estado);
    List<EnvioResponse> getEnviosByCentro(Long centroId);
    EnvioResponse getEnvioById(Long id);
    EnvioResponse crearEnvio(EnvioRequest request);
    EnvioResponse actualizarEnvio(Long id, EnvioRequest request);
    EnvioResponse actualizarEstadoEnvio(Long id, String estado);
    void eliminarEnvio(Long id);
}
