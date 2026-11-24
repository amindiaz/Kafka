package cl.donaciones.usuarios.service;

import cl.donaciones.usuarios.dto.UsuarioDTOs.*;

import java.util.List;

public interface UsuarioService {
    TokenResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    List<UsuarioResponse> getAll();
    UsuarioResponse getById(Long id);
    UsuarioResponse getByEmail(String email);
    UsuarioResponse update(Long id, UpdateRequest request);
    void delete(Long id);
}
