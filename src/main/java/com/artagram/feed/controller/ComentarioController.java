package com.artagram.feed.controller;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.dto.ComentarioDto;
import com.artagram.feed.dto.CrearComentarioRequest;
import com.artagram.feed.service.ComentarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicaciones/{publicacionId}/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;
    private final JwtUserResolver userResolver;

    public ComentarioController(ComentarioService comentarioService, JwtUserResolver userResolver) {
        this.comentarioService = comentarioService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<List<ComentarioDto>> listar(@PathVariable UUID publicacionId) {
        return ResponseEntity.ok(comentarioService.listarPorPublicacion(publicacionId));
    }

    @PostMapping
    public ResponseEntity<ComentarioDto> crear(@PathVariable UUID publicacionId,
                                               @Valid @RequestBody CrearComentarioRequest req,
                                               HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(comentarioService.crear(publicacionId, usuario.id(), req));
    }

    @DeleteMapping("/{comentarioId}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID publicacionId,
                                         @PathVariable UUID comentarioId,
                                         HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        boolean esAdmin = usuario.roles() != null && usuario.roles().contains("ADMIN");
        comentarioService.eliminar(publicacionId, comentarioId, usuario.id(), esAdmin);
        return ResponseEntity.noContent().build();
    }
}
