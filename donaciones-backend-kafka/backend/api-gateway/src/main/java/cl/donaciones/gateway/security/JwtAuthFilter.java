package cl.donaciones.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Filtro WebFlux que intercepta todas las peticiones,
 * valida el JWT del IDaaS (Keycloak, JWKS) y propaga la identidad al contexto reactivo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter implements WebFilter {

    private final ReactiveJwtDecoder jwtDecoder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Preflight CORS del navegador: no lleva token; lo resuelve la configuración CORS del gateway
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        // Rutas públicas – no requieren token
        if (path.startsWith("/api/auth/") || path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Petición sin token a {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return jwtDecoder.decode(authHeader.substring(7))
                .flatMap(jwt -> {
                    String username = jwt.getClaimAsString("preferred_username");   // Keycloak
                    if (username == null) username = jwt.getClaimAsString("username");   // Cognito
                    if (username == null) username = jwt.getSubject();
                    List<GrantedAuthority> authorities = authorities(jwt);
                    String roles = authorities.stream().map(GrantedAuthority::getAuthority)
                            .map(r -> r.substring("ROLE_".length())).collect(java.util.stream.Collectors.joining(","));

                    log.debug("JWT válido – usuario={}, roles={}, path={}", username, roles, path);

                    // Propagar usuario autenticado aguas abajo (headers internos)
                    String user = username;
                    ServerWebExchange mutated = exchange.mutate()
                            .request(r -> r.header("X-Auth-User", user).header("X-Auth-Role", roles))
                            .build();
                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    return chain.filter(mutated)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                })
                .onErrorResume(org.springframework.security.oauth2.jwt.JwtException.class,
                        e -> {
                            log.warn("Token inválido para {}: {}", path, e.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        });
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
