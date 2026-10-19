package com.artagram.feed.controller;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.dto.CrearVersionRequest;
import com.artagram.feed.dto.VersionPublicacionDto;
import com.artagram.feed.service.VersionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicaciones/{publicacionId}/versiones")
public class VersionController {

    private final VersionService versionService;
    private final JwtUserResolver userResolver;

    public VersionController(VersionService versionService, JwtUserResolver userResolver) {
        this.versionService = versionService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<List<VersionPublicacionDto>> listar(@PathVariable UUID publicacionId) {
        return ResponseEntity.ok(versionService.listarPorPublicacion(publicacionId));
    }

    @PostMapping
    public ResponseEntity<VersionPublicacionDto> crear(@PathVariable UUID publicacionId,
                                                      @Valid @RequestBody CrearVersionRequest req,
                                                      HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        boolean esAdmin = usuario.roles() != null && usuario.roles().contains("ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(versionService.crear(publicacionId, usuario.id(), esAdmin, req));
    }
}
