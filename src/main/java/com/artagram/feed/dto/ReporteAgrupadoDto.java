package com.artagram.feed.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReporteAgrupadoDto(
        UUID publicacionId,
        String titulo,
        String imagenUrl,
        String autorNombre,
        boolean oculta,
        int total,
        Map<String, Integer> motivos,
        List<String> detalles,
        String ultimoEn
) {
}
