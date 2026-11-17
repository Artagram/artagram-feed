package com.artagram.feed.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class GroqClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> respuesta(int status, String cuerpo) {
        HttpResponse<String> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(status);
        when(r.body()).thenReturn(cuerpo);
        return r;
    }

    private Object llamar(GroqClient client) {
        return client.completarJsonConImagen("sistema", "instruccion", "data:image/png;base64,AAAA", 200, 0.2);
    }

    @Test
    void sinClaveNoLlamaAGroq() {
        HttpClient http = mock(HttpClient.class);
        assertThat(new GroqClient("", "m", mapper, http).completarJsonConImagen("s", "i", "d", 1, 0)).isEmpty();
        assertThat(new GroqClient(null, "m", mapper, http).completarJsonConImagen("s", "i", "d", 1, 0)).isEmpty();
        verifyNoInteractions(http);
        assertThat(new GroqClient("", "m", mapper).completarJsonConImagen("s", "i", "d", 1, 0)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void devuelveElContenidoDelModelo() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> ok = respuesta(200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"etiquetas\\\":[]}\"}}]}");
        HttpResponse<String> vacio = respuesta(200, "{\"choices\":[]}");
        HttpResponse<String> error = respuesta(429, "x".repeat(400));
        HttpResponse<String> errorSinCuerpo = respuesta(500, null);
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(ok, vacio, error, errorSinCuerpo);
        GroqClient client = new GroqClient("clave", "m", mapper, http);

        assertThat(client.completarJsonConImagen("s", "i", "d", 10, 0.1)).contains("{\"etiquetas\":[]}");
        assertThat(client.completarJsonConImagen("s", "i", "d", 10, 0.1)).isEmpty();
        assertThat(client.completarJsonConImagen("s", "i", "d", 10, 0.1)).isEmpty();
        assertThat(client.completarJsonConImagen("s", "i", "d", 10, 0.1)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void erroresDeRedEInterrupcionesDevuelvenVacio() throws Exception {
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("sin red"))
                .thenThrow(new InterruptedException("cortado"));
        GroqClient client = new GroqClient("clave", "m", mapper, http);

        assertThat(llamar(client)).isEqualTo(java.util.Optional.empty());
        assertThat(llamar(client)).isEqualTo(java.util.Optional.empty());
        assertThat(Thread.interrupted()).isTrue();
    }
}
