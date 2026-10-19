package com.artagram.feed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comentarios_publicacion")
public class ComentarioPublicacion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private UsuarioProyeccion autor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public ComentarioPublicacion() {
    }

    public ComentarioPublicacion(UUID id, Publicacion publicacion, UsuarioProyeccion autor, String contenido) {
        this.id = id != null ? id : UUID.randomUUID();
        this.publicacion = publicacion;
        this.autor = autor;
        this.contenido = contenido;
        this.creadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public UsuarioProyeccion getAutor() { return autor; }
    public String getContenido() { return contenido; }
    public Instant getCreadoEn() { return creadoEn; }
}
