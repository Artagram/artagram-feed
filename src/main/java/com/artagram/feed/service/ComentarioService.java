package com.artagram.feed.service;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.ComentarioPublicacion;
import com.artagram.feed.domain.ComentarioPublicacionRepository;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.domain.UsuarioProyeccionRepository;
import com.artagram.feed.dto.ComentarioDto;
import com.artagram.feed.dto.CrearComentarioRequest;
import com.artagram.feed.events.FeedEventPublisher;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComentarioService {

    private final ComentarioPublicacionRepository comentarioRepo;
    private final PublicacionRepository publicacionRepo;
    private final UsuarioProyeccionRepository usuarioRepo;
    private final FeedEventPublisher eventPublisher;

    public ComentarioService(ComentarioPublicacionRepository comentarioRepo,
                             PublicacionRepository publicacionRepo,
                             UsuarioProyeccionRepository usuarioRepo,
                             FeedEventPublisher eventPublisher) {
        this.comentarioRepo = comentarioRepo;
        this.publicacionRepo = publicacionRepo;
        this.usuarioRepo = usuarioRepo;
        this.eventPublisher = eventPublisher;
    }

    public List<ComentarioDto> listarPorPublicacion(UUID publicacionId) {
        return comentarioRepo.findByPublicacionIdOrderByCreadoEnAsc(publicacionId).stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public ComentarioDto crear(UUID publicacionId, UUID autorId, CrearComentarioRequest req) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));

        UsuarioProyeccion autor = usuarioRepo.findById(autorId)
                .orElseGet(() -> usuarioRepo.save(new UsuarioProyeccion(autorId, "artista_" + autorId.toString().substring(0, 6), "Artista", null, false)));

        ComentarioPublicacion com = new ComentarioPublicacion(UUID.randomUUID(), p, autor, req.contenido().trim());
        com = comentarioRepo.save(com);

        p.setContadorComentarios(p.getContadorComentarios() + 1);
        publicacionRepo.save(p);

        eventPublisher.publicarComentarioCreado(
                com.getId(), p.getId(), autor.getId(), autor.getNombreArtista(),
                p.getAutor().getId(), com.getContenido(), p.getContadorComentarios()
        );

        return aDto(com);
    }

    @Transactional
    public void eliminar(UUID publicacionId, UUID comentarioId, UUID usuarioId, boolean esAdmin) {
        ComentarioPublicacion com = comentarioRepo.findById(comentarioId)
                .orElseThrow(() -> ApiException.notFound("COMENTARIO_NO_ENCONTRADO", "El comentario no existe"));

        Publicacion p = com.getPublicacion();
        boolean esAutorComentario = com.getAutor().getId().equals(usuarioId);
        boolean esAutorPublicacion = p.getAutor().getId().equals(usuarioId);

        if (!esAutorComentario && !esAutorPublicacion && !esAdmin) {
            throw ApiException.forbidden("SIN_PERMISO", "No tienes permiso para eliminar este comentario");
        }

        comentarioRepo.delete(com);
        p.setContadorComentarios(Math.max(0, p.getContadorComentarios() - 1));
        publicacionRepo.save(p);

        eventPublisher.publicarComentarioEliminado(comentarioId, publicacionId, p.getContadorComentarios());
    }

    private ComentarioDto aDto(ComentarioPublicacion c) {
        return new ComentarioDto(
                c.getId(),
                c.getPublicacion().getId(),
                c.getAutor().getId(),
                c.getAutor().getNombreArtista(),
                c.getContenido(),
                c.getCreadoEn().toString()
        );
    }
}
