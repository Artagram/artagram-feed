package com.artagram.feed.dto;

import java.util.UUID;

public record VersionPublicacionDto(
        UUID id,
        UUID publicacionId,
        int numeroVersion,
        String imagenUrl,
        String notaCambio,
        String creadoEn
) {
}
