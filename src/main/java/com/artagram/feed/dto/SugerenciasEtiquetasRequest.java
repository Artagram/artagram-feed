package com.artagram.feed.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record SugerenciasEtiquetasRequest(
        @NotBlank(message = "La imagen es obligatoria para generar sugerencias")
        String imagenBase64,
        String tituloBorrador,
        String descripcionBorrador
) {
}
