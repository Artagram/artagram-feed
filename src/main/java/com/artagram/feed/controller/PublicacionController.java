package com.artagram.feed.controller;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.dto.CrearPublicacionRequest;
import com.artagram.feed.dto.EditarPublicacionRequest;
import com.artagram.feed.dto.LikeResponse;
import com.artagram.feed.dto.PublicacionDto;
import com.artagram.feed.service.PublicacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final JwtUserResolver userResolver;

    public PublicacionController(PublicacionService publicacionService, JwtUserResolver userResolver) {
        this.publicacionService = publicacionService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<Page<PublicacionDto>> listar(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "recientes") String orden,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamano,
            HttpServletRequest request) {
        UUID usuarioId = userResolver.resolverOpcional(request).map(JwtUserResolver.UsuarioAutenticado::id).orElse(null);
        return ResponseEntity.ok(publicacionService.listar(tag, orden, q, pagina, tamano, usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacionDto> obtenerPorId(@PathVariable UUID id, HttpServletRequest request) {
        UUID usuarioId = userResolver.resolverOpcional(request).map(JwtUserResolver.UsuarioAutenticado::id).orElse(null);
        return ResponseEntity.ok(publicacionService.obtenerPorId(id, usuarioId));
    }

    @PostMapping
    public ResponseEntity<PublicacionDto> crear(@Valid @RequestBody CrearPublicacionRequest req, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(publicacionService.crear(usuario.id(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicacionDto> editar(@PathVariable UUID id,
                                                @Valid @RequestBody EditarPublicacionRequest req,
                                                HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        boolean esAdmin = usuario.roles() != null && usuario.roles().contains("ADMIN");
        return ResponseEntity.ok(publicacionService.editar(id, usuario.id(), esAdmin, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        boolean esAdmin = usuario.roles() != null && usuario.roles().contains("ADMIN");
        publicacionService.eliminar(id, usuario.id(), esAdmin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<LikeResponse> toggleLike(@PathVariable UUID id, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(publicacionService.toggleLike(id, usuario.id()));
    }
}
