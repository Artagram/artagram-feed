package com.artagram.feed.service;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.EstadoReporte;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.ReportePublicacion;
import com.artagram.feed.domain.ReportePublicacionRepository;
import com.artagram.feed.dto.ReportarRequest;
import com.artagram.feed.dto.ReporteAgrupadoDto;
import com.artagram.feed.dto.ResolverReporteRequest;
import com.artagram.feed.events.FeedEventPublisher;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModeracionService {

    private static final String OCULTADA = "OCULTADA";
    private static final String RESTAURADA = "RESTAURADA";

    private final ReportePublicacionRepository reporteRepo;
    private final PublicacionRepository publicacionRepo;
    private final PublicacionService publicacionService;
    private final FeedEventPublisher eventPublisher;
    private final int reportesParaOcultar;

    public ModeracionService(ReportePublicacionRepository reporteRepo,
                             PublicacionRepository publicacionRepo,
                             PublicacionService publicacionService,
                             FeedEventPublisher eventPublisher,
                             @Value("${artagram.moderacion.reportes-para-ocultar:3}") int reportesParaOcultar) {
        this.reporteRepo = reporteRepo;
        this.publicacionRepo = publicacionRepo;
        this.publicacionService = publicacionService;
        this.eventPublisher = eventPublisher;
        this.reportesParaOcultar = reportesParaOcultar;
    }

    /**
     * El bloqueo de la fila serializa los reportes de una obra: sin el, dos
     * reportes simultaneos podrian contar 2 cada uno y ninguno la ocultaria.
     * Solo cuentan los pendientes, asi una obra restaurada no se vuelve a
     * ocultar por los reportes que el administrador ya descarto.
     */
    @Transactional
    public void reportar(UUID publicacionId, UUID reportanteId, ReportarRequest req) {
        Publicacion p = publicacionRepo.buscarParaActualizar(publicacionId)
                .filter(pub -> !pub.isOculta())
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));
        if (p.getAutor().getId().equals(reportanteId)) {
            throw ApiException.badRequest("REPORTE_PROPIO", "No puedes reportar tu propia obra");
        }
        if (reporteRepo.existsByPublicacionIdAndReportanteId(publicacionId, reportanteId)) {
            throw ApiException.conflict("YA_REPORTADA", "Ya reportaste esta obra");
        }
        reporteRepo.save(new ReportePublicacion(p, reportanteId, req.motivo(), req.detalle()));

        if (reporteRepo.countByPublicacionIdAndEstado(publicacionId, EstadoReporte.PENDIENTE) >= reportesParaOcultar) {
            p.setOculta(true);
            eventPublisher.publicarPostModerado(p.getId(), p.getAutor().getId(), p.getTitulo(), OCULTADA);
        }
    }

    @Transactional(readOnly = true)
    public List<ReporteAgrupadoDto> listarPendientes() {
        Map<UUID, List<ReportePublicacion>> porObra = new LinkedHashMap<>();
        for (ReportePublicacion r : reporteRepo.findByEstadoOrderByCreadoEnDesc(EstadoReporte.PENDIENTE)) {
            porObra.computeIfAbsent(r.getPublicacion().getId(), id -> new ArrayList<>()).add(r);
        }
        return porObra.entrySet().stream().map(e -> agrupar(e.getKey(), e.getValue())).toList();
    }

    private ReporteAgrupadoDto agrupar(UUID publicacionId, List<ReportePublicacion> reportes) {
        Publicacion p = reportes.get(0).getPublicacion();
        Map<String, Integer> motivos = new TreeMap<>();
        reportes.forEach(r -> motivos.merge(r.getMotivo().name(), 1, Integer::sum));
        List<String> detalles = reportes.stream()
                .map(ReportePublicacion::getDetalle)
                .filter(Objects::nonNull)
                .filter(d -> !d.isBlank())
                .toList();
        return new ReporteAgrupadoDto(publicacionId, p.getTitulo(), publicacionService.urlImagen(p),
                p.getAutor().getNombreArtista(), p.isOculta(), reportes.size(), motivos, detalles,
                reportes.get(0).getCreadoEn().toString());
    }

    @Transactional
    public void resolver(UUID publicacionId, ResolverReporteRequest.Accion accion) {
        Publicacion p = publicacionRepo.buscarParaActualizar(publicacionId)
                .orElseThrow(() -> ApiException.notFound("PUBLICACION_NO_ENCONTRADA", "La publicación no existe"));
        boolean ocultar = accion == ResolverReporteRequest.Accion.OCULTAR;
        reporteRepo.resolverPendientes(publicacionId, ocultar ? EstadoReporte.ACEPTADO : EstadoReporte.DESCARTADO);
        if (p.isOculta() != ocultar) {
            p.setOculta(ocultar);
            eventPublisher.publicarPostModerado(p.getId(), p.getAutor().getId(), p.getTitulo(), ocultar ? OCULTADA : RESTAURADA);
        }
    }

    /** Pone en cola las obras publicadas antes de que existiera la marca de agua. */
    @Transactional(readOnly = true)
    public int procesarObrasSinMarca() {
        List<Publicacion> pendientes = publicacionRepo.findByMarcaAguaRutaIsNull();
        pendientes.forEach(p -> eventPublisher.publicarPostPublicado(
                p.getId(), p.getAutor().getId(), p.getAutor().getNombreArtista(), p.getTitulo(), p.getImagenUrl()));
        return pendientes.size();
    }
}
