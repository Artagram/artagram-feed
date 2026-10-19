package com.artagram.feed.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "artagram.storage.type", havingValue = "disk", matchIfMissing = false)
public class DiskStorageProvider implements StorageProvider {

    private final Path root;

    public DiskStorageProvider(@Value("${artagram.storage.local-dir:uploads}") String localDir) {
        this.root = Path.of(localDir).toAbsolutePath().normalize();
    }

    @Override
    public void guardar(String ruta, InputStream contenido, long tamanoBytes, String contentType) throws IOException {
        Path destino = resolver(ruta);
        Files.createDirectories(destino.getParent());
        Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public boolean existe(String ruta) {
        return Files.exists(resolver(ruta));
    }

    @Override
    public Resource leer(String ruta) {
        return new FileSystemResource(resolver(ruta));
    }

    @Override
    public void eliminar(String ruta) throws IOException {
        Files.deleteIfExists(resolver(ruta));
    }

    private Path resolver(String ruta) {
        return root.resolve(ruta).normalize();
    }
}
