package com.artagram.feed.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioProyeccionRepository extends JpaRepository<UsuarioProyeccion, UUID> {
    Optional<UsuarioProyeccion> findByNombreUsuarioIgnoreCase(String nombreUsuario);
}
