package com.artagram.feed.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "artagram.planes")
public class PlanProperties {

    private int iaGratisPorMes = 5;
    private int publicacionesGratis = 50;
    private int publicacionesPro = 500;
    private int imagenMbGratis = 10;
    private int imagenMbPro = 25;

    public int getIaGratisPorMes() { return iaGratisPorMes; }
    public void setIaGratisPorMes(int iaGratisPorMes) { this.iaGratisPorMes = iaGratisPorMes; }
    public int getPublicacionesGratis() { return publicacionesGratis; }
    public void setPublicacionesGratis(int publicacionesGratis) { this.publicacionesGratis = publicacionesGratis; }
    public int getPublicacionesPro() { return publicacionesPro; }
    public void setPublicacionesPro(int publicacionesPro) { this.publicacionesPro = publicacionesPro; }
    public int getImagenMbGratis() { return imagenMbGratis; }
    public void setImagenMbGratis(int imagenMbGratis) { this.imagenMbGratis = imagenMbGratis; }
    public int getImagenMbPro() { return imagenMbPro; }
    public void setImagenMbPro(int imagenMbPro) { this.imagenMbPro = imagenMbPro; }
}
