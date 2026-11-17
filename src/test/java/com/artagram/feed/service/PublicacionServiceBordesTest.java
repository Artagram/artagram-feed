package com.artagram.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.LikePublicacionRepository;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.SeguidorProyeccionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.domain.UsuarioProyeccionRepository;
import com.artagram.feed.domain.VersionPublicacionRepository;
import com.artagram.feed.dto.EditarPublicacionRequest;
import com.artagram.feed.events.FeedEventPublisher;
import com.artagram.feed.storage.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class PublicacionServiceBordesTest {

    private final UUID autorId = UUID.randomUUID();
    private final UUID otroId = UUID.randomUUID();
    private PublicacionRepository repo;
    private StorageService storage;
    private FeedEventPublisher eventos;
    private PublicacionService service;
    private Publicacion obra;

    @BeforeEach
    void setUp() {
        repo = mock(PublicacionRepository.class);
        storage = mock(StorageService.class);
        eventos = mock(FeedEventPublisher.class);
        service = new PublicacionService(repo, mock(VersionPublicacionRepository.class), mock(LikePublicacionRepository.class),
                mock(UsuarioProyeccionRepository.class), mock(SeguidorProyeccionRepository.class), storage, eventos,
                mock(PlanService.class));
        obra = new Publicacion(UUID.randomUUID(), new UsuarioProyeccion(autorId, "ana", "ana", null, false),
                "Árbol de Otoño!", "d", "oleo", "/x.png", Set.of("oleo"));
        when(repo.findById(obra.getId())).thenReturn(Optional.of(obra));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Page<Publicacion> pagina = new PageImpl<>(List.of(obra));
        when(repo.buscarPorTexto(eq("mar"), any())).thenReturn(pagina);
        when(repo.findByEtiqueta(eq("oleo"), any())).thenReturn(pagina);
        when(repo.findByOcultaFalseOrderByContadorLikesDescCreadoEnDesc(any())).thenReturn(pagina);
        when(repo.findByOcultaFalseOrderByCreadoEnDesc(any())).thenReturn(pagina);
        when(repo.findByAutorIdOrderByCreadoEnDesc(eq(autorId), any())).thenReturn(pagina);
        when(repo.findByAutorIdAndOcultaFalseOrderByCreadoEnDesc(eq(autorId), any())).thenReturn(Page.empty());
        when(repo.findByAutorIdAndDestacadaTrueOrderByDestacadaEnDesc(autorId)).thenReturn(List.of(obra));
        when(repo.findByAutorIdAndDestacadaTrueAndOcultaFalseOrderByDestacadaEnDesc(autorId)).thenReturn(List.of());
    }

    @Test
    void listarSegunBusquedaEtiquetaYOrden() {
        assertThat(service.listar(null, null, " mar ", 0, 12, null)).hasSize(1);
        assertThat(service.listar(" oleo ", null, "", 0, 12, null)).hasSize(1);
        assertThat(service.listar(null, "POPULARES", null, 0, 12, autorId)).hasSize(1);
        assertThat(service.listar(" ", "recientes", null, 0, 12, null)).hasSize(1);
    }

    @Test
    void listadosDelArtistaIncluyenOcultasSoloParaElAutorOAdmin() {
        assertThat(service.listarPorArtista(autorId, 0, 12, autorId, false)).hasSize(1);
        assertThat(service.listarPorArtista(autorId, 0, 12, otroId, true)).hasSize(1);
        assertThat(service.listarPorArtista(autorId, 0, 12, otroId, false)).isEmpty();
        assertThat(service.listarDestacadas(autorId, autorId, false)).hasSize(1);
        assertThat(service.listarDestacadas(autorId, null, false)).isEmpty();
    }

    @Test
    void obtenerCuentaLaVistaYOcultaSoloEsVisibleParaAutorOAdmin() {
        service.obtenerPorId(obra.getId(), null, false);
        assertThat(obra.getContadorVistas()).isEqualTo(1);

        obra.setOculta(true);
        assertThatThrownBy(() -> service.obtenerPorId(obra.getId(), otroId, false)).isInstanceOf(ApiException.class);
        service.obtenerPorId(obra.getId(), autorId, false);
        service.obtenerPorId(obra.getId(), otroId, true);
        assertThat(obra.getContadorVistas()).isEqualTo(3);
    }

    @Test
    void editarYEliminarSoloAutorOAdmin() {
        var dto = service.editar(obra.getId(), autorId, false, new EditarPublicacionRequest("Nuevo", "d2", "acuarela", List.of("a")));
        assertThat(dto.titulo()).isEqualTo("Nuevo");
        assertThat(obra.getEtiquetas()).containsExactly("a");
        service.editar(obra.getId(), otroId, true, new EditarPublicacionRequest("Otro", null, null, null));
        assertThat(obra.getEtiquetas()).containsExactly("a");

        assertThatThrownBy(() -> service.editar(obra.getId(), otroId, false, new EditarPublicacionRequest("x", null, null, null)))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("SIN_PERMISO");
        assertThatThrownBy(() -> service.editar(UUID.randomUUID(), autorId, true, new EditarPublicacionRequest("x", null, null, null)))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("PUBLICACION_NO_ENCONTRADA");

        assertThatThrownBy(() -> service.eliminar(obra.getId(), otroId, false)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.eliminar(UUID.randomUUID(), otroId, true)).isInstanceOf(ApiException.class);
        service.eliminar(obra.getId(), otroId, true);
        verify(repo).delete(obra);
    }

    @Test
    void descargaPideLaMarcaDeAguaSiAunNoExiste() {
        assertThatThrownBy(() -> service.urlDescarga(obra.getId(), autorId, false))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("MARCA_EN_PROCESO");
        verify(eventos).publicarPostPublicado(obra.getId(), autorId, "ana", obra.getTitulo(), "/x.png");

        obra.setMarcaAguaRuta("agua/x.jpg");
        when(storage.urlDescarga("agua/x.jpg", "arbol-de-otono-artagram.jpg")).thenReturn("https://cdn/descarga");
        assertThat(service.urlDescarga(obra.getId(), autorId, false).url()).isEqualTo("https://cdn/descarga");

        obra.setTitulo("¡¡!!");
        when(storage.urlDescarga("agua/x.jpg", "obra-artagram.jpg")).thenReturn("https://cdn/obra");
        assertThat(service.urlDescarga(obra.getId(), autorId, false).url()).isEqualTo("https://cdn/obra");
    }

    @Test
    void quitarDestacadaYDestacarObraAjena() {
        obra.destacar();
        assertThat(service.quitarDestacada(obra.getId(), autorId)).isNotNull();
        assertThat(obra.isDestacada()).isFalse();
        service.quitarDestacada(obra.getId(), autorId);
        assertThatThrownBy(() -> service.destacar(obra.getId(), otroId, true))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("SIN_PERMISO");
        assertThatThrownBy(() -> service.quitarDestacada(UUID.randomUUID(), autorId)).isInstanceOf(ApiException.class);
    }

    @Test
    void feedsRecienteSiguiendoYExplorar() {
        SeguidorProyeccionRepository seguidores = mock(SeguidorProyeccionRepository.class);
        PublicacionService conSeguidores = new PublicacionService(repo, mock(VersionPublicacionRepository.class),
                mock(LikePublicacionRepository.class), mock(UsuarioProyeccionRepository.class), seguidores, storage, eventos,
                mock(PlanService.class));
        when(seguidores.findSeguidosIdsBySeguidorId(otroId)).thenReturn(List.of(autorId));
        when(seguidores.findSeguidosIdsBySeguidorId(autorId)).thenReturn(List.of());
        when(repo.findByAutorIdInAndOcultaFalseOrderByCreadoEnDesc(eq(List.of(autorId)), any()))
                .thenReturn(new PageImpl<>(List.of(obra)));

        assertThat(conSeguidores.feedReciente(null, 0, 10).publicaciones()).hasSize(1);
        assertThat(conSeguidores.feedSiguiendo(otroId, 0, 10).publicaciones()).hasSize(1);
        var vacio = conSeguidores.feedSiguiendo(autorId, 0, 10);
        assertThat(vacio.publicaciones()).isEmpty();
        assertThat(vacio.artistasSugeridos()).isNotEmpty();
        assertThat(conSeguidores.feedExplorar(null, 0, 10).artistasSugeridos()).contains("arte");
    }
}
