package com.artagram.feed.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FeedEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public FeedEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPostPublicado(UUID publicacionId, UUID autorId, String titulo, String imagenUrl) {
        Map<String, Object> evento = Map.of(
                "tipo", "POST_PUBLICADO",
                "publicacionId", publicacionId.toString(),
                "autorId", autorId.toString(),
                "titulo", titulo,
                "imagenUrl", imagenUrl,
                "timestamp", Instant.now().toString()
        );
        log.info("Publicando post.published para publicacionId={}", publicacionId);
        rabbitTemplate.convertAndSend(FeedRabbitMqConfig.EXCHANGE, "post.published", evento);
    }

    public void publicarLikeModificado(UUID publicacionId, UUID usuarioId, UUID autorId, int contadorLikes, boolean meGusta) {
        Map<String, Object> evento = Map.of(
                "tipo", "LIKES_ACTUALIZADOS",
                "publicacionId", publicacionId.toString(),
                "usuarioId", usuarioId.toString(),
                "autorId", autorId.toString(),
                "contadorLikes", contadorLikes,
                "meGusta", meGusta,
                "timestamp", Instant.now().toString()
        );
        log.info("Publicando like.updated para publicacionId={}", publicacionId);
        rabbitTemplate.convertAndSend(FeedRabbitMqConfig.EXCHANGE, "like.updated", evento);
        if (meGusta) {
            rabbitTemplate.convertAndSend(FeedRabbitMqConfig.EXCHANGE, "like.created", evento);
        }
    }

    public void publicarComentarioCreado(UUID comentarioId, UUID publicacionId, UUID autorId, String autorNombre,
                                         UUID publicacionAutorId, String contenido, int contadorComentarios) {
        Map<String, Object> evento = Map.of(
                "tipo", "COMENTARIO_CREADO",
                "comentarioId", comentarioId.toString(),
                "publicacionId", publicacionId.toString(),
                "autorId", autorId.toString(),
                "autorNombre", autorNombre,
                "publicacionAutorId", publicacionAutorId.toString(),
                "contenido", contenido,
                "contadorComentarios", contadorComentarios,
                "timestamp", Instant.now().toString()
        );
        log.info("Publicando comment.created para publicacionId={}", publicacionId);
        rabbitTemplate.convertAndSend(FeedRabbitMqConfig.EXCHANGE, "comment.created", evento);
    }

    public void publicarComentarioEliminado(UUID comentarioId, UUID publicacionId, int contadorComentarios) {
        Map<String, Object> evento = Map.of(
                "tipo", "COMENTARIO_ELIMINADO",
                "comentarioId", comentarioId.toString(),
                "publicacionId", publicacionId.toString(),
                "contadorComentarios", contadorComentarios,
                "timestamp", Instant.now().toString()
        );
        log.info("Publicando comment.deleted para publicacionId={}", publicacionId);
        rabbitTemplate.convertAndSend(FeedRabbitMqConfig.EXCHANGE, "comment.deleted", evento);
    }

    public void solicitarAnalisisIa(UUID publicacionId, UUID autorId, String imagenUrl, String titulo, String descripcion) {
        Map<String, Object> evento = Map.of(
                "tipo", "SOLICITUD_ANALISIS",
                "publicacionId", publicacionId.toString(),
                "autorId", autorId.toString(),
                "imagenUrl", imagenUrl,
                "titulo", titulo != null ? titulo : "",
                "descripcion", descripcion != null ? descripcion : "",
                "timestamp", Instant.now().toString()
        );
        log.info("Publicando ai.analyze para publicacionId={}", publicacionId);
        rabbitTemplate.convertAndSend(FeedRabbitMqConfig.EXCHANGE, "ai.analyze", evento);
    }
}
