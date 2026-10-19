package com.artagram.feed.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "usuarios_proyeccion")
public class UsuarioProyeccion {

    @Id
    private UUID id;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    @Column(name = "nombre_artista", length = 100)
    private String nombreArtista;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(nullable = false)
    private boolean premium;

    public UsuarioProyeccion() {
    }

    public UsuarioProyeccion(UUID id, String nombreUsuario, String nombreArtista, String fotoUrl, boolean premium) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.nombreArtista = nombreArtista;
        this.fotoUrl = fotoUrl;
        this.premium = premium;
    }

    public UUID getId() { return id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getNombreArtista() { return nombreArtista; }
    public void setNombreArtista(String nombreArtista) { this.nombreArtista = nombreArtista; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }
}
