package com.artagram.feed.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class S3StorageProviderTest {

    @Test
    void laUrlDeDescargaFuerzaElArchivoAdjunto() {
        try (ServidorQueRechaza s3Caido = new ServidorQueRechaza();
             S3StorageProvider provider = new S3StorageProvider(s3Caido.url(), "", "us-east-1", "bucket", "clave", "secreto", true)) {
            String url = provider.urlPrefirmadaDescarga("marcadas/a.jpg", "obra-artagram.jpg", Duration.ofMinutes(5)).orElseThrow();

            assertThat(URLDecoder.decode(url, StandardCharsets.UTF_8))
                    .contains("marcadas/a.jpg")
                    .contains("response-content-disposition=attachment; filename=\"obra-artagram.jpg\"")
                    .contains("X-Amz-Signature=");
        }
    }
}
