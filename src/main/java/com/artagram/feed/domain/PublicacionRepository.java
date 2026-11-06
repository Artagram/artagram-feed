package com.artagram.feed.domain;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    Page<Publicacion> findByOcultaFalseOrderByCreadoEnDesc(Pageable pageable);

    Page<Publicacion> findByOcultaFalseOrderByContadorLikesDescCreadoEnDesc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Publicacion p WHERE p.id = :id")
    Optional<Publicacion> buscarParaActualizar(@Param("id") UUID id);

    List<Publicacion> findByMarcaAguaRutaIsNull();

    @Query("SELECT DISTINCT p FROM Publicacion p JOIN p.etiquetas e WHERE LOWER(e) = LOWER(:etiqueta) AND p.oculta = false ORDER BY p.creadoEn DESC")
    Page<Publicacion> findByEtiqueta(@Param("etiqueta") String etiqueta, Pageable pageable);

    long countByAutorId(UUID autorId);

    long countByAutorIdAndDestacadaTrue(UUID autorId);

    List<Publicacion> findByAutorIdAndDestacadaTrueOrderByDestacadaEnDesc(UUID autorId);

    List<Publicacion> findByAutorIdAndDestacadaTrueAndOcultaFalseOrderByDestacadaEnDesc(UUID autorId);

    Page<Publicacion> findByAutorIdOrderByCreadoEnDesc(UUID autorId, Pageable pageable);

    Page<Publicacion> findByAutorIdAndOcultaFalseOrderByCreadoEnDesc(UUID autorId, Pageable pageable);

    Page<Publicacion> findByAutorIdInAndOcultaFalseOrderByCreadoEnDesc(Collection<UUID> autoresIds, Pageable pageable);

    @Query("SELECT p FROM Publicacion p WHERE p.oculta = false AND (LOWER(p.titulo) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY p.creadoEn DESC")
    Page<Publicacion> buscarPorTexto(@Param("q") String query, Pageable pageable);
}
