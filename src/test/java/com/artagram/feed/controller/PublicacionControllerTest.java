package com.artagram.feed.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.common.JwtUserResolver.UsuarioAutenticado;
import com.artagram.feed.service.PublicacionService;
import com.artagram.feed.service.SubidaPublicacionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PublicacionControllerTest {

    @Test
    void aceptaElFormularioQueEnviaElFrontend() throws Exception {
        UUID autorId = UUID.randomUUID();
        JwtUserResolver resolver = mock(JwtUserResolver.class);
        when(resolver.resolverRequerido(any())).thenReturn(new UsuarioAutenticado(autorId, "lucia", null, "USUARIO", true));
        SubidaPublicacionService subida = mock(SubidaPublicacionService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new PublicacionController(mock(PublicacionService.class), subida, resolver)).build();

        mvc.perform(multipart("/api/publicaciones")
                        .file(new MockMultipartFile("obraFinal", "o.png", "image/png", new byte[]{1}))
                        .file(new MockMultipartFile("imagenesProceso", "p.png", "image/png", new byte[]{2}))
                        .param("titulo", "Jardines")
                        .param("etiquetas", "acuarela", "paisaje"))
                .andExpect(status().isCreated());

        verify(subida).publicar(eq(autorId), eq(true), eq("Jardines"), any(), any(),
                eq(List.of("acuarela", "paisaje")), any(), any());
    }
}
