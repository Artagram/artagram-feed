package com.artagram.feed.dto;

import java.util.List;

public record SugerenciasEtiquetasResponse(
        String tituloSugerido,
        String descripcionSugerida,
        List<String> etiquetas
) {
}
