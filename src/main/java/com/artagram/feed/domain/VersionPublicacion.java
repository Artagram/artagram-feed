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
@Table(name = "versiones_publicacion")
public class VersionPublicacion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(name = "numero_version", nullable = false)
    private int numeroVersion;

    @Column(name = "imagen_url", nullable = false, length = 500)
    private String imagenUrl;

    @Column(name = "nota_cambio", columnDefinition = "TEXT")
    private String notaCambio;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public VersionPublicacion() {
    }

    public VersionPublicacion(UUID id, Publicacion publicacion, int numeroVersion, String imagenUrl, String notaCambio) {
        this.id = id != null ? id : UUID.randomUUID();
        this.publicacion = publicacion;
        this.numeroVersion = numeroVersion;
        this.imagenUrl = imagenUrl;
        this.notaCambio = notaCambio;
        this.creadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public int getNumeroVersion() { return numeroVersion; }
    public String getImagenUrl() { return imagenUrl; }
    public String getNotaCambio() { return notaCambio; }
    public Instant getCreadoEn() { return creadoEn; }
}
