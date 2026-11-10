package com.artagram.feed.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artagram.feed.common.GlobalExceptionHandler;
import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.common.JwtUserResolver.UsuarioAutenticado;
import com.artagram.feed.service.ModeracionService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ModeracionControllerTest {

    private JwtUserResolver resolver;
    private ModeracionService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        resolver = mock(JwtUserResolver.class);
        service = mock(ModeracionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ModeracionController(service, resolver))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private void comoRol(String rol) {
        when(resolver.resolverRequerido(any())).thenReturn(new UsuarioAutenticado(UUID.randomUUID(), "x", null, rol, false));
    }

    @Test
    void unUsuarioComunNoModera() throws Exception {
        comoRol("USUARIO");

        mvc.perform(get("/api/moderacion/reportes")).andExpect(status().isForbidden());
        mvc.perform(post("/api/moderacion/marcas/procesar")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void elAdminVeLosReportes() throws Exception {
        comoRol("ADMIN");

        mvc.perform(get("/api/moderacion/reportes")).andExpect(status().isOk());
    }

    @Test
    void unMotivoInexistenteEsErrorDelCliente() throws Exception {
        comoRol("USUARIO");

        mvc.perform(post("/api/publicaciones/" + UUID.randomUUID() + "/reportar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"NO_EXISTE\"}"))
                .andExpect(status().isBadRequest());
    }
}
