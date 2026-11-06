package com.artagram.feed.common;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String traceId
) {
    public ErrorResponse(String code, String message, String traceId) {
        this(code, message, Instant.now(), traceId);
    }
}
