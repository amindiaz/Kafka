package cl.donaciones.usuarios.dto;

import cl.donaciones.usuarios.model.Usuario;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

public class UsuarioDTOs {

    @Data
    public static class RegisterRequest {
        @NotBlank
        private String nombre;

        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 6)
        private String password;

        private String rol = "USER";
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    public static class TokenResponse {
        private String token;
        private String tipo = "Bearer";
        private Long usuarioId;
        private String nombre;
        private String rol;

        public static TokenResponse of(String token, Usuario u) {
            TokenResponse r = new TokenResponse();
            r.token     = token;
            r.usuarioId = u.getId();
            r.nombre    = u.getNombre();
            r.rol       = u.getRol().name();
            return r;
        }
    }

    @Data
    public static class UsuarioResponse {
        private Long id;
        private String nombre;
        private String email;
        private String rol;
        private Boolean activo;
        private LocalDateTime createdAt;

        public static UsuarioResponse from(Usuario u) {
            UsuarioResponse r = new UsuarioResponse();
            r.id        = u.getId();
            r.nombre    = u.getNombre();
            r.email     = u.getEmail();
            r.rol       = u.getRol().name();
            r.activo    = u.getActivo();
            r.createdAt = u.getCreatedAt();
            return r;
        }
    }

    @Data
    public static class UpdateRequest {
        private String nombre;
        private String rol;
        private Boolean activo;
    }
}
