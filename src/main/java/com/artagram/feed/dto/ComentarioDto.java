package com.artagram.feed.dto;

import java.util.UUID;

public record ComentarioDto(
        UUID id,
        UUID publicacionId,
        UUID autorId,
        String autorNombre,
        String contenido,
        String creadoEn
) {
}
