package com.artagram.feed.storage;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.dto.PresignResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class StorageService {

    private final StorageProvider provider;
    private final String publicPrefix;

    public StorageService(StorageProvider provider,
                          @Value("${artagram.storage.endpoint:http://localhost:9000}") String s3Endpoint,
                          @Value("${artagram.storage.bucket:artagram-uploads}") String bucket,
                          @Value("${artagram.storage.type:s3}") String storageType) {
        this.provider = provider;
        if ("s3".equalsIgnoreCase(storageType)) {
            this.publicPrefix = s3Endpoint + "/" + bucket + "/";
        } else {
            this.publicPrefix = "/media/";
        }
    }

    public String guardar(String categoria, String nombreArchivo, byte[] contenido, String contentType) {
        String ext = "";
        int dot = nombreArchivo.lastIndexOf('.');
        if (dot >= 0) {
            ext = nombreArchivo.substring(dot);
        }
        String ruta = categoria + "/" + UUID.randomUUID() + ext;
        try {
            provider.guardar(ruta, new ByteArrayInputStream(contenido), contenido.length, contentType);
            return ruta;
        } catch (IOException ex) {
            throw ApiException.badRequest("ERROR_STORAGE", "No se pudo guardar el archivo: " + ex.getMessage());
        }
    }

    public Resource leer(String ruta) {
        try {
            return provider.leer(ruta);
        } catch (IOException ex) {
            throw ApiException.notFound("ARCHIVO_NO_ENCONTRADO", "Archivo no encontrado: " + ruta);
        }
    }

    public void eliminar(String ruta) {
        try {
            provider.eliminar(ruta);
        } catch (Exception ignored) {
        }
    }

    public PresignResponse generarUrlSubida(String filename, String contentType) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            ext = filename.substring(dot);
        }
        String key = "publicaciones/" + UUID.randomUUID() + ext;
        String uploadUrl = provider.urlPrefirmadaSubida(key, contentType, Duration.ofMinutes(15))
                .orElse(publicPrefix + key);
        String publicUrl = publicPrefix + key;
        return new PresignResponse(uploadUrl, publicUrl, key);
    }

    public String resolverUrlPublica(String ruta) {
        if (ruta == null || ruta.isBlank()) return null;
        if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
            return ruta;
        }
        return publicPrefix + ruta;
    }
}
