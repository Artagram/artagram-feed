package com.artagram.feed.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportePublicacionRepository extends JpaRepository<ReportePublicacion, UUID> {

    boolean existsByPublicacionIdAndReportanteId(UUID publicacionId, UUID reportanteId);

    long countByPublicacionIdAndEstado(UUID publicacionId, EstadoReporte estado);

    List<ReportePublicacion> findByEstadoOrderByCreadoEnDesc(EstadoReporte estado);

    @Modifying
    @Query("UPDATE ReportePublicacion r SET r.estado = :estado WHERE r.publicacion.id = :publicacionId AND r.estado = com.artagram.feed.domain.EstadoReporte.PENDIENTE")
    int resolverPendientes(@Param("publicacionId") UUID publicacionId, @Param("estado") EstadoReporte estado);
}
