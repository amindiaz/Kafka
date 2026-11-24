package cl.donaciones.necesidades.service;

import cl.donaciones.necesidades.dto.NecesidadDTOs.*;

import java.util.List;

public interface NecesidadService {
    List<NecesidadResponse> getAll();
    List<NecesidadResponse> getByEstado(String estado);
    List<NecesidadResponse> getByPrioridad(String prioridad);
    List<NecesidadResponse> getByUsuario(Long usuarioId);
    NecesidadResponse getById(Long id);
    NecesidadResponse crear(NecesidadRequest request);
    NecesidadResponse actualizar(Long id, NecesidadRequest request);
    NecesidadResponse actualizarEstado(Long id, String estado);
    void eliminar(Long id);
}
