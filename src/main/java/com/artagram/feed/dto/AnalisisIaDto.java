package com.artagram.feed.dto;

import java.util.List;
import java.util.UUID;

public record AnalisisIaDto(
        UUID id,
        UUID publicacionId,
        String estado,
        String diagnosticoGeneral,
        List<String> puntosFuertes,
        List<String> areasMejora,
        List<String> consejos,
        int scoreEstimado,
        String paletaDetectada,
        String composicion,
        String creadoEn
) {
}
