package com.artagram.feed.domain;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    Page<Publicacion> findAllByOrderByCreadoEnDesc(Pageable pageable);

    Page<Publicacion> findAllByOrderByContadorLikesDescCreadoEnDesc(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Publicacion p JOIN p.etiquetas e WHERE LOWER(e) = LOWER(:etiqueta) ORDER BY p.creadoEn DESC")
    Page<Publicacion> findByEtiqueta(@Param("etiqueta") String etiqueta, Pageable pageable);

    Page<Publicacion> findByAutorIdOrderByCreadoEnDesc(UUID autorId, Pageable pageable);

    Page<Publicacion> findByAutorIdInOrderByCreadoEnDesc(Collection<UUID> autoresIds, Pageable pageable);

    @Query("SELECT p FROM Publicacion p WHERE LOWER(p.titulo) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY p.creadoEn DESC")
    Page<Publicacion> buscarPorTexto(@Param("q") String query, Pageable pageable);
}
