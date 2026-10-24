package com.artagram.feed.service;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.LikePublicacion;
import com.artagram.feed.domain.LikePublicacionRepository;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.SeguidorProyeccionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.domain.UsuarioProyeccionRepository;
import com.artagram.feed.domain.VersionPublicacion;
import com.artagram.feed.domain.VersionPublicacionRepository;
import com.artagram.feed.dto.CrearPublicacionRequest;
import com.artagram.feed.dto.EditarPublicacionRequest;
import com.artagram.feed.dto.FeedResponse;
import com.artagram.feed.dto.LikeResponse;
import com.artagram.feed.dto.PublicacionDto;
import com.artagram.feed.events.FeedEventPublisher;
import com.artagram.feed.storage.StorageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicacionService {

    private final PublicacionRepository publicacionRepo;
    private final VersionPublicacionRepository versionRepo;
    private final LikePublicacionRepository likeRepo;
    private final UsuarioProyeccionRepository usuarioRepo;
    private final SeguidorProyeccionRepository seguidorRepo;
    private final StorageService storageService;
    private final FeedEventPublisher eventPublisher;

    public PublicacionService(PublicacionRepository publicacionRepo,
                              VersionPublicacionRepository versionRepo,
                              LikePublicacionRepository likeRepo,
                              UsuarioProyeccionRepository usuarioRepo,
                              SeguidorProyeccionRepository seguidorRepo,
                              StorageService storageService,
                              FeedEventPublisher eventPublisher) {
        this.publicacionRepo = publicacionRepo;
        this.versionRepo = versionRepo;
        this.likeRepo = likeRepo;
        this.usuarioRepo = usuarioRepo;
        this.seguidorRepo = seguidorRepo;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    public Page<PublicacionDto> listar(String tag, String orden, String busqueda, int pagina, int tamano, UUID usuarioActualId) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        Page<Publicacion> posts;

        if (busqueda != null && !busqueda.isBlank()) {
            posts = publicacionRepo.buscarPorTexto(busqueda.trim(), pageable);
        } else if (tag != null && !tag.isBlank()) {
            posts = publicacionRepo.findByEtiqueta(tag.trim(), pageable);
        } else if ("populares".equalsIgnoreCase(orden)) {
            posts = publicacionRepo.findAllByOrderByContadorLikesDescCreadoEnDesc(pageable);
        } else {
            posts = publicacionRepo.findAllByOrderByCreadoEnDesc(pageable);
        }

        return posts.map(p -> aDto(p, usuarioActualId));
    }

    @Transactional
    public PublicacionDto obtenerPorId(UUID id, UUID usuarioActualId) {
        Publicacion p = publicacionRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));
        p.setContadorVistas(p.getContadorVistas() + 1);
        publicacionRepo.save(p);
        return aDto(p, usuarioActualId);
    }

    @Transactional
    public PublicacionDto crear(UUID autorId, CrearPublicacionRequest req) {
        UsuarioProyeccion autor = usuarioRepo.findById(autorId)
                .orElseGet(() -> usuarioRepo.save(new UsuarioProyeccion(autorId, "artista_" + autorId.toString().substring(0, 6), "Artista", null, false)));

        Set<String> etiquetas = req.etiquetas() != null ? new HashSet<>(req.etiquetas()) : new HashSet<>();
        Publicacion p = new Publicacion(UUID.randomUUID(), autor, req.titulo(), req.descripcion(), req.tecnica(), req.imagenUrl(), etiquetas);
        p = publicacionRepo.save(p);

        if (req.imagenesProcesoUrls() != null && !req.imagenesProcesoUrls().isEmpty()) {
            int num = 1;
            for (String urlProc : req.imagenesProcesoUrls()) {
                versionRepo.save(new VersionPublicacion(UUID.randomUUID(), p, num++, urlProc, "Paso del proceso"));
            }
        }

        eventPublisher.publicarPostPublicado(p.getId(), autor.getId(), p.getTitulo(), p.getImagenUrl());
        return aDto(p, autorId);
    }

    @Transactional
    public PublicacionDto editar(UUID publicacionId, UUID usuarioId, boolean esAdmin, EditarPublicacionRequest req) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));

        if (!p.getAutor().getId().equals(usuarioId) && !esAdmin) {
            throw ApiException.forbidden("SIN_PERMISO", "No tienes permiso para editar esta publicación");
        }

        p.setTitulo(req.titulo());
        p.setDescripcion(req.descripcion());
        p.setTecnica(req.tecnica());
        if (req.etiquetas() != null) {
            p.setEtiquetas(new HashSet<>(req.etiquetas()));
        }
        p.setActualizadoEn(Instant.now());
        p = publicacionRepo.save(p);

        return aDto(p, usuarioId);
    }

    @Transactional
    public void eliminar(UUID publicacionId, UUID usuarioId, boolean esAdmin) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));

        if (!p.getAutor().getId().equals(usuarioId) && !esAdmin) {
            throw ApiException.forbidden("SIN_PERMISO", "No tienes permiso para eliminar esta publicación");
        }

        publicacionRepo.delete(p);
    }

    @Transactional
    public LikeResponse toggleLike(UUID publicacionId, UUID usuarioId) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));

        boolean yaDioLike = likeRepo.existsById_PublicacionIdAndId_UsuarioId(publicacionId, usuarioId);
        boolean nuevoEstadoMeGusta;

        if (yaDioLike) {
            likeRepo.deleteById_PublicacionIdAndId_UsuarioId(publicacionId, usuarioId);
            p.setContadorLikes(p.getContadorLikes() - 1);
            nuevoEstadoMeGusta = false;
        } else {
            UsuarioProyeccion u = usuarioRepo.findById(usuarioId)
                    .orElseGet(() -> usuarioRepo.save(new UsuarioProyeccion(usuarioId, "artista_" + usuarioId.toString().substring(0, 6), "Artista", null, false)));
            likeRepo.save(new LikePublicacion(p, u));
            p.setContadorLikes(p.getContadorLikes() + 1);
            nuevoEstadoMeGusta = true;
        }

        publicacionRepo.save(p);
        eventPublisher.publicarLikeModificado(publicacionId, usuarioId, p.getAutor().getId(), p.getContadorLikes(), nuevoEstadoMeGusta);

        return new LikeResponse(nuevoEstadoMeGusta, p.getContadorLikes());
    }

    public FeedResponse feedSiguiendo(UUID usuarioId, int pagina, int tamano) {
        List<UUID> seguidosIds = seguidorRepo.findSeguidosIdsBySeguidorId(usuarioId);
        if (seguidosIds.isEmpty()) {
            return new FeedResponse(Collections.emptyList(), null, null, false, List.of("sam_pixel", "lucia_acuarela", "noa_tinta"));
        }

        Pageable pageable = PageRequest.of(pagina, tamano);
        Page<Publicacion> posts = publicacionRepo.findByAutorIdInOrderByCreadoEnDesc(seguidosIds, pageable);

        List<PublicacionDto> dtos = posts.getContent().stream()
                .map(p -> aDto(p, usuarioId))
                .toList();

        return new FeedResponse(dtos, null, null, posts.hasNext(), Collections.emptyList());
    }

    public FeedResponse feedExplorar(UUID usuarioActualId, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        Page<Publicacion> posts = publicacionRepo.findAllByOrderByContadorLikesDescCreadoEnDesc(pageable);
        List<PublicacionDto> dtos = posts.getContent().stream()
                .map(p -> aDto(p, usuarioActualId))
                .toList();
        return new FeedResponse(dtos, null, null, posts.hasNext(), List.of("arte", "acuarela", "digital"));
    }

    public PublicacionDto aDto(Publicacion p, UUID usuarioActualId) {
        List<String> versiones = versionRepo.findByPublicacionIdOrderByNumeroVersionAsc(p.getId()).stream()
                .map(v -> storageService.resolverUrlPublica(v.getImagenUrl()))
                .toList();

        boolean meGusta = false;
        if (usuarioActualId != null) {
            meGusta = likeRepo.existsById_PublicacionIdAndId_UsuarioId(p.getId(), usuarioActualId);
        }

        String autorNombre = p.getAutor() != null ? p.getAutor().getNombreArtista() : "artista";
        UUID autorId = p.getAutor() != null ? p.getAutor().getId() : null;

        return new PublicacionDto(
                p.getId(),
                autorId,
                autorNombre,
                p.getTitulo(),
                p.getDescripcion(),
                storageService.resolverUrlPublica(p.getImagenUrl()),
                versiones,
                new ArrayList<>(p.getEtiquetas()),
                p.getContadorLikes(),
                meGusta,
                p.getCreadoEn().toString(),
                p.getContadorComentarios(),
                p.getVersion(),
                p.getActualizadoEn() != null ? p.getActualizadoEn().toString() : null
        );
    }
}
