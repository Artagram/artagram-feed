package com.artagram.feed.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignRequest(
        @NotBlank(message = "El nombre de archivo es obligatorio")
        String filename,
        @NotBlank(message = "El tipo de contenido es obligatorio")
        String contentType
) {
}
