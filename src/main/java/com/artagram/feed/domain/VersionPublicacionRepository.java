package com.artagram.feed.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VersionPublicacionRepository extends JpaRepository<VersionPublicacion, UUID> {
    List<VersionPublicacion> findByPublicacionIdOrderByNumeroVersionAsc(UUID publicacionId);
}
