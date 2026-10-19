package com.artagram.feed.dto;

public record LikeResponse(
        boolean meGusta,
        int contadorLikes
) {
}
