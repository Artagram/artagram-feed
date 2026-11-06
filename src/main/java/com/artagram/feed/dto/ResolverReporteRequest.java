package com.artagram.feed.dto;

import jakarta.validation.constraints.NotNull;

public record ResolverReporteRequest(@NotNull(message = "La acción es obligatoria") Accion accion) {

    public enum Accion {
        OCULTAR,
        RESTAURAR
    }
}
