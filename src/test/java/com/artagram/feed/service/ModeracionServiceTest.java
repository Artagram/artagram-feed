package com.artagram.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.EstadoReporte;
import com.artagram.feed.domain.MotivoReporte;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.ReportePublicacion;
import com.artagram.feed.domain.ReportePublicacionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.dto.ReportarRequest;
import com.artagram.feed.dto.ResolverReporteRequest.Accion;
import com.artagram.feed.events.FeedEventPublisher;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModeracionServiceTest {

    private final UUID autorId = UUID.randomUUID();
    private final UUID reportanteId = UUID.randomUUID();
    private final ReportarRequest req = new ReportarRequest(MotivoReporte.SPAM, "repetida");

    private ReportePublicacionRepository reportes;
    private PublicacionRepository publicaciones;
    private FeedEventPublisher eventos;
    private ModeracionService service;
    private Publicacion obra;

    @BeforeEach
    void setUp() {
        reportes = mock(ReportePublicacionRepository.class);
        publicaciones = mock(PublicacionRepository.class);
        eventos = mock(FeedEventPublisher.class);
        service = new ModeracionService(reportes, publicaciones, mock(PublicacionService.class), eventos, 3);
        obra = new Publicacion(UUID.randomUUID(), new UsuarioProyeccion(autorId, "lucia", "Lucía", null, false),
                "Obra", null, null, "o.jpg", Set.of());
        when(publicaciones.buscarParaActualizar(obra.getId())).thenReturn(Optional.of(obra));
    }

    private void pendientes(long cantidad) {
        when(reportes.countByPublicacionIdAndEstado(obra.getId(), EstadoReporte.PENDIENTE)).thenReturn(cantidad);
    }

    @Test
    void dosReportesNoOcultanLaObra() {
        pendientes(2);

        service.reportar(obra.getId(), reportanteId, req);

        verify(reportes).save(any());
        assertThat(obra.isOculta()).isFalse();
    }

    @Test
    void elTercerReporteLaOcultaYAvisaAlAutor() {
        pendientes(3);

        service.reportar(obra.getId(), reportanteId, req);

        assertThat(obra.isOculta()).isTrue();
        verify(eventos).publicarPostModerado(obra.getId(), autorId, "Obra", "OCULTADA");
    }

    @Test
    void nadieReportaDosVecesLaMismaObra() {
        when(reportes.existsByPublicacionIdAndReportanteId(obra.getId(), reportanteId)).thenReturn(true);

        assertThatThrownBy(() -> service.reportar(obra.getId(), reportanteId, req))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("YA_REPORTADA"));
        verify(reportes, never()).save(any());
    }

    @Test
    void elAutorNoReportaSuPropiaObra() {
        assertThatThrownBy(() -> service.reportar(obra.getId(), autorId, req))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("REPORTE_PROPIO"));
    }

    @Test
    void unaObraYaOcultaNoSePuedeReportar() {
        obra.setOculta(true);

        assertThatThrownBy(() -> service.reportar(obra.getId(), reportanteId, req)).isInstanceOf(ApiException.class);
    }

    @Test
    void restaurarDescartaLosReportesYAvisa() {
        obra.setOculta(true);

        service.resolver(obra.getId(), Accion.RESTAURAR);

        assertThat(obra.isOculta()).isFalse();
        verify(reportes).resolverPendientes(obra.getId(), EstadoReporte.DESCARTADO);
        verify(eventos).publicarPostModerado(obra.getId(), autorId, "Obra", "RESTAURADA");
    }

    @Test
    void confirmarUnaObraYaOcultaNoAvisaDosVeces() {
        obra.setOculta(true);

        service.resolver(obra.getId(), Accion.OCULTAR);

        verify(reportes).resolverPendientes(obra.getId(), EstadoReporte.ACEPTADO);
        verify(eventos, never()).publicarPostModerado(any(), any(), anyString(), anyString());
    }

    @Test
    void agrupaLosReportesPorObra() {
        when(reportes.findByEstadoOrderByCreadoEnDesc(EstadoReporte.PENDIENTE)).thenReturn(List.of(
                new ReportePublicacion(obra, UUID.randomUUID(), MotivoReporte.SPAM, "a"),
                new ReportePublicacion(obra, UUID.randomUUID(), MotivoReporte.SPAM, " "),
                new ReportePublicacion(obra, UUID.randomUUID(), MotivoReporte.PLAGIO, null)));

        var grupos = service.listarPendientes();

        assertThat(grupos).hasSize(1);
        assertThat(grupos.get(0).total()).isEqualTo(3);
        assertThat(grupos.get(0).motivos()).isEqualTo(Map.of("PLAGIO", 1, "SPAM", 2));
        assertThat(grupos.get(0).detalles()).containsExactly("a");
    }

    @Test
    void procesaLasObrasSinMarca() {
        when(publicaciones.findByMarcaAguaRutaIsNull()).thenReturn(List.of(obra));

        assertThat(service.procesarObrasSinMarca()).isEqualTo(1);
        verify(eventos).publicarPostPublicado(obra.getId(), autorId, "Lucía", "Obra", "o.jpg");
    }
}
