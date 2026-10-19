package com.artagram.feed.events;

import com.artagram.feed.domain.AnalisisIa;
import com.artagram.feed.domain.AnalisisIaRepository;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.SeguidorProyeccion;
import com.artagram.feed.domain.SeguidorProyeccionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.domain.UsuarioProyeccionRepository;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FeedEventListener {

    private static final Logger log = LoggerFactory.getLogger(FeedEventListener.class);

    private final UsuarioProyeccionRepository usuarioRepo;
    private final SeguidorProyeccionRepository seguidorRepo;
    private final PublicacionRepository publicacionRepo;
    private final AnalisisIaRepository analisisRepo;

    public FeedEventListener(UsuarioProyeccionRepository usuarioRepo,
                             SeguidorProyeccionRepository seguidorRepo,
                             PublicacionRepository publicacionRepo,
                             AnalisisIaRepository analisisRepo) {
        this.usuarioRepo = usuarioRepo;
        this.seguidorRepo = seguidorRepo;
        this.publicacionRepo = publicacionRepo;
        this.analisisRepo = analisisRepo;
    }

    @RabbitListener(queues = FeedRabbitMqConfig.QUEUE_USER_REGISTERED)
    @Transactional
    public void onUserRegistered(Map<String, Object> payload) {
        try {
            UUID id = UUID.fromString((String) payload.get("id"));
            String nombreArtista = (String) payload.get("nombreArtista");
            String correo = (String) payload.get("correo");
            Boolean premium = (Boolean) payload.get("premium");

            UsuarioProyeccion usuario = usuarioRepo.findById(id).orElse(new UsuarioProyeccion());
            usuario.setNombreUsuario(nombreArtista);
            usuario.setNombreArtista(nombreArtista);
            usuario.setPremium(Boolean.TRUE.equals(premium));
            if (payload.get("avatar") != null) {
                usuario.setFotoUrl((String) payload.get("avatar"));
            }
            usuarioRepo.save(usuario);
            log.info("Usuario proyección actualizado en Feed: id={}, nombre={}", id, nombreArtista);
        } catch (Exception ex) {
            log.error("Error procesando user.registered en Feed: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = FeedRabbitMqConfig.QUEUE_USER_DELETED)
    @Transactional
    public void onUserDeleted(Map<String, Object> payload) {
        try {
            UUID id = UUID.fromString((String) payload.get("id"));
            usuarioRepo.deleteById(id);
            log.info("Usuario proyección eliminado en Feed: id={}", id);
        } catch (Exception ex) {
            log.error("Error procesando user.deleted en Feed: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = FeedRabbitMqConfig.QUEUE_FOLLOW_CREATED)
    @Transactional
    public void onFollowCreated(Map<String, Object> payload) {
        try {
            UUID seguidorId = UUID.fromString((String) payload.get("seguidorId"));
            UUID seguidoId = UUID.fromString((String) payload.get("seguidoId"));
            if (!seguidorRepo.existsById_SeguidorIdAndId_SeguidoId(seguidorId, seguidoId)) {
                seguidorRepo.save(new SeguidorProyeccion(seguidorId, seguidoId));
                log.info("Seguidor proyección registrado: {} -> {}", seguidorId, seguidoId);
            }
        } catch (Exception ex) {
            log.error("Error procesando follow.created en Feed: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = FeedRabbitMqConfig.QUEUE_FOLLOW_DELETED)
    @Transactional
    public void onFollowDeleted(Map<String, Object> payload) {
        try {
            UUID seguidorId = UUID.fromString((String) payload.get("seguidorId"));
            UUID seguidoId = UUID.fromString((String) payload.get("seguidoId"));
            seguidorRepo.deleteById_SeguidorIdAndId_SeguidoId(seguidorId, seguidoId);
            log.info("Seguidor proyección eliminado: {} -> {}", seguidorId, seguidoId);
        } catch (Exception ex) {
            log.error("Error procesando follow.deleted en Feed: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = FeedRabbitMqConfig.QUEUE_MEDIA_PROCESSED)
    @Transactional
    public void onMediaProcessed(Map<String, Object> payload) {
        try {
            UUID publicacionId = UUID.fromString((String) payload.get("publicacionId"));
            String miniaturaUrl = (String) payload.get("miniaturaUrl");
            String blurhash = (String) payload.get("blurhash");

            publicacionRepo.findById(publicacionId).ifPresent(pub -> {
                if (miniaturaUrl != null) pub.setMiniaturaUrl(miniaturaUrl);
                if (blurhash != null) pub.setBlurhash(blurhash);
                publicacionRepo.save(pub);
                log.info("Publicación {} enriquecida con miniatura y blurhash por media worker", publicacionId);
            });
        } catch (Exception ex) {
            log.error("Error procesando media.processed en Feed: {}", ex.getMessage());
        }
    }

    @RabbitListener(queues = FeedRabbitMqConfig.QUEUE_AI_COMPLETED)
    @Transactional
    public void onAiCompleted(Map<String, Object> payload) {
        try {
            UUID publicacionId = UUID.fromString((String) payload.get("publicacionId"));
            String estado = (String) payload.getOrDefault("estado", "COMPLETADO");
            String diagnostico = (String) payload.get("diagnosticoGeneral");
            String paleta = (String) payload.get("paletaDetectada");
            String composicion = (String) payload.get("composicion");
            String puntosMejora = (String) payload.get("puntosMejora");
            String sugerencias = (String) payload.get("sugerencias");
            Number score = (Number) payload.get("puntuacionArmonia");

            publicacionRepo.findById(publicacionId).ifPresent(pub -> {
                AnalisisIa analisis = analisisRepo.findTopByPublicacionIdOrderByCreadoEnDesc(publicacionId)
                        .orElse(new AnalisisIa());
                analisis.setPublicacion(pub);
                analisis.setEstado(estado);
                analisis.setDiagnosticoGeneral(diagnostico);
                analisis.setPaletaDetectada(paleta);
                analisis.setComposicion(composicion);
                analisis.setPuntosMejora(puntosMejora);
                analisis.setSugerencias(sugerencias);
                analisis.setPuntuacionArmonia(score != null ? score.intValue() : 85);
                analisisRepo.save(analisis);
                log.info("Análisis IA persistido para publicación {}", publicacionId);
            });
        } catch (Exception ex) {
            log.error("Error procesando ai.completed en Feed: {}", ex.getMessage());
        }
    }
}
