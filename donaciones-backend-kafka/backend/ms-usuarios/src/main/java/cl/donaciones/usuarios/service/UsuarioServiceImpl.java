package cl.donaciones.usuarios.service;

import cl.donaciones.usuarios.dto.UsuarioDTOs.*;
import cl.donaciones.usuarios.model.Usuario;
import cl.donaciones.usuarios.repository.UsuarioRepository;
import cl.donaciones.usuarios.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email ya registrado: " + request.getEmail());
        }

        Usuario.Rol rol;
        try {
            rol = Usuario.Rol.valueOf(request.getRol() != null ? request.getRol().toUpperCase() : "USER");
        } catch (IllegalArgumentException e) {
            rol = Usuario.Rol.USER;
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(rol)
                .build();

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario registrado id={} email={}", saved.getId(), saved.getEmail());

        String token = jwtService.generateToken(saved.getEmail(), saved.getRol().name(), saved.getId());
        return TokenResponse.of(token, saved);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!usuario.getActivo()) {
            throw new IllegalStateException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        log.info("Login exitoso usuario id={}", usuario.getId());
        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol().name(), usuario.getId());
        return TokenResponse.of(token, usuario);
    }

    @Override
    public List<UsuarioResponse> getAll() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Override
    public UsuarioResponse getById(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
    }

    @Override
    public UsuarioResponse getByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
    }

    @Override
    @Transactional
    public UsuarioResponse update(Long id, UpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));

        if (request.getNombre() != null) usuario.setNombre(request.getNombre());
        if (request.getActivo() != null) usuario.setActivo(request.getActivo());
        if (request.getRol()    != null) {
            try {
                usuario.setRol(Usuario.Rol.valueOf(request.getRol().toUpperCase()));
            } catch (IllegalArgumentException ignored) { /* mantiene rol actual */ }
        }

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado: " + id);
        }
        usuarioRepository.deleteById(id);
        log.info("Usuario eliminado id={}", id);
    }
}
