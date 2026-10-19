package com.artagram.feed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "likes_publicacion")
public class LikePublicacion {

    @EmbeddedId
    private LikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("publicacionId")
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioProyeccion usuario;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public LikePublicacion() {
    }

    public LikePublicacion(Publicacion publicacion, UsuarioProyeccion usuario) {
        this.publicacion = publicacion;
        this.usuario = usuario;
        this.id = new LikeId(publicacion.getId(), usuario.getId());
        this.creadoEn = Instant.now();
    }

    public LikeId getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public UsuarioProyeccion getUsuario() { return usuario; }
    public Instant getCreadoEn() { return creadoEn; }

    @Embeddable
    public static class LikeId implements Serializable {
        @Column(name = "publicacion_id")
        private UUID publicacionId;

        @Column(name = "usuario_id")
        private UUID usuarioId;

        public LikeId() {}

        public LikeId(UUID publicacionId, UUID usuarioId) {
            this.publicacionId = publicacionId;
            this.usuarioId = usuarioId;
        }

        public UUID getPublicacionId() { return publicacionId; }
        public UUID getUsuarioId() { return usuarioId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LikeId likeId)) return false;
            return Objects.equals(publicacionId, likeId.publicacionId) && Objects.equals(usuarioId, likeId.usuarioId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(publicacionId, usuarioId);
        }
    }
}
