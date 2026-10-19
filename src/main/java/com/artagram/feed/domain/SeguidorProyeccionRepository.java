package com.artagram.feed.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeguidorProyeccionRepository extends JpaRepository<SeguidorProyeccion, SeguidorProyeccion.SeguidorId> {

    @Query("SELECT s.id.seguidoId FROM SeguidorProyeccion s WHERE s.id.seguidorId = :seguidorId")
    List<UUID> findSeguidosIdsBySeguidorId(@Param("seguidorId") UUID seguidorId);

    boolean existsById_SeguidorIdAndId_SeguidoId(UUID seguidorId, UUID seguidoId);

    void deleteById_SeguidorIdAndId_SeguidoId(UUID seguidorId, UUID seguidoId);
}
