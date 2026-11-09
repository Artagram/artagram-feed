package com.artagram.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.AnalisisIa;
import com.artagram.feed.domain.AnalisisIaRepository;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.dto.SugerenciasEtiquetasRequest;
import com.artagram.feed.events.FeedEventPublisher;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnalisisIaServiceTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID publicacionId = UUID.randomUUID();
    private PublicacionRepository publicaciones;
    private AnalisisIaRepository analisis;
    private FeedEventPublisher eventos;
    private PlanService plan;
    private AnalisisIaService service;

    @BeforeEach
    void setUp() {
        publicaciones = mock(PublicacionRepository.class);
        analisis = mock(AnalisisIaRepository.class);
        eventos = mock(FeedEventPublisher.class);
        plan = mock(PlanService.class);
        service = new AnalisisIaService(analisis, publicaciones, eventos, plan);
    }

    @Test
    void elPrimerAnalisisDeUnaObraSeGuardaConIdentificador() {
        when(publicaciones.findById(publicacionId)).thenReturn(Optional.of(mock(Publicacion.class)));
        when(analisis.findTopByPublicacionIdOrderByCreadoEnDesc(publicacionId)).thenReturn(Optional.empty());
        when(analisis.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.solicitarAnalisis(publicacionId, usuarioId, false);

        ArgumentCaptor<AnalisisIa> guardado = ArgumentCaptor.forClass(AnalisisIa.class);
        verify(analisis).save(guardado.capture());
        assertThat(guardado.getValue().getId()).isNotNull();
    }

    @Test
    void sinCupoNoSeEncolaElAnalisis() {
        when(publicaciones.findById(publicacionId)).thenReturn(Optional.of(mock(Publicacion.class)));
        doThrow(ApiException.forbidden("PLAN_LIMITE", "sin cupo")).when(plan).consumirIa(usuarioId, false);

        assertThatThrownBy(() -> service.solicitarAnalisis(publicacionId, usuarioId, false)).isInstanceOf(ApiException.class);
        verifyNoInteractions(eventos, analisis);
    }

    @Test
    void unaObraInexistenteNoGastaCupo() {
        when(publicaciones.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitarAnalisis(publicacionId, usuarioId, false)).isInstanceOf(ApiException.class);
        verifyNoInteractions(plan);
    }

    @Test
    void lasSugerenciasCompartenElMismoCupo() {
        service.sugerirEtiquetas(new SugerenciasEtiquetasRequest("img", "acuarela", null), usuarioId, false);

        verify(plan).consumirIa(usuarioId, false);
    }
}
