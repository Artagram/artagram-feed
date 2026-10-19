package com.artagram.feed.controller;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.dto.FeedResponse;
import com.artagram.feed.service.PublicacionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final PublicacionService publicacionService;
    private final JwtUserResolver userResolver;

    public FeedController(PublicacionService publicacionService, JwtUserResolver userResolver) {
        this.publicacionService = publicacionService;
        this.userResolver = userResolver;
    }

    @GetMapping("/siguiendo")
    public ResponseEntity<FeedResponse> siguiendo(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(publicacionService.feedSiguiendo(usuario.id(), pagina, tamano));
    }

    @GetMapping("/explorar")
    public ResponseEntity<FeedResponse> explorar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano,
            HttpServletRequest request) {
        UUID usuarioId = userResolver.resolverOpcional(request).map(JwtUserResolver.UsuarioAutenticado::id).orElse(null);
        return ResponseEntity.ok(publicacionService.feedExplorar(usuarioId, pagina, tamano));
    }
}
