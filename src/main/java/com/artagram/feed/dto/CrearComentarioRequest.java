package com.artagram.feed.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearComentarioRequest(
        @NotBlank(message = "El contenido no puede estar vacío")
        String contenido
) {
}
