package com.artagram.feed.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.common.GlobalExceptionHandler;
import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.common.JwtUserResolver.UsuarioAutenticado;
import com.artagram.feed.dto.ComentarioDto;
import com.artagram.feed.dto.DescargaResponse;
import com.artagram.feed.dto.PresignResponse;
import com.artagram.feed.dto.SugerenciasEtiquetasResponse;
import com.artagram.feed.service.AnalisisIaService;
import com.artagram.feed.service.ComentarioService;
import com.artagram.feed.service.PublicacionService;
import com.artagram.feed.service.SubidaPublicacionService;
import com.artagram.feed.service.VersionService;
import com.artagram.feed.storage.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControladoresFeedTest {

    private final UUID yo = UUID.randomUUID();
    private final UUID obra = UUID.randomUUID();
    private JwtUserResolver resolver;
    private PublicacionService publicaciones;
    private ComentarioService comentarios;
    private VersionService versiones;
    private AnalisisIaService analisis;
    private StorageService storage;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        resolver = mock(JwtUserResolver.class);
        publicaciones = mock(PublicacionService.class);
        comentarios = mock(ComentarioService.class);
        versiones = mock(VersionService.class);
        analisis = mock(AnalisisIaService.class);
        storage = mock(StorageService.class);
        UsuarioAutenticado admin = new UsuarioAutenticado(yo, "ana", "a@c.com", "ADMIN", true);
        when(resolver.resolverRequerido(any())).thenReturn(admin);
        when(resolver.resolverOpcional(any())).thenReturn(Optional.of(admin));
        mvc = MockMvcBuilders.standaloneSetup(
                        new PublicacionController(publicaciones, mock(SubidaPublicacionService.class), resolver),
                        new ComentarioController(comentarios, resolver),
                        new VersionController(versiones, resolver),
                        new AnalisisIaController(analisis, resolver),
                        new MediaController(storage, resolver))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void endpointsDePublicaciones() throws Exception {
        when(publicaciones.listar(any(), any(), any(), anyInt(), anyInt(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 12), 0));
        when(publicaciones.listarPorArtista(any(), anyInt(), anyInt(), any(), anyBoolean())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 12), 0));
        when(publicaciones.listarDestacadas(any(), any(), anyBoolean())).thenReturn(List.of());
        when(publicaciones.urlDescarga(obra, yo, true)).thenReturn(new DescargaResponse("https://cdn/x.jpg"));

        mvc.perform(get("/api/publicaciones").param("q", "mar")).andExpect(status().isOk());
        mvc.perform(get("/api/publicaciones/" + obra)).andExpect(status().isOk());
        mvc.perform(put("/api/publicaciones/" + obra).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Nuevo\"}")).andExpect(status().isOk());
        mvc.perform(delete("/api/publicaciones/" + obra)).andExpect(status().isNoContent());
        mvc.perform(get("/api/publicaciones/artista/" + yo)).andExpect(status().isOk());
        mvc.perform(get("/api/publicaciones/artista/" + yo + "/destacadas")).andExpect(status().isOk());
        mvc.perform(get("/api/publicaciones/" + obra + "/descargar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://cdn/x.jpg"));
        mvc.perform(put("/api/publicaciones/" + obra + "/destacar")).andExpect(status().isOk());
        mvc.perform(delete("/api/publicaciones/" + obra + "/destacar")).andExpect(status().isOk());
        mvc.perform(post("/api/publicaciones/" + obra + "/like")).andExpect(status().isOk());

        verify(publicaciones).obtenerPorId(obra, yo, true);
        verify(publicaciones).eliminar(obra, yo, true);
        verify(publicaciones).destacar(obra, yo, true);
        verify(publicaciones).quitarDestacada(obra, yo);
        verify(publicaciones).toggleLike(obra, yo);
    }

    @Test
    void sinSesionLasConsultasPublicasUsanUsuarioNulo() throws Exception {
        when(resolver.resolverOpcional(any())).thenReturn(Optional.empty());
        when(publicaciones.listar(any(), any(), any(), anyInt(), anyInt(), isNull())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 12), 0));

        mvc.perform(get("/api/publicaciones")).andExpect(status().isOk());
        mvc.perform(get("/api/publicaciones/" + obra)).andExpect(status().isOk());
        verify(publicaciones).obtenerPorId(obra, null, false);
    }

    @Test
    void comentariosVersionesYAnalisis() throws Exception {
        when(comentarios.crear(eq(obra), eq(yo), any()))
                .thenReturn(new ComentarioDto(UUID.randomUUID(), obra, yo, "ana", "hola", "2026-01-01T00:00:00Z"));
        when(analisis.sugerirEtiquetas(any(), eq(yo), eq(true)))
                .thenReturn(new SugerenciasEtiquetasResponse("t", "d", List.of("oleo")));
        UUID comentario = UUID.randomUUID();

        mvc.perform(get("/api/publicaciones/" + obra + "/comentarios")).andExpect(status().isOk());
        mvc.perform(post("/api/publicaciones/" + obra + "/comentarios").contentType(MediaType.APPLICATION_JSON)
                .content("{\"contenido\":\"hola\"}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido").value("hola"));
        mvc.perform(delete("/api/publicaciones/" + obra + "/comentarios/" + comentario)).andExpect(status().isNoContent());
        mvc.perform(get("/api/publicaciones/" + obra + "/versiones")).andExpect(status().isOk());
        mvc.perform(post("/api/publicaciones/" + obra + "/versiones").contentType(MediaType.APPLICATION_JSON)
                .content("{\"imagenUrl\":\"v.png\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/publicaciones/" + obra + "/analisis")).andExpect(status().isOk());
        mvc.perform(post("/api/publicaciones/" + obra + "/analisis")).andExpect(status().isOk());
        mvc.perform(post("/api/ia/sugerencias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"imagenBase64\":\"AAAA\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.etiquetas[0]").value("oleo"));

        verify(comentarios).eliminar(obra, comentario, yo, true);
        verify(versiones).crear(eq(obra), eq(yo), eq(true), any());
        verify(analisis).solicitarAnalisis(obra, yo, true);
    }

    @Test
    void mediaPresignYServirArchivos() throws Exception {
        when(storage.generarUrlSubida("a.png", "image/png")).thenReturn(new PresignResponse("https://s3/up", "https://cdn/a.png", "k"));
        when(storage.leer(any())).thenReturn(new ByteArrayResource(new byte[]{1, 2}));

        mvc.perform(post("/api/media/presign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"filename\":\"a.png\",\"contentType\":\"image/png\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.publicUrl").value("https://cdn/a.png"));
        mvc.perform(get("/media/publicaciones/a.png")).andExpect(header().string("Content-Type", "image/png"));
        mvc.perform(get("/media/publicaciones/a.webp")).andExpect(header().string("Content-Type", "image/webp"));
        mvc.perform(get("/media/publicaciones/a.jpg")).andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @Test
    void erroresUsanElFormatoComun() throws Exception {
        when(comentarios.listarPorPublicacion(obra))
                .thenThrow(ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "no"))
                .thenThrow(new IllegalStateException("boom"));

        mvc.perform(get("/api/publicaciones/" + obra + "/comentarios")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PUBLICACION_NO_ENCONTRADA"));
        mvc.perform(get("/api/publicaciones/" + obra + "/comentarios")).andExpect(status().isInternalServerError());
        mvc.perform(post("/api/publicaciones/" + obra + "/comentarios").contentType(MediaType.APPLICATION_JSON)
                .content("{\"contenido\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/publicaciones/" + obra + "/comentarios").contentType(MediaType.APPLICATION_JSON)
                .content("{x")).andExpect(status().isBadRequest());
    }
}
