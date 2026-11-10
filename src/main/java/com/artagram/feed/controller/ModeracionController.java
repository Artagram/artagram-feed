package com.artagram.feed.controller;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.dto.ReportarRequest;
import com.artagram.feed.dto.ReporteAgrupadoDto;
import com.artagram.feed.dto.ResolverReporteRequest;
import com.artagram.feed.service.ModeracionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModeracionController {

    private final ModeracionService moderacionService;
    private final JwtUserResolver userResolver;

    public ModeracionController(ModeracionService moderacionService, JwtUserResolver userResolver) {
        this.moderacionService = moderacionService;
        this.userResolver = userResolver;
    }

    @PostMapping("/api/publicaciones/{id}/reportar")
    public ResponseEntity<Void> reportar(@PathVariable UUID id, @Valid @RequestBody ReportarRequest req, HttpServletRequest request) {
        moderacionService.reportar(id, userResolver.resolverRequerido(request).id(), req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/moderacion/reportes")
    public ResponseEntity<List<ReporteAgrupadoDto>> reportes(HttpServletRequest request) {
        exigirAdmin(request);
        return ResponseEntity.ok(moderacionService.listarPendientes());
    }

    @PostMapping("/api/moderacion/publicaciones/{id}/resolver")
    public ResponseEntity<Void> resolver(@PathVariable UUID id, @Valid @RequestBody ResolverReporteRequest req, HttpServletRequest request) {
        exigirAdmin(request);
        moderacionService.resolver(id, req.accion());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/moderacion/marcas/procesar")
    public ResponseEntity<Map<String, Integer>> procesarMarcas(HttpServletRequest request) {
        exigirAdmin(request);
        return ResponseEntity.ok(Map.of("enviadas", moderacionService.procesarObrasSinMarca()));
    }

    private void exigirAdmin(HttpServletRequest request) {
        String rol = userResolver.resolverRequerido(request).roles();
        if (rol == null || !rol.contains("ADMIN")) {
            throw ApiException.forbidden("SIN_PERMISO", "Solo un administrador puede moderar");
        }
    }
}
