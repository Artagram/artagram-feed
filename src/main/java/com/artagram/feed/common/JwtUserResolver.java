package com.artagram.feed.common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtUserResolver {

    private final RsaTokenValidator validator;

    public JwtUserResolver(RsaTokenValidator validator) {
        this.validator = validator;
    }

    public record UsuarioAutenticado(UUID id, String nombre, String correo, String roles, boolean premium) {
    }

    public Optional<UsuarioAutenticado> resolverOpcional(HttpServletRequest request) {
        String headerUserId = request.getHeader("X-User-Id");
        if (headerUserId != null && !headerUserId.isBlank()) {
            String roles = request.getHeader("X-User-Roles");
            String nombre = request.getHeader("X-User-Nombre");
            if (nombre == null) nombre = "artista";
            return Optional.of(new UsuarioAutenticado(UUID.fromString(headerUserId), nombre, null, roles, false));
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = validator.validarYExtraer(token);
                UUID id = UUID.fromString(claims.getSubject());
                String nombre = claims.get("nombre", String.class);
                String roles = claims.get("roles", String.class);
                Boolean premium = claims.get("premium", Boolean.class);
                return Optional.of(new UsuarioAutenticado(id, nombre != null ? nombre : "artista", claims.get("email", String.class), roles, Boolean.TRUE.equals(premium)));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public UsuarioAutenticado resolverRequerido(HttpServletRequest request) {
        return resolverOpcional(request)
                .orElseThrow(() -> ApiException.unauthorized("NO_AUTENTICADO", "Se requiere token de autenticación"));
    }
}
