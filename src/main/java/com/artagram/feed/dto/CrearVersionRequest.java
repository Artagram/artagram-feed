package com.artagram.feed.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearVersionRequest(
        @NotBlank(message = "La URL de la imagen de proceso es obligatoria")
        String imagenUrl,
        String notaCambio
) {
}
