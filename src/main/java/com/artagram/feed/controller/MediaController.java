package com.artagram.feed.controller;

import com.artagram.feed.common.JwtUserResolver;
import com.artagram.feed.dto.PresignRequest;
import com.artagram.feed.dto.PresignResponse;
import com.artagram.feed.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MediaController {

    private final StorageService storageService;
    private final JwtUserResolver userResolver;

    public MediaController(StorageService storageService, JwtUserResolver userResolver) {
        this.storageService = storageService;
        this.userResolver = userResolver;
    }

    @PostMapping("/api/media/presign")
    public ResponseEntity<PresignResponse> presign(@Valid @RequestBody PresignRequest req, HttpServletRequest request) {
        userResolver.resolverRequerido(request);
        return ResponseEntity.ok(storageService.generarUrlSubida(req.filename(), req.contentType()));
    }

    @GetMapping("/media/**")
    public ResponseEntity<Resource> servirMedia(HttpServletRequest request) {
        String path = request.getRequestURI().substring("/media/".length());
        Resource recurso = storageService.leer(path);

        String contentType = MediaType.IMAGE_JPEG_VALUE;
        if (path.endsWith(".png")) contentType = MediaType.IMAGE_PNG_VALUE;
        else if (path.endsWith(".webp")) contentType = "image/webp";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(recurso);
    }
}
