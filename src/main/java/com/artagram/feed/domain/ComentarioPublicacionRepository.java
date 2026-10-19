package com.artagram.feed.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComentarioPublicacionRepository extends JpaRepository<ComentarioPublicacion, UUID> {
    List<ComentarioPublicacion> findByPublicacionIdOrderByCreadoEnAsc(UUID publicacionId);
    long countByPublicacionId(UUID publicacionId);
}
