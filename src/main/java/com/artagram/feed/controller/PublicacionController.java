package com.artagram.feed.controller;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.dto.DescargaResponse;
import com.artagram.feed.dto.EditarPublicacionRequest;
import com.artagram.feed.dto.LikeResponse;
import com.artagram.feed.dto.PublicacionDto;
import com.artagram.feed.service.PublicacionService;
import com.artagram.feed.service.SubidaPublicacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final SubidaPublicacionService subidaService;
    private final JwtUserResolver userResolver;

    public PublicacionController(PublicacionService publicacionService, SubidaPublicacionService subidaService,
                                 JwtUserResolver userResolver) {
        this.publicacionService = publicacionService;
        this.subidaService = subidaService;
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
        var usuario = userResolver.resolverOpcional(request);
        return ResponseEntity.ok(publicacionService.obtenerPorId(id, idDe(usuario), esAdmin(usuario)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PublicacionDto> crear(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String tecnica,
            @RequestParam(required = false) List<String> etiquetas,
            @RequestParam(required = false) MultipartFile obraFinal,
            @RequestParam(required = false) List<MultipartFile> imagenesProceso,
            HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subidaService.publicar(
                usuario.id(), usuario.premium(), titulo, descripcion, tecnica, etiquetas, obraFinal, imagenesProceso));
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

    @GetMapping("/artista/{autorId}")
    public ResponseEntity<Page<PublicacionDto>> listarPorArtista(
            @PathVariable UUID autorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "12") int tamano,
            HttpServletRequest request) {
        var usuario = userResolver.resolverOpcional(request);
        return ResponseEntity.ok(publicacionService.listarPorArtista(autorId, pagina, tamano, idDe(usuario), esAdmin(usuario)));
    }

    @GetMapping("/artista/{autorId}/destacadas")
    public ResponseEntity<List<PublicacionDto>> destacadas(@PathVariable UUID autorId, HttpServletRequest request) {
        var usuario = userResolver.resolverOpcional(request);
        return ResponseEntity.ok(publicacionService.listarDestacadas(autorId, idDe(usuario), esAdmin(usuario)));
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<DescargaResponse> descargar(@PathVariable UUID id, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(publicacionService.urlDescarga(id, usuario.id(), esAdmin(Optional.of(usuario))));
    }

    @PutMapping("/{id}/destacar")
    public ResponseEntity<PublicacionDto> destacar(@PathVariable UUID id, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(publicacionService.destacar(id, usuario.id(), usuario.premium()));
    }

    @DeleteMapping("/{id}/destacar")
    public ResponseEntity<PublicacionDto> quitarDestacada(@PathVariable UUID id, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(publicacionService.quitarDestacada(id, usuario.id()));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<LikeResponse> toggleLike(@PathVariable UUID id, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(publicacionService.toggleLike(id, usuario.id()));
    }

    private UUID idDe(Optional<JwtUserResolver.UsuarioAutenticado> usuario) {
        return usuario.map(JwtUserResolver.UsuarioAutenticado::id).orElse(null);
    }

    private boolean esAdmin(Optional<JwtUserResolver.UsuarioAutenticado> usuario) {
        return usuario.map(JwtUserResolver.UsuarioAutenticado::roles).filter(r -> r.contains("ADMIN")).isPresent();
    }
}
