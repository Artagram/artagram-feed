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
                          @Value("${artagram.storage.public-endpoint:}") String s3PublicEndpoint,
                          @Value("${artagram.storage.bucket:artagram-uploads}") String bucket,
                          @Value("${artagram.storage.type:s3}") String storageType) {
        this.provider = provider;
        // En AWS no se configura endpoint: las imagenes se sirven por /media/ desde este servicio.
        if ("s3".equalsIgnoreCase(storageType) && s3Endpoint != null && !s3Endpoint.isBlank()) {
            String base = s3PublicEndpoint != null && !s3PublicEndpoint.isBlank() ? s3PublicEndpoint : s3Endpoint;
            this.publicPrefix = base + "/" + bucket + "/";
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

    public String urlDescarga(String ruta, String nombreArchivo) {
        return provider.urlPrefirmadaDescarga(ruta, nombreArchivo, Duration.ofMinutes(5))
                .orElseGet(() -> resolverUrlPublica(ruta));
    }

    /**
     * URL que un servicio de fuera (Groq) puede descargar. En AWS la ruta publica es
     * relativa (/media/...), asi que se firma una URL temporal de S3.
     */
    public String urlLecturaExterna(String ruta) {
        if (ruta == null || ruta.isBlank()) return null;
        if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
            return ruta;
        }
        String clave = ruta.startsWith("/media/") ? ruta.substring("/media/".length()) : ruta;
        return provider.urlPrefirmadaLectura(clave, Duration.ofMinutes(30))
                .orElseGet(() -> resolverUrlPublica(clave));
    }

    public String resolverUrlPublica(String ruta) {
        if (ruta == null || ruta.isBlank()) return null;
        if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
            return ruta;
        }
        return publicPrefix + ruta;
    }
}
