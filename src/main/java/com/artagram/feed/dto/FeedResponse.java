package com.artagram.feed.dto;

import java.util.List;

public record FeedResponse(
        List<PublicacionDto> publicaciones,
        String cursorFechaSiguiente,
        String cursorIdSiguiente,
        boolean hayMas,
        List<String> artistasSugeridos
) {
}
