package com.artagram.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.ComentarioPublicacion;
import com.artagram.feed.domain.ComentarioPublicacionRepository;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.domain.UsuarioProyeccionRepository;
import com.artagram.feed.domain.VersionPublicacion;
import com.artagram.feed.domain.VersionPublicacionRepository;
import com.artagram.feed.dto.CrearComentarioRequest;
import com.artagram.feed.dto.CrearVersionRequest;
import com.artagram.feed.events.FeedEventPublisher;
import com.artagram.feed.storage.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComentarioYVersionServiceTest {

    private final UUID autorId = UUID.randomUUID();
    private final UUID otroId = UUID.randomUUID();
    private final UsuarioProyeccion autor = new UsuarioProyeccion(autorId, "ana", "ana", null, false);
    private final UsuarioProyeccion otro = new UsuarioProyeccion(otroId, "sam", "sam", null, false);
    private Publicacion publicacion;

    private ComentarioPublicacionRepository comentarios;
    private PublicacionRepository publicaciones;
    private UsuarioProyeccionRepository usuarios;
    private FeedEventPublisher eventos;
    private ComentarioService comentarioService;

    private VersionPublicacionRepository versiones;
    private StorageService storage;
    private VersionService versionService;

    @BeforeEach
    void setUp() {
        publicacion = new Publicacion(UUID.randomUUID(), autor, "Mar", "d", "oleo", "/x.png", Set.of());
        comentarios = mock(ComentarioPublicacionRepository.class);
        publicaciones = mock(PublicacionRepository.class);
        usuarios = mock(UsuarioProyeccionRepository.class);
        eventos = mock(FeedEventPublisher.class);
        comentarioService = new ComentarioService(comentarios, publicaciones, usuarios, eventos);
        versiones = mock(VersionPublicacionRepository.class);
        storage = mock(StorageService.class);
        versionService = new VersionService(versiones, publicaciones, storage);
        when(comentarios.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(versiones.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(usuarios.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storage.resolverUrlPublica(any())).thenAnswer(inv -> "http://cdn/" + inv.getArgument(0));
        when(publicaciones.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
    }

    @Test
    void crearYListarComentarios() {
        when(usuarios.findById(otroId)).thenReturn(Optional.of(otro));
        var dto = comentarioService.crear(publicacion.getId(), otroId, new CrearComentarioRequest("  hola  "));

        assertThat(dto.contenido()).isEqualTo("hola");
        assertThat(dto.autorNombre()).isEqualTo("sam");
        assertThat(publicacion.getContadorComentarios()).isEqualTo(1);
        verify(eventos).publicarComentarioCreado(any(), eq(publicacion.getId()), eq(otroId), eq("sam"), eq(autorId), eq("hola"), eq(1));

        when(comentarios.findByPublicacionIdOrderByCreadoEnAsc(publicacion.getId()))
                .thenReturn(List.of(new ComentarioPublicacion(null, publicacion, otro, "hola")));
        assertThat(comentarioService.listarPorPublicacion(publicacion.getId())).hasSize(1);
    }

    @Test
    void comentarDeUnUsuarioSinProyeccionLaCrea() {
        UUID nuevo = UUID.randomUUID();
        when(usuarios.findById(nuevo)).thenReturn(Optional.empty());

        assertThat(comentarioService.crear(publicacion.getId(), nuevo, new CrearComentarioRequest("x")).autorNombre())
                .isEqualTo("Artista");
        assertThatThrownBy(() -> comentarioService.crear(UUID.randomUUID(), nuevo, new CrearComentarioRequest("x")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void eliminarComentarioSegunPermisos() {
        ComentarioPublicacion delOtro = new ComentarioPublicacion(null, publicacion, otro, "x");
        UUID extraño = UUID.randomUUID();
        when(comentarios.findById(delOtro.getId())).thenReturn(Optional.of(delOtro));

        assertThatThrownBy(() -> comentarioService.eliminar(publicacion.getId(), delOtro.getId(), extraño, false))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("SIN_PERMISO");
        comentarioService.eliminar(publicacion.getId(), delOtro.getId(), otroId, false);
        comentarioService.eliminar(publicacion.getId(), delOtro.getId(), autorId, false);
        comentarioService.eliminar(publicacion.getId(), delOtro.getId(), extraño, true);

        assertThat(publicacion.getContadorComentarios()).isZero();
        verify(eventos, org.mockito.Mockito.times(3)).publicarComentarioEliminado(delOtro.getId(), publicacion.getId(), 0);
        assertThatThrownBy(() -> comentarioService.eliminar(publicacion.getId(), UUID.randomUUID(), autorId, true))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("COMENTARIO_NO_ENCONTRADO");
    }

    @Test
    void versionesSoloLasAgregaElAutorOUnAdmin() {
        when(versiones.findByPublicacionIdOrderByNumeroVersionAsc(publicacion.getId()))
                .thenReturn(List.of(new VersionPublicacion(null, publicacion, 1, "v1.png", "boceto")));

        var v = versionService.crear(publicacion.getId(), autorId, false, new CrearVersionRequest("v2.png", "color"));
        assertThat(v.numeroVersion()).isEqualTo(2);
        assertThat(v.imagenUrl()).isEqualTo("http://cdn/v2.png");
        assertThat(versionService.crear(publicacion.getId(), otroId, true, new CrearVersionRequest("v3.png", null))).isNotNull();
        assertThat(versionService.listarPorPublicacion(publicacion.getId())).singleElement()
                .extracting("notaCambio").isEqualTo("boceto");

        assertThatThrownBy(() -> versionService.crear(publicacion.getId(), otroId, false, new CrearVersionRequest("x", null)))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("SIN_PERMISO");
        assertThatThrownBy(() -> versionService.crear(UUID.randomUUID(), autorId, true, new CrearVersionRequest("x", null)))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PUBLICACION_NO_ENCONTRADA");
        verify(versiones, never()).save(eq(null));
    }
}
