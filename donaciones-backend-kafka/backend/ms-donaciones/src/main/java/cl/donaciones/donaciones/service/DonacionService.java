package cl.donaciones.donaciones.service;

import cl.donaciones.donaciones.dto.DonacionDTOs.*;

import java.util.List;

public interface DonacionService {

    List<DonacionResponse> getAll();

    List<DonacionResponse> getByEstado(String estado);

    List<DonacionResponse> getByUsuario(Long usuarioId);

    DonacionResponse getById(Long id);

    DonacionResponse crear(DonacionRequest request);

    DonacionResponse actualizar(Long id, DonacionRequest request);

    DonacionResponse actualizarEstado(Long id, String estado);

    void eliminar(Long id);
}
