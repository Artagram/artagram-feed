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
@Table(name = "analisis_ia")
public class AnalisisIa {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "diagnostico_general", columnDefinition = "TEXT")
    private String diagnosticoGeneral;

    @Column(name = "paleta_detectada", columnDefinition = "TEXT")
    private String paletaDetectada;

    @Column(columnDefinition = "TEXT")
    private String composicion;

    @Column(name = "puntos_mejora", columnDefinition = "TEXT")
    private String puntosMejora;

    @Column(columnDefinition = "TEXT")
    private String sugerencias;

    @Column(name = "puntuacion_armonia")
    private Integer puntuacionArmonia;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public AnalisisIa() {
    }

    public AnalisisIa(UUID id, Publicacion publicacion, String estado, String diagnosticoGeneral,
                      String paletaDetectada, String composicion, String puntosMejora,
                      String sugerencias, Integer puntuacionArmonia) {
        this.id = id != null ? id : UUID.randomUUID();
        this.publicacion = publicacion;
        this.estado = estado;
        this.diagnosticoGeneral = diagnosticoGeneral;
        this.paletaDetectada = paletaDetectada;
        this.composicion = composicion;
        this.puntosMejora = puntosMejora;
        this.sugerencias = sugerencias;
        this.puntuacionArmonia = puntuacionArmonia;
        this.creadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public Publicacion getPublicacion() { return publicacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDiagnosticoGeneral() { return diagnosticoGeneral; }
    public void setDiagnosticoGeneral(String diagnosticoGeneral) { this.diagnosticoGeneral = diagnosticoGeneral; }
    public String getPaletaDetectada() { return paletaDetectada; }
    public void setPaletaDetectada(String paletaDetectada) { this.paletaDetectada = paletaDetectada; }
    public String getComposicion() { return composicion; }
    public void setComposicion(String composicion) { this.composicion = composicion; }
    public String getPuntosMejora() { return puntosMejora; }
    public void setPuntosMejora(String puntosMejora) { this.puntosMejora = puntosMejora; }
    public String getSugerencias() { return sugerencias; }
    public void setSugerencias(String sugerencias) { this.sugerencias = sugerencias; }
    public Integer getPuntuacionArmonia() { return puntuacionArmonia; }
    public void setPuntuacionArmonia(Integer puntuacionArmonia) { this.puntuacionArmonia = puntuacionArmonia; }
    public Instant getCreadoEn() { return creadoEn; }
}
