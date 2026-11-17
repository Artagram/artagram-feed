package com.artagram.feed.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.feed.domain.AnalisisIa;
import com.artagram.feed.domain.AnalisisIaRepository;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.SeguidorProyeccion;
import com.artagram.feed.domain.SeguidorProyeccionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.domain.UsuarioProyeccionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class EventosFeedTest {

    private final UsuarioProyeccionRepository usuarios = mock(UsuarioProyeccionRepository.class);
    private final SeguidorProyeccionRepository seguidores = mock(SeguidorProyeccionRepository.class);
    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final AnalisisIaRepository analisis = mock(AnalisisIaRepository.class);
    private final FeedEventListener listener = new FeedEventListener(usuarios, seguidores, publicaciones, analisis);
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    private Publicacion publicacion() {
        return new Publicacion(UUID.randomUUID(), new UsuarioProyeccion(a, "ana", "ana", null, false),
                "Mar", "d", "oleo", "/media/x.png", Set.of());
    }

    @Test
    void usuarioRegistradoCreaOActualizaLaProyeccion() {
        UsuarioProyeccion existente = new UsuarioProyeccion(a, "viejo", "viejo", null, false);
        when(usuarios.findById(a)).thenReturn(Optional.of(existente));
        when(usuarios.findById(b)).thenReturn(Optional.empty());

        listener.onUserRegistered(Map.of("usuarioId", a.toString(), "nombreArtista", "ana", "premium", true, "avatar", "/a.png"));
        listener.onUserRegistered(Map.of("usuarioId", b.toString(), "nombreArtista", "sam"));
        listener.onUserRegistered(Map.of("usuarioId", "no-uuid"));

        assertThat(existente.getNombreArtista()).isEqualTo("ana");
        assertThat(existente.isPremium()).isTrue();
        assertThat(existente.getFotoUrl()).isEqualTo("/a.png");
        verify(usuarios, times(2)).save(any());
    }

    @Test
    void usuarioEliminadoYSeguimientos() {
        when(seguidores.existsById_SeguidorIdAndId_SeguidoId(a, b)).thenReturn(false, true);

        listener.onUserDeleted(Map.of("usuarioId", a.toString()));
        listener.onUserDeleted(Map.of("usuarioId", "mal"));
        listener.onFollowCreated(Map.of("seguidorId", a.toString(), "seguidoId", b.toString()));
        listener.onFollowCreated(Map.of("seguidorId", a.toString(), "seguidoId", b.toString()));
        listener.onFollowCreated(Map.of("seguidorId", "mal"));
        listener.onFollowDeleted(Map.of("seguidorId", a.toString(), "seguidoId", b.toString()));
        listener.onFollowDeleted(Map.of("seguidorId", "mal"));

        verify(usuarios).deleteById(a);
        verify(seguidores, times(1)).save(any(SeguidorProyeccion.class));
        verify(seguidores).deleteById_SeguidorIdAndId_SeguidoId(a, b);
    }

    @Test
    void mediaProcesadaEnriqueceLaPublicacion() {
        Publicacion p = publicacion();
        Publicacion sinCambios = publicacion();
        when(publicaciones.findById(p.getId())).thenReturn(Optional.of(p));
        when(publicaciones.findById(sinCambios.getId())).thenReturn(Optional.of(sinCambios));

        listener.onMediaProcessed(Map.of("publicacionId", p.getId().toString(), "miniaturaUrl", "/m.png",
                "blurhash", "LKO2", "marcaAguaRuta", "agua/x.png"));
        listener.onMediaProcessed(Map.of("publicacionId", sinCambios.getId().toString()));
        listener.onMediaProcessed(Map.of("publicacionId", "mal"));

        assertThat(p.getMiniaturaUrl()).isEqualTo("/m.png");
        assertThat(p.getBlurhash()).isEqualTo("LKO2");
        assertThat(p.getMarcaAguaRuta()).isEqualTo("agua/x.png");
        verify(publicaciones, times(2)).save(any());
    }

    @Test
    void analisisCompletadoSePersiste() {
        Publicacion p = publicacion();
        when(publicaciones.findById(p.getId())).thenReturn(Optional.of(p));
        AnalisisIa previo = new AnalisisIa();
        when(analisis.findTopByPublicacionIdOrderByCreadoEnDesc(p.getId())).thenReturn(Optional.of(previo), Optional.empty());

        Map<String, Object> completo = new HashMap<>(Map.of("publicacionId", p.getId().toString(), "estado", "COMPLETADO",
                "diagnosticoGeneral", "bien", "paletaDetectada", "azules", "composicion", "regla de tercios",
                "puntosMejora", "luz", "sugerencias", "más contraste", "puntuacionArmonia", 72));
        listener.onAiCompleted(completo);
        listener.onAiCompleted(Map.of("publicacionId", p.getId().toString()));
        listener.onAiCompleted(Map.of("publicacionId", "mal"));

        assertThat(previo.getPuntuacionArmonia()).isEqualTo(72);
        assertThat(previo.getPaletaDetectada()).isEqualTo("azules");
        ArgumentCaptor<AnalisisIa> guardado = ArgumentCaptor.forClass(AnalisisIa.class);
        verify(analisis, times(2)).save(guardado.capture());
        assertThat(guardado.getAllValues().get(1).getPuntuacionArmonia()).isEqualTo(85);
        assertThat(guardado.getAllValues().get(1).getEstado()).isEqualTo("COMPLETADO");
    }

    @Test
    void elPublicadorEmiteCadaEvento() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        FeedEventPublisher pub = new FeedEventPublisher(rabbit);
        UUID p = UUID.randomUUID();

        pub.publicarPostPublicado(p, a, null, "Mar", "/x.png");
        pub.publicarPostModerado(p, a, "Mar", "OCULTADA");
        pub.publicarLikeModificado(p, b, null, a, 3, true);
        pub.publicarLikeModificado(p, b, "sam", a, 2, false);
        pub.publicarComentarioCreado(UUID.randomUUID(), p, b, "sam", a, "hola", 1);
        pub.publicarComentarioEliminado(UUID.randomUUID(), p, 0);
        pub.solicitarAnalisisIa(p, a, "/x.png", null, null);

        for (String rk : List.of("post.published", "post.moderated", "like.created", "comment.created",
                "comment.deleted", "ai.analyze")) {
            verify(rabbit).convertAndSend(eq("artagram.events"), eq(rk), any(Object.class));
        }
        verify(rabbit, times(2)).convertAndSend(eq("artagram.events"), eq("like.updated"), any(Object.class));
        verify(rabbit, never()).convertAndSend(anyString(), eq("otro"), any(Object.class));
    }

    @Test
    void laConfiguracionDeclaraTodasLasColas() {
        FeedRabbitMqConfig c = new FeedRabbitMqConfig();
        var ex = c.eventsExchange();
        assertThat(c.jsonMessageConverter()).isNotNull();
        List<Queue> colas = List.of(c.feedUserRegisteredQueue(), c.feedUserDeletedQueue(), c.feedFollowCreatedQueue(),
                c.feedFollowDeletedQueue(), c.feedMediaProcessedQueue(), c.feedCanvasClosedQueue(), c.feedAiCompletedQueue());
        List<Binding> bindings = List.of(c.feedUserRegisteredBinding(colas.get(0), ex), c.feedUserDeletedBinding(colas.get(1), ex),
                c.feedFollowCreatedBinding(colas.get(2), ex), c.feedFollowDeletedBinding(colas.get(3), ex),
                c.feedMediaProcessedBinding(colas.get(4), ex), c.feedCanvasClosedBinding(colas.get(5), ex),
                c.feedAiCompletedBinding(colas.get(6), ex));

        assertThat(colas).allSatisfy(q -> assertThat(q.getArguments()).containsEntry("x-dead-letter-exchange", "artagram.dlx"));
        assertThat(bindings).hasSize(7).extracting(Binding::getRoutingKey).contains("user.registered", "ai.completed");

        var id = new SeguidorProyeccion(a, b).getId();
        assertThat(id).isEqualTo(new SeguidorProyeccion.SeguidorId(a, b)).isEqualTo(id).isNotEqualTo("x")
                .isNotEqualTo(new SeguidorProyeccion.SeguidorId(b, a));
        assertThat(id.hashCode()).isEqualTo(new SeguidorProyeccion.SeguidorId(a, b).hashCode());
        assertThat(id.getSeguidorId()).isEqualTo(a);
        assertThat(id.getSeguidoId()).isEqualTo(b);
        assertThat(new SeguidorProyeccion(a, b).getCreadoEn()).isNotNull();
        assertThat(new SeguidorProyeccion().getId()).isNull();
        assertThat(new SeguidorProyeccion.SeguidorId().getSeguidoId()).isNull();
    }
}
