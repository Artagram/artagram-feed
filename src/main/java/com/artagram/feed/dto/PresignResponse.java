package com.artagram.feed.dto;

public record PresignResponse(
        String uploadUrl,
        String publicUrl,
        String key
) {
}
