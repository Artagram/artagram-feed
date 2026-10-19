package com.artagram.feed.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CrearPublicacionRequest(
        @NotBlank(message = "El título es obligatorio")
        String titulo,
        String descripcion,
        String tecnica,
        @NotBlank(message = "La imagen es obligatoria")
        String imagenUrl,
        List<String> imagenesProcesoUrls,
        List<String> etiquetas
) {
}
