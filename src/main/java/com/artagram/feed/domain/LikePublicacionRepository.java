package com.artagram.feed.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikePublicacionRepository extends JpaRepository<LikePublicacion, LikePublicacion.LikeId> {
    boolean existsById_PublicacionIdAndId_UsuarioId(UUID publicacionId, UUID usuarioId);
    void deleteById_PublicacionIdAndId_UsuarioId(UUID publicacionId, UUID usuarioId);
    long countById_PublicacionId(UUID publicacionId);
}
