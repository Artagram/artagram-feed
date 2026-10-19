package com.artagram.feed.controller;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.dto.AnalisisIaDto;
import com.artagram.feed.dto.SugerenciasEtiquetasRequest;
import com.artagram.feed.dto.SugerenciasEtiquetasResponse;
import com.artagram.feed.service.AnalisisIaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalisisIaController {

    private final AnalisisIaService analisisService;
    private final JwtUserResolver userResolver;

    public AnalisisIaController(AnalisisIaService analisisService, JwtUserResolver userResolver) {
        this.analisisService = analisisService;
        this.userResolver = userResolver;
    }

    @GetMapping("/api/publicaciones/{id}/analisis")
    public ResponseEntity<AnalisisIaDto> obtenerAnalisis(@PathVariable UUID id) {
        return ResponseEntity.ok(analisisService.obtenerAnalisis(id));
    }

    @PostMapping("/api/publicaciones/{id}/analisis")
    public ResponseEntity<AnalisisIaDto> solicitarAnalisis(@PathVariable UUID id, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(analisisService.solicitarAnalisis(id, usuario.id()));
    }

    @PostMapping("/api/ia/sugerencias")
    public ResponseEntity<SugerenciasEtiquetasResponse> sugerirEtiquetas(@Valid @RequestBody SugerenciasEtiquetasRequest req) {
        return ResponseEntity.ok(analisisService.sugerirEtiquetas(req));
    }
}
