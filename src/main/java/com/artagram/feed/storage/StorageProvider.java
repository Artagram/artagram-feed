package com.artagram.feed.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import org.springframework.core.io.Resource;

public interface StorageProvider {

    void guardar(String ruta, InputStream contenido, long tamanoBytes, String contentType) throws IOException;

    boolean existe(String ruta);

    Resource leer(String ruta) throws IOException;

    void eliminar(String ruta) throws IOException;

    default Optional<String> urlPrefirmadaSubida(String ruta, String contentType, Duration ttl) {
        return Optional.empty();
    }

    default Optional<String> urlPrefirmadaLectura(String ruta, Duration ttl) {
        return Optional.empty();
    }
}
