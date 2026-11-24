package cl.donaciones.logistica.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * Valida el JWT emitido por el IDaaS (Keycloak) contra su JWKS (firma RS256, expiración).
 * El {@link JwtDecoder} se configura con spring.security.oauth2.resourceserver.jwt.jwk-set-uri.
 * Los roles se leen de realm_access.roles (ADMIN, OPERADOR, USER).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Jwt jwt = jwtDecoder.decode(authHeader.substring(7));
            String username = jwt.getClaimAsString("preferred_username");   // Keycloak
            if (username == null) username = jwt.getClaimAsString("username");   // Cognito (access token)
            if (username == null) username = jwt.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var auth = new UsernamePasswordAuthenticationToken(username, null, authorities(jwt));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            log.warn("Token inválido en ms-logistica: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /** Roles desde Keycloak (realm_access.roles) o Cognito (cognito:groups); sin roles → USER. */
    private List<GrantedAuthority> authorities(Jwt jwt) {
        Collection<?> roles = rolesDe(jwt);
        if (roles.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return roles.stream()
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()))
                .toList();
    }

    private static Collection<?> rolesDe(Jwt jwt) {
        if (jwt.getClaim("cognito:groups") instanceof Collection<?> g && !g.isEmpty()) {
            return g;
        }
        if (jwt.getClaim("realm_access") instanceof Map<?, ?> m && m.get("roles") instanceof Collection<?> r) {
            return r;
        }
        return List.of();
    }
}
