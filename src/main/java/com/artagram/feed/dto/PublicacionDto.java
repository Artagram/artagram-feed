package com.artagram.feed.dto;

import java.util.List;
import java.util.UUID;

public record PublicacionDto(
        UUID id,
        UUID autorId,
        String autorNombre,
        String titulo,
        String descripcion,
        String obraFinalUrl,
        List<String> imagenesProcesoUrls,
        List<String> etiquetas,
        int contadorLikes,
        boolean meGusta,
        String creadoEn,
        int contadorComentarios,
        int version,
        String editadoEn
) {
}
