package com.artagram.feed.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtUserResolverTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final RsaTokenValidator validator = mock(RsaTokenValidator.class);
    private final JwtUserResolver resolver = new JwtUserResolver(validator);

    /** Lo que llega desde el gateway: el token original mas las cabeceras X-User-*. */
    private MockHttpServletRequest peticionDelGateway() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-User-Id", usuarioId.toString());
        request.addHeader("X-User-Name", "lucia");
        request.addHeader("X-User-Role", "ADMIN");
        return request;
    }

    @Test
    void conTokenSeLeenPremiumYRolAunqueVenganLasCabeceras() {
        Claims claims = Jwts.claims().subject(usuarioId.toString()).add("nombre", "lucia")
                .add("rol", "ADMIN").add("premium", true).build();
        when(validator.validarYExtraer(any())).thenReturn(claims);

        var usuario = resolver.resolverRequerido(peticionDelGateway());

        assertThat(usuario.premium()).isTrue();
        assertThat(usuario.roles()).isEqualTo("ADMIN");
    }

    @Test
    void sinTokenLasCabecerasDanIdentidadPeroNoPremium() {
        MockHttpServletRequest request = peticionDelGateway();
        request.removeHeader("Authorization");

        var usuario = resolver.resolverRequerido(request);

        assertThat(usuario.id()).isEqualTo(usuarioId);
        assertThat(usuario.nombre()).isEqualTo("lucia");
        assertThat(usuario.roles()).isEqualTo("ADMIN");
        assertThat(usuario.premium()).isFalse();
    }
}
