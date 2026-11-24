package cl.donaciones.donaciones.config;

import cl.donaciones.donaciones.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad del microservicio donaciones.
 *
 * El API-Gateway ya valida el JWT antes de rutear la petición aquí.
 * Este filtro re-establece el contexto de seguridad para que
 * @PreAuthorize funcione correctamente.
 *
 * Reglas de acceso:
 *  - GET (lectura)            → USER, OPERADOR, ADMIN
 *  - POST / PUT / PATCH       → OPERADOR, ADMIN
 *  - DELETE                   → solo ADMIN
 *  - /actuator/**             → sin autenticación (health-check)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/donaciones/**").hasAnyRole("USER", "OPERADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/donaciones/**").hasAnyRole("OPERADOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/donaciones/**").hasAnyRole("OPERADOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/donaciones/**").hasAnyRole("OPERADOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/donaciones/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
