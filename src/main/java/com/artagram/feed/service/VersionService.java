package com.artagram.feed.service;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.VersionPublicacion;
import com.artagram.feed.domain.VersionPublicacionRepository;
import com.artagram.feed.dto.CrearVersionRequest;
import com.artagram.feed.dto.VersionPublicacionDto;
import com.artagram.feed.storage.StorageService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VersionService {

    private final VersionPublicacionRepository versionRepo;
    private final PublicacionRepository publicacionRepo;
    private final StorageService storageService;

    public VersionService(VersionPublicacionRepository versionRepo,
                          PublicacionRepository publicacionRepo,
                          StorageService storageService) {
        this.versionRepo = versionRepo;
        this.publicacionRepo = publicacionRepo;
        this.storageService = storageService;
    }

    public List<VersionPublicacionDto> listarPorPublicacion(UUID publicacionId) {
        return versionRepo.findByPublicacionIdOrderByNumeroVersionAsc(publicacionId).stream()
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public VersionPublicacionDto crear(UUID publicacionId, UUID usuarioId, boolean esAdmin, CrearVersionRequest req) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));

        if (!p.getAutor().getId().equals(usuarioId) && !esAdmin) {
            throw ApiException.forbidden("SIN_PERMISO", "No tienes permiso para agregar versiones a esta obra");
        }

        List<VersionPublicacion> existentes = versionRepo.findByPublicacionIdOrderByNumeroVersionAsc(publicacionId);
        int siguienteNumero = existentes.size() + 1;

        VersionPublicacion version = new VersionPublicacion(
                UUID.randomUUID(), p, siguienteNumero, req.imagenUrl(), req.notaCambio()
        );
        version = versionRepo.save(version);

        return aDto(version);
    }

    private VersionPublicacionDto aDto(VersionPublicacion v) {
        return new VersionPublicacionDto(
                v.getId(),
                v.getPublicacion().getId(),
                v.getNumeroVersion(),
                storageService.resolverUrlPublica(v.getImagenUrl()),
                v.getNotaCambio(),
                v.getCreadoEn().toString()
        );
    }
}
