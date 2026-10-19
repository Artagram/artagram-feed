package com.artagram.feed.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record EditarPublicacionRequest(
        @NotBlank(message = "El título es obligatorio")
        String titulo,
        String descripcion,
        String tecnica,
        List<String> etiquetas
) {
}
