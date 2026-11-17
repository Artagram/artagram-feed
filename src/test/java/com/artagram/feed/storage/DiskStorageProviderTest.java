package com.artagram.feed.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.artagram.feed.common.ApiException;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiskStorageProviderTest {

    @TempDir
    Path dir;

    @Test
    void guardaLeeYEliminaDentroDelDirectorio() throws Exception {
        DiskStorageProvider disk = new DiskStorageProvider(dir.toString());

        disk.guardar("publicaciones/a.png", new ByteArrayInputStream(new byte[]{1, 2, 3}), 3, "image/png");
        assertThat(disk.existe("publicaciones/a.png")).isTrue();
        assertThat(disk.leer("publicaciones/a.png").contentLength()).isEqualTo(3);
        disk.eliminar("publicaciones/a.png");
        assertThat(disk.existe("publicaciones/a.png")).isFalse();

        assertThatThrownBy(() -> disk.existe("../fuera.png"))
                .isInstanceOf(ApiException.class);
        assertThat(disk.urlPrefirmadaSubida("a", "image/png", Duration.ofMinutes(1))).isEmpty();
        assertThat(disk.urlPrefirmadaDescarga("a", "a.png", Duration.ofMinutes(1))).isEmpty();
        assertThat(disk.urlPrefirmadaLectura("a", Duration.ofMinutes(1))).isEmpty();
    }
}
