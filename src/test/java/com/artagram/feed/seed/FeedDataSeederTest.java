package com.artagram.feed.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.feed.domain.ComentarioPublicacionRepository;
import com.artagram.feed.domain.LikePublicacionRepository;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.SeguidorProyeccionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.domain.UsuarioProyeccionRepository;
import com.artagram.feed.domain.VersionPublicacionRepository;
import com.artagram.feed.storage.StorageService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class FeedDataSeederTest {

    private final UsuarioProyeccionRepository usuarios = mock(UsuarioProyeccionRepository.class);
    private final SeguidorProyeccionRepository seguidores = mock(SeguidorProyeccionRepository.class);
    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final VersionPublicacionRepository versiones = mock(VersionPublicacionRepository.class);
    private final LikePublicacionRepository likes = mock(LikePublicacionRepository.class);
    private final ComentarioPublicacionRepository comentarios = mock(ComentarioPublicacionRepository.class);
    private final StorageService storage = mock(StorageService.class);

    private FeedDataSeeder seeder() {
        return new FeedDataSeeder(usuarios, seguidores, publicaciones, versiones, likes, comentarios, storage,
                new DefaultResourceLoader());
    }

    @Test
    void siembraUsuariosSeguidoresYObrasConSusImagenes() {
        when(usuarios.findById(any())).thenAnswer(inv -> Optional.of(
                new UsuarioProyeccion(inv.getArgument(0), "u", "U", null, false)));
        when(publicaciones.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storage.guardar(anyString(), anyString(), any(), eq("image/jpeg"))).thenReturn("publicaciones/x.jpg");

        seeder().run();

        verify(usuarios, atLeastOnce()).save(any());
        verify(seguidores, times(2)).save(any());
        verify(publicaciones, times(6)).save(any());
        verify(likes, times(6)).save(any());
        verify(comentarios, times(6)).save(any());
        verify(storage, atLeastOnce()).guardar(anyString(), anyString(), any(), eq("image/jpeg"));
    }

    @Test
    void siYaHayDatosNoDuplicaNada() {
        when(usuarios.existsById(any())).thenReturn(true);
        when(seguidores.existsById_SeguidorIdAndId_SeguidoId(any(), any())).thenReturn(true);
        when(publicaciones.existsById(any())).thenReturn(true);

        seeder().run();

        verify(usuarios, never()).save(any());
        verify(seguidores, never()).save(any());
        verify(publicaciones, never()).save(any());
    }

    @Test
    void siGuardarUnaImagenFallaSigueConLaSiguiente() {
        when(usuarios.findById(any())).thenAnswer(inv -> Optional.of(
                new UsuarioProyeccion(inv.getArgument(0), "u", "U", null, false)));
        when(publicaciones.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storage.guardar(anyString(), anyString(), any(), anyString())).thenThrow(new IllegalStateException("disco lleno"));
        when(likes.existsById_PublicacionIdAndId_UsuarioId(any(), any())).thenReturn(true);

        seeder().run();

        verify(publicaciones, times(6)).save(any());
        verify(likes, never()).save(any());
    }
}
