package com.artagram.feed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "seguidores_proyeccion")
public class SeguidorProyeccion {

    @EmbeddedId
    private SeguidorId id;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public SeguidorProyeccion() {
    }

    public SeguidorProyeccion(UUID seguidorId, UUID seguidoId) {
        this.id = new SeguidorId(seguidorId, seguidoId);
        this.creadoEn = Instant.now();
    }

    public SeguidorId getId() { return id; }
    public Instant getCreadoEn() { return creadoEn; }

    @Embeddable
    public static class SeguidorId implements Serializable {
        @Column(name = "seguidor_id")
        private UUID seguidorId;

        @Column(name = "seguido_id")
        private UUID seguidoId;

        public SeguidorId() {}

        public SeguidorId(UUID seguidorId, UUID seguidoId) {
            this.seguidorId = seguidorId;
            this.seguidoId = seguidoId;
        }

        public UUID getSeguidorId() { return seguidorId; }
        public UUID getSeguidoId() { return seguidoId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SeguidorId that)) return false;
            return Objects.equals(seguidorId, that.seguidorId) && Objects.equals(seguidoId, that.seguidoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(seguidorId, seguidoId);
        }
    }
}
