package com.artagram.feed.dto;

import com.artagram.feed.domain.MotivoReporte;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportarRequest(
        @NotNull(message = "El motivo es obligatorio")
        MotivoReporte motivo,
        @Size(max = 500, message = "El detalle admite hasta 500 caracteres")
        String detalle
) {
}
