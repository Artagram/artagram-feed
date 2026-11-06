package com.artagram.feed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reportes_publicacion")
public class ReportePublicacion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(name = "reportante_id", nullable = false)
    private UUID reportanteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoReporte motivo;

    @Column(length = 500)
    private String detalle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReporte estado = EstadoReporte.PENDIENTE;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected ReportePublicacion() {
    }

    public ReportePublicacion(Publicacion publicacion, UUID reportanteId, MotivoReporte motivo, String detalle) {
        this.id = UUID.randomUUID();
        this.publicacion = publicacion;
        this.reportanteId = reportanteId;
        this.motivo = motivo;
        this.detalle = detalle;
        this.creadoEn = Instant.now();
    }

    public Publicacion getPublicacion() { return publicacion; }
    public MotivoReporte getMotivo() { return motivo; }
    public String getDetalle() { return detalle; }
    public Instant getCreadoEn() { return creadoEn; }
}
