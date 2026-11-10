package com.artagram.feed.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Llamada directa y sincrona a Groq para las sugerencias al publicar. El analisis
 * completo de una obra sigue yendo por cola a artagram-ai-worker; esto en cambio
 * tiene que responder mientras la persona llena el formulario.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);
    private static final URI URL = URI.create("https://api.groq.com/openai/v1/chat/completions");

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient http;

    public GroqClient(@Value("${artagram.ia.groq-api-key:}") String apiKey,
                      @Value("${artagram.ia.groq-model:qwen/qwen3.8-27b}") String model,
                      ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** Contenido de la respuesta del modelo, o vacio si no hay clave o Groq falla. */
    public Optional<String> completarJsonConImagen(String sistema, String instruccion, String imagenDataUri,
                                                   int maxTokens, double temperatura) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("GROQ_API_KEY no configurada; las sugerencias usan la respuesta de respaldo");
            return Optional.empty();
        }
        try {
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("model", model);
            cuerpo.put("temperature", temperatura);
            cuerpo.put("max_tokens", maxTokens);
            cuerpo.put("response_format", Map.of("type", "json_object"));
            cuerpo.put("messages", List.of(
                    Map.of("role", "system", "content", sistema),
                    Map.of("role", "user", "content", List.of(
                            Map.of("type", "text", "text", instruccion),
                            Map.of("type", "image_url", "image_url", Map.of("url", imagenDataUri))))));

            HttpRequest peticion = HttpRequest.newBuilder(URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cuerpo)))
                    .build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 400) {
                String detalle = respuesta.body() == null ? "" : respuesta.body();
                log.warn("Groq respondio {} al sugerir metadatos: {}", respuesta.statusCode(),
                        detalle.substring(0, Math.min(detalle.length(), 300)));
                return Optional.empty();
            }
            String contenido = objectMapper.readTree(respuesta.body())
                    .path("choices").path(0).path("message").path("content").asText("");
            return contenido.isBlank() ? Optional.empty() : Optional.of(contenido);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Consulta a Groq interrumpida");
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("No se pudo consultar Groq: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
