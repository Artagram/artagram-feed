package com.artagram.feed.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class S3StorageProviderErroresTest {

    @Test
    void sinS3DisponibleLasOperacionesFallanDeFormaControlada() {
        try (ServidorQueRechaza s3Caido = new ServidorQueRechaza();
             S3StorageProvider s3 = new S3StorageProvider(s3Caido.url(), "http://cdn.local", "us-east-1",
                "bucket", "clave", "secreto", true)) {
            assertThat(s3.existe("a.png")).isFalse();
            assertThatCode(() -> s3.eliminar("/a.png")).doesNotThrowAnyException();
            assertThatThrownBy(() -> s3.guardar("a.png", new ByteArrayInputStream(new byte[]{1}), 1, "image/png"))
                    .isInstanceOf(Exception.class);
            assertThatThrownBy(() -> s3.guardar("b.png", new ByteArrayInputStream(new byte[]{1}), 1, null))
                    .isInstanceOf(Exception.class);
            assertThatThrownBy(() -> s3.leer("a.png")).isInstanceOf(Exception.class);
            assertThat(s3.urlPrefirmadaSubida("a.png", "image/png", Duration.ofMinutes(1))).isPresent();
            assertThat(s3.urlPrefirmadaSubida("a.png", null, Duration.ofMinutes(1))).isPresent();
            assertThat(s3.urlPrefirmadaLectura("a.png", Duration.ofMinutes(1))).isPresent();
        }
    }
}
