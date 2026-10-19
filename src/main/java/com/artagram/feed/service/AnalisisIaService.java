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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalisisIaService {

    private final AnalisisIaRepository analisisRepo;
    private final PublicacionRepository publicacionRepo;
    private final FeedEventPublisher eventPublisher;

    public AnalisisIaService(AnalisisIaRepository analisisRepo,
                             PublicacionRepository publicacionRepo,
                             FeedEventPublisher eventPublisher) {
        this.analisisRepo = analisisRepo;
        this.publicacionRepo = publicacionRepo;
        this.eventPublisher = eventPublisher;
    }

    public AnalisisIaDto obtenerAnalisis(UUID publicacionId) {
        return analisisRepo.findTopByPublicacionIdOrderByCreadoEnDesc(publicacionId)
                .map(this::aDto)
                .orElseThrow(() -> ApiException.notFound("ANALISIS_NO_DISPONIBLE", "Aún no se ha realizado un análisis para esta obra"));
    }

    @Transactional
    public AnalisisIaDto solicitarAnalisis(UUID publicacionId, UUID usuarioId) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));

        AnalisisIa pendiente = analisisRepo.findTopByPublicacionIdOrderByCreadoEnDesc(publicacionId)
                .orElse(new AnalisisIa());
        pendiente.setPublicacion(p);
        pendiente.setEstado("PENDIENTE");
        pendiente.setDiagnosticoGeneral("Análisis en proceso por el motor de visión artificial...");
        pendiente = analisisRepo.save(pendiente);

        eventPublisher.solicitarAnalisisIa(p.getId(), usuarioId, p.getImagenUrl(), p.getTitulo(), p.getDescripcion());

        return aDto(pendiente);
    }

    public SugerenciasEtiquetasResponse sugerirEtiquetas(SugerenciasEtiquetasRequest req) {
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
