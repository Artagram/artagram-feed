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
        return desdeToken(request).or(() -> desdeCabeceras(request));
    }

    /** Va primero: el gateway reenvia el token y solo ahi viene el claim premium. */
    private Optional<UsuarioAutenticado> desdeToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            Claims claims = validator.validarYExtraer(authHeader.substring(7));
            UUID id = UUID.fromString(claims.getSubject());
            String nombre = claims.get("nombre", String.class);
            String rol = claims.get("rol", String.class);
            Boolean premium = claims.get("premium", Boolean.class);
            return Optional.of(new UsuarioAutenticado(id, nombre != null ? nombre : "artista", claims.get("email", String.class), rol, Boolean.TRUE.equals(premium)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<UsuarioAutenticado> desdeCabeceras(HttpServletRequest request) {
        String headerUserId = request.getHeader("X-User-Id");
        if (headerUserId == null || headerUserId.isBlank()) {
            return Optional.empty();
        }
        String nombre = request.getHeader("X-User-Name");
        return Optional.of(new UsuarioAutenticado(UUID.fromString(headerUserId), nombre != null ? nombre : "artista",
                null, request.getHeader("X-User-Role"), false));
    }

    public UsuarioAutenticado resolverRequerido(HttpServletRequest request) {
        return resolverOpcional(request)
                .orElseThrow(() -> ApiException.unauthorized("NO_AUTENTICADO", "Se requiere token de autenticación"));
    }
}
