package com.artagram.feed.service;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.AnalisisIa;
import com.artagram.feed.domain.AnalisisIaRepository;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.dto.AnalisisIaDto;
import com.artagram.feed.dto.SugerenciasEtiquetasRequest;
import com.artagram.feed.dto.SugerenciasEtiquetasResponse;
import com.artagram.feed.events.FeedEventPublisher;
import com.artagram.feed.ia.GroqClient;
import com.artagram.feed.storage.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalisisIaService {

    private static final Logger log = LoggerFactory.getLogger(AnalisisIaService.class);

    // Mismo pedido que hacia el backend anterior a la migracion (SugerenciaEtiquetasEstrategia).
    private static final String SISTEMA_SUGERENCIAS = "Eres un curador de arte digital y redactor de portafolios artísticos.";
    private static final String INSTRUCCION_SUGERENCIAS = """
            Observa esta ilustración y genera en español un título evocador, una descripción artística corta y 4 a 6 etiquetas relevantes.
            Responde EXCLUSIVAMENTE con un JSON con esta estructura:
            {
              "tituloSugerido": "Título sugerido...",
              "descripcionSugerida": "Descripción de la obra...",
              "etiquetas": ["estilo", "tecnica", "tema"]
            }
            """;
    private static final int MAX_ETIQUETAS = 6;

    private final AnalisisIaRepository analisisRepo;
    private final PublicacionRepository publicacionRepo;
    private final FeedEventPublisher eventPublisher;
    private final PlanService planService;
    private final GroqClient groq;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    public AnalisisIaService(AnalisisIaRepository analisisRepo,
                             PublicacionRepository publicacionRepo,
                             FeedEventPublisher eventPublisher,
                             PlanService planService,
                             GroqClient groq,
                             ObjectMapper objectMapper,
                             StorageService storageService) {
        this.analisisRepo = analisisRepo;
        this.publicacionRepo = publicacionRepo;
        this.eventPublisher = eventPublisher;
        this.planService = planService;
        this.groq = groq;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    public AnalisisIaDto obtenerAnalisis(UUID publicacionId) {
        return analisisRepo.findTopByPublicacionIdOrderByCreadoEnDesc(publicacionId)
                .map(this::aDto)
                .orElseThrow(() -> ApiException.notFound("ANALISIS_NO_DISPONIBLE", "Aún no se ha realizado un análisis para esta obra"));
    }

    @Transactional
    public AnalisisIaDto solicitarAnalisis(UUID publicacionId, UUID usuarioId, boolean premium) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));
        planService.consumirIa(usuarioId, premium);

        AnalisisIa pendiente = analisisRepo.findTopByPublicacionIdOrderByCreadoEnDesc(publicacionId)
                .orElseGet(() -> new AnalisisIa(null, p, "PENDIENTE", null, null, null, null, null, null));
        pendiente.setPublicacion(p);
        pendiente.setEstado("PENDIENTE");
        pendiente.setDiagnosticoGeneral("Análisis en proceso por el motor de visión artificial...");
        pendiente = analisisRepo.save(pendiente);

        // La ruta guardada es la clave en S3; Groq necesita una URL que pueda descargar.
        eventPublisher.solicitarAnalisisIa(p.getId(), usuarioId, storageService.urlLecturaExterna(p.getImagenUrl()),
                p.getTitulo(), p.getDescripcion());

        return aDto(pendiente);
    }

    public SugerenciasEtiquetasResponse sugerirEtiquetas(SugerenciasEtiquetasRequest req, UUID usuarioId, boolean premium) {
        planService.consumirIa(usuarioId, premium);
        SugerenciasEtiquetasResponse respaldo = sugerenciaDeRespaldo(req);
        return groq.completarJsonConImagen(SISTEMA_SUGERENCIAS, INSTRUCCION_SUGERENCIAS,
                        aDataUri(req.imagenBase64()), 350, 0.4)
                .flatMap(json -> leerSugerencia(json, respaldo))
                .orElse(respaldo);
    }

    /** Lo que falte en la respuesta de la IA se completa con la sugerencia de respaldo. */
    private Optional<SugerenciasEtiquetasResponse> leerSugerencia(String contenido, SugerenciasEtiquetasResponse respaldo) {
        int inicio = contenido.indexOf('{');
        int fin = contenido.lastIndexOf('}');
        if (inicio < 0 || fin <= inicio) {
            log.warn("La IA no devolvio un JSON de sugerencias");
            return Optional.empty();
        }
        try {
            JsonNode nodo = objectMapper.readTree(contenido.substring(inicio, fin + 1));
            String titulo = nodo.path("tituloSugerido").asText("").trim();
            String descripcion = nodo.path("descripcionSugerida").asText("").trim();
            List<String> etiquetas = new ArrayList<>();
            for (JsonNode etiqueta : nodo.path("etiquetas")) {
                String limpia = etiqueta.asText("").trim().replaceFirst("^#+", "").trim();
                if (!limpia.isEmpty() && !etiquetas.contains(limpia) && etiquetas.size() < MAX_ETIQUETAS) {
                    etiquetas.add(limpia);
                }
            }
            if (titulo.isEmpty() && descripcion.isEmpty() && etiquetas.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SugerenciasEtiquetasResponse(
                    titulo.isEmpty() ? respaldo.tituloSugerido() : titulo,
                    descripcion.isEmpty() ? respaldo.descripcionSugerida() : descripcion,
                    etiquetas.isEmpty() ? respaldo.etiquetas() : etiquetas));
        } catch (Exception ex) {
            log.warn("La IA no devolvio el formato de sugerencias esperado: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** El frontend manda el base64 sin el prefijo data:, asi que el tipo se deduce de la firma. */
    static String aDataUri(String imagen) {
        if (imagen.startsWith("data:")) {
            return imagen;
        }
        String tipo = imagen.startsWith("/9j/") ? "image/jpeg"
                : imagen.startsWith("R0lGOD") ? "image/gif"
                : imagen.startsWith("UklGR") ? "image/webp"
                : "image/png";
        return "data:" + tipo + ";base64," + imagen;
    }

    /** Respuesta sin IA: se usa si Groq no esta configurado, falla o responde algo ilegible. */
    private SugerenciasEtiquetasResponse sugerenciaDeRespaldo(SugerenciasEtiquetasRequest req) {
        String base = (req.tituloBorrador() != null ? req.tituloBorrador() : "").toLowerCase();
        List<String> tags;
        if (base.contains("acuarela") || base.contains("jardin")) {
            tags = List.of("acuarela", "naturaleza", "iluminacion", "paisaje", "arte-tradicional");
        } else if (base.contains("pixel") || base.contains("8-bit") || base.contains("digital")) {
            tags = List.of("pixelart", "arte-digital", "retro", "gameart", "paleta-limitada");
        } else if (base.contains("tinta") || base.contains("estudio") || base.contains("retrato")) {
            tags = List.of("tinta-china", "retrato", "lineart", "claroscuro", "dibujo");
        } else {
            tags = List.of("arte", "ilustracion", "color", "composicion", "creatividad");
        }

        String tituloSugerido = (req.tituloBorrador() != null && !req.tituloBorrador().isBlank())
                ? req.tituloBorrador()
                : "Composición Visual #" + (int)(Math.random() * 100);

        String descSugerida = (req.descripcionBorrador() != null && !req.descripcionBorrador().isBlank())
                ? req.descripcionBorrador()
                : "Exploración de contrastes tonales, ritmo y armonía cromática.";

        return new SugerenciasEtiquetasResponse(tituloSugerido, descSugerida, tags);
    }

    private AnalisisIaDto aDto(AnalisisIa a) {
        List<String> puntosFuertes = a.getComposicion() != null
                ? Arrays.stream(a.getComposicion().split("\n")).filter(s -> !s.isBlank()).toList()
                : List.of("Buen balance tonal", "Composición equilibrada");

        List<String> areasMejora = a.getPuntosMejora() != null
                ? Arrays.stream(a.getPuntosMejora().split("\n")).filter(s -> !s.isBlank()).toList()
                : List.of("Aumentar el contraste en el punto focal");

        List<String> consejos = a.getSugerencias() != null
                ? Arrays.stream(a.getSugerencias().split("\n")).filter(s -> !s.isBlank()).toList()
                : List.of("Experimentar con veladuras adicionales");

        int score = a.getPuntuacionArmonia() != null ? a.getPuntuacionArmonia() : 88;

        return new AnalisisIaDto(
                a.getId(),
                a.getPublicacion().getId(),
                a.getEstado(),
                a.getDiagnosticoGeneral(),
                puntosFuertes,
                areasMejora,
                consejos,
                score,
                a.getPaletaDetectada(),
                a.getComposicion(),
                a.getCreadoEn().toString()
        );
    }
}
