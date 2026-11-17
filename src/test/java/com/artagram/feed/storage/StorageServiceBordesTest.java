package com.artagram.feed.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.artagram.feed.common.ApiException;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StorageServiceBordesTest {

    private final StorageProvider provider = mock(StorageProvider.class);
    private final StorageService disco = new StorageService(provider, "http://minio:9000", "", "bucket", "disk");

    @Test
    void guardarConYSinExtensionYFalloDelProveedor() throws Exception {
        assertThat(disco.guardar("publicaciones", "a.png", new byte[]{1}, "image/png")).startsWith("publicaciones/").endsWith(".png");
        assertThat(disco.guardar("publicaciones", "sinext", new byte[]{1}, "image/png")).doesNotContain(".");

        doThrow(new IOException("lleno")).when(provider).guardar(anyString(), any(), anyLong(), anyString());
        assertThatThrownBy(() -> disco.guardar("p", "a.png", new byte[]{1}, "image/png"))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("ERROR_STORAGE");
    }

    @Test
    void leerYEliminarToleranErrores() throws Exception {
        when(provider.leer("no")).thenThrow(new IOException("no"));
        doThrow(new IOException("x")).when(provider).eliminar("roto");

        assertThatThrownBy(() -> disco.leer("no")).isInstanceOf(ApiException.class)
                .extracting("codigo").isEqualTo("ARCHIVO_NO_ENCONTRADO");
        assertThatCode(() -> disco.eliminar("roto")).doesNotThrowAnyException();
    }

    @Test
    void urlsPrefirmadasConRespaldoPublico() {
        when(provider.urlPrefirmadaSubida(anyString(), eq("image/png"), any())).thenReturn(Optional.empty());
        when(provider.urlPrefirmadaDescarga(eq("a.png"), eq("a.jpg"), any())).thenReturn(Optional.of("https://s3/firmada"));
        when(provider.urlPrefirmadaDescarga(eq("b.png"), any(), any())).thenReturn(Optional.empty());
        when(provider.urlPrefirmadaLectura(eq("c.png"), any())).thenReturn(Optional.empty());

        var presign = disco.generarUrlSubida("foto.png", "image/png");
        assertThat(presign.uploadUrl()).isEqualTo(presign.publicUrl()).startsWith("/media/publicaciones/");
        assertThat(disco.generarUrlSubida("sinext", "image/png").publicUrl()).doesNotContain(".");
        assertThat(disco.urlDescarga("a.png", "a.jpg")).isEqualTo("https://s3/firmada");
        assertThat(disco.urlDescarga("b.png", "b.jpg")).isEqualTo("/media/b.png");
        assertThat(disco.urlLecturaExterna("/media/c.png")).isEqualTo("/media/c.png");
        assertThat(disco.urlLecturaExterna("https://ya/es.png")).isEqualTo("https://ya/es.png");
        assertThat(disco.urlLecturaExterna(" ")).isNull();
        assertThat(disco.urlLecturaExterna(null)).isNull();
        assertThat(disco.resolverUrlPublica("")).isNull();
    }
}
