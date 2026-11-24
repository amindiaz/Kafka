package cl.donaciones.usuarios.service;

import cl.donaciones.usuarios.dto.UsuarioDTOs.*;
import cl.donaciones.usuarios.model.Usuario;
import cl.donaciones.usuarios.repository.UsuarioRepository;
import cl.donaciones.usuarios.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder   passwordEncoder;
    @Mock JwtService        jwtService;
    @InjectMocks UsuarioServiceImpl service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Ana Pérez")
                .email("ana@example.com")
                .password("$2a$10$hashedPassword")
                .rol(Usuario.Rol.USER)
                .activo(true)
                .build();
    }

    @Test
    void register_newEmail_returnsToken() {
        RegisterRequest req = new RegisterRequest();
        req.setNombre("Ana Pérez"); req.setEmail("ana@example.com"); req.setPassword("password123");

        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("jwt-token");

        TokenResponse r = service.register(req);
        assertThat(r.getToken()).isEqualTo("jwt-token");
        assertThat(r.getNombre()).isEqualTo("Ana Pérez");
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("ana@example.com"); req.setNombre("X"); req.setPassword("123456");
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya registrado");
    }

    @Test
    void login_validCredentials_returnsToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ana@example.com"); req.setPassword("password123");

        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password123", usuario.getPassword())).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("jwt-token");

        TokenResponse r = service.login(req);
        assertThat(r.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_wrongPassword_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ana@example.com"); req.setPassword("wrong");

        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", usuario.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    void login_inactiveUser_throwsException() {
        usuario.setActivo(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("ana@example.com"); req.setPassword("pass");

        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> service.login(req)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getAll_returnsAllUsers() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));
        assertThat(service.getAll()).hasSize(1);
        assertThat(service.getAll().get(0).getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void getById_found_returnsResponse() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        UsuarioResponse r = service.getById(1L);
        assertThat(r.getId()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_throwsException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_existingId_callsDelete() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);
        service.delete(1L);
        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void delete_notFound_throwsException() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
