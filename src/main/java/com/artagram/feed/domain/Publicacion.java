package com.artagram.feed.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "publicaciones")
public class Publicacion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private UsuarioProyeccion autor;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 50)
    private String tecnica;

    @Column(name = "imagen_url", nullable = false, length = 500)
    private String imagenUrl;

    @Column(name = "miniatura_url", length = 500)
    private String miniaturaUrl;

    @Column(length = 100)
    private String blurhash;

    @Column(name = "contador_likes", nullable = false)
    private int contadorLikes = 0;

    @Column(name = "contador_comentarios", nullable = false)
    private int contadorComentarios = 0;

    @Column(name = "contador_vistas", nullable = false)
    private int contadorVistas = 0;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "etiquetas_publicacion", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Column(name = "etiqueta", length = 50)
    private Set<String> etiquetas = new HashSet<>();

    public Publicacion() {
    }

    public Publicacion(UUID id, UsuarioProyeccion autor, String titulo, String descripcion,
                       String tecnica, String imagenUrl, Set<String> etiquetas) {
        this.id = id != null ? id : UUID.randomUUID();
        this.autor = autor;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tecnica = tecnica;
        this.imagenUrl = imagenUrl;
        this.etiquetas = etiquetas != null ? new HashSet<>(etiquetas) : new HashSet<>();
        this.creadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public UsuarioProyeccion getAutor() { return autor; }
    public void setAutor(UsuarioProyeccion autor) { this.autor = autor; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTecnica() { return tecnica; }
    public void setTecnica(String tecnica) { this.tecnica = tecnica; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getMiniaturaUrl() { return miniaturaUrl; }
    public void setMiniaturaUrl(String miniaturaUrl) { this.miniaturaUrl = miniaturaUrl; }
    public String getBlurhash() { return blurhash; }
    public void setBlurhash(String blurhash) { this.blurhash = blurhash; }
    public int getContadorLikes() { return contadorLikes; }
    public void setContadorLikes(int contadorLikes) { this.contadorLikes = Math.max(0, contadorLikes); }
    public int getContadorComentarios() { return contadorComentarios; }
    public void setContadorComentarios(int contadorComentarios) { this.contadorComentarios = Math.max(0, contadorComentarios); }
    public int getContadorVistas() { return contadorVistas; }
    public void setContadorVistas(int contadorVistas) { this.contadorVistas = contadorVistas; }
    public int getVersion() { return version; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
    public Set<String> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(Set<String> etiquetas) { this.etiquetas = etiquetas; }
}
