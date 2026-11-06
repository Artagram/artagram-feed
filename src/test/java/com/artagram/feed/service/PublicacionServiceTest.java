package com.artagram.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import com.artagram.feed.dto.CrearPublicacionRequest;
import com.artagram.feed.dto.LikeResponse;
import com.artagram.feed.dto.PublicacionDto;
import com.artagram.feed.events.FeedEventPublisher;
import com.artagram.feed.storage.StorageService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceTest {

    @Mock
    private PublicacionRepository publicacionRepo;
    @Mock
    private VersionPublicacionRepository versionRepo;
    @Mock
    private LikePublicacionRepository likeRepo;
    @Mock
    private UsuarioProyeccionRepository usuarioRepo;
    @Mock
    private SeguidorProyeccionRepository seguidorRepo;
    @Mock
    private StorageService storageService;
    @Mock
    private FeedEventPublisher eventPublisher;
    @Mock
    private PlanService planService;

    private PublicacionService service;

    @BeforeEach
    void setUp() {
        service = new PublicacionService(publicacionRepo, versionRepo, likeRepo, usuarioRepo, seguidorRepo, storageService, eventPublisher, planService);
    }

    @Test
    void crearPublicacionExitosa() {
        UUID autorId = UUID.randomUUID();
        UsuarioProyeccion autor = new UsuarioProyeccion(autorId, "lucia_acuarela", "Lucía Mora", "avatar.jpg", false);
        when(usuarioRepo.findById(autorId)).thenReturn(Optional.of(autor));

        when(publicacionRepo.save(any(Publicacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.resolverUrlPublica(any())).thenAnswer(inv -> "http://cdn/" + inv.getArgument(0));

        CrearPublicacionRequest req = new CrearPublicacionRequest(
                "Mi Acuarela", "Paisaje de montaña", "Acuarela", "publicaciones/img.jpg",
                List.of("versiones/proc1.jpg"), List.of("acuarela", "paisaje")
        );

        PublicacionDto dto = service.crear(autorId, req);

        assertThat(dto).isNotNull();
        assertThat(dto.titulo()).isEqualTo("Mi Acuarela");
        assertThat(dto.autorNombre()).isEqualTo("Lucía Mora");
        verify(eventPublisher).publicarPostPublicado(any(), any(), any(), any(), any());
    }

    @Test
    void toggleLikeAlternaCorrectamente() {
        UUID pubId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UsuarioProyeccion autor = new UsuarioProyeccion(UUID.randomUUID(), "autor", "Autor", null, false);
        Publicacion p = new Publicacion(pubId, autor, "Obra", "Desc", "Oleo", "img.jpg", Set.of());
        p.setContadorLikes(5);

        when(publicacionRepo.findById(pubId)).thenReturn(Optional.of(p));
        when(likeRepo.existsById_PublicacionIdAndId_UsuarioId(pubId, usuarioId)).thenReturn(false);
        when(usuarioRepo.findById(usuarioId)).thenReturn(Optional.of(new UsuarioProyeccion(usuarioId, "liker", "Liker", null, false)));
        when(publicacionRepo.save(any())).thenReturn(p);

        LikeResponse res = service.toggleLike(pubId, usuarioId);

        assertThat(res.meGusta()).isTrue();
        assertThat(res.contadorLikes()).isEqualTo(6);
        verify(eventPublisher).publicarLikeModificado(pubId, usuarioId, autor.getId(), 6, true);
    }

    private Publicacion obraDe(UUID autorId) {
        UsuarioProyeccion autor = new UsuarioProyeccion(autorId, "lucia", "Lucía", null, false);
        Publicacion p = new Publicacion(UUID.randomUUID(), autor, "Obra", null, null, "o.jpg", Set.of());
        when(publicacionRepo.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void elAutorDestacaYQuitaSuObra() {
        UUID autorId = UUID.randomUUID();
        Publicacion p = obraDe(autorId);
        when(publicacionRepo.save(p)).thenReturn(p);

        assertThat(service.destacar(p.getId(), autorId, false).destacada()).isTrue();
        verify(planService).verificarCupoDestacadas(autorId, false);

        assertThat(service.quitarDestacada(p.getId(), autorId).destacada()).isFalse();
    }

    @Test
    void volverADestacarNoGastaCupo() {
        UUID autorId = UUID.randomUUID();
        Publicacion p = obraDe(autorId);
        p.destacar();

        assertThat(service.destacar(p.getId(), autorId, false).destacada()).isTrue();
        verify(planService, never()).verificarCupoDestacadas(any(), anyBoolean());
    }

    @Test
    void sinCupoLaObraNoQuedaDestacada() {
        UUID autorId = UUID.randomUUID();
        Publicacion p = obraDe(autorId);
        doThrow(ApiException.forbidden("PLAN_LIMITE", "tope")).when(planService).verificarCupoDestacadas(autorId, false);

        assertThatThrownBy(() -> service.destacar(p.getId(), autorId, false)).isInstanceOf(ApiException.class);
        assertThat(p.isDestacada()).isFalse();
    }

    @Test
    void nadieMasPuedeDestacarObrasAjenas() {
        Publicacion p = obraDe(UUID.randomUUID());
        UUID otro = UUID.randomUUID();

        assertThatThrownBy(() -> service.destacar(p.getId(), otro, true))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("SIN_PERMISO"));
        assertThatThrownBy(() -> service.quitarDestacada(p.getId(), otro)).isInstanceOf(ApiException.class);
        assertThat(p.isDestacada()).isFalse();
    }

    @Test
    void laDescargaEntregaSoloLaVersionMarcada() {
        Publicacion p = obraDe(UUID.randomUUID());
        p.setTitulo("Atardecer en Cañón!");
        p.setMarcaAguaRuta("marcadas/x.jpg");
        when(storageService.urlDescarga("marcadas/x.jpg", "atardecer-en-canon-artagram.jpg")).thenReturn("https://s3/firmada");

        assertThat(service.urlDescarga(p.getId()).url()).isEqualTo("https://s3/firmada");
    }

    @Test
    void unaObraSinMarcaSeProcesaYNuncaEntregaElOriginal() {
        Publicacion p = obraDe(UUID.randomUUID());

        assertThatThrownBy(() -> service.urlDescarga(p.getId()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("MARCA_EN_PROCESO"));
        verify(eventPublisher).publicarPostPublicado(p.getId(), p.getAutor().getId(), p.getAutor().getNombreArtista(), "Obra", "o.jpg");
        verify(storageService, never()).urlDescarga(any(), any());
    }
}
