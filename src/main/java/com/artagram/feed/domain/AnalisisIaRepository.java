package com.artagram.feed.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalisisIaRepository extends JpaRepository<AnalisisIa, UUID> {
    Optional<AnalisisIa> findTopByPublicacionIdOrderByCreadoEnDesc(UUID publicacionId);
}
