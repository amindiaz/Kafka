package cl.donaciones.necesidades.config;

import cl.donaciones.necesidades.security.JwtAuthFilter;
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
 * Reglas de acceso para ms-necesidades:
 *  - Cualquier usuario autenticado puede REPORTAR necesidades (POST)
 *  - Lectura abierta a todos los autenticados (GET)
 *  - Actualización de estado: OPERADOR o ADMIN
 *  - Eliminación: solo ADMIN
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
                        .requestMatchers(HttpMethod.GET, "/api/necesidades/**").hasAnyRole("USER", "OPERADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/necesidades/**").hasAnyRole("USER", "OPERADOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/necesidades/**").hasAnyRole("OPERADOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/necesidades/**").hasAnyRole("OPERADOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/necesidades/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
