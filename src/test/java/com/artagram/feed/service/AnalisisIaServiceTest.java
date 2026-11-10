package com.artagram.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import com.artagram.feed.dto.SugerenciasEtiquetasResponse;
import com.artagram.feed.events.FeedEventPublisher;
import com.artagram.feed.ia.GroqClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private GroqClient groq;
    private AnalisisIaService service;

    @BeforeEach
    void setUp() {
        publicaciones = mock(PublicacionRepository.class);
        analisis = mock(AnalisisIaRepository.class);
        eventos = mock(FeedEventPublisher.class);
        plan = mock(PlanService.class);
        groq = mock(GroqClient.class);
        when(groq.completarJsonConImagen(anyString(), anyString(), anyString(), anyInt(), anyDouble())).thenReturn(Optional.empty());
        service = new AnalisisIaService(analisis, publicaciones, eventos, plan, groq, new ObjectMapper());
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

    private SugerenciasEtiquetasResponse sugerirConRespuesta(String respuestaIa) {
        when(groq.completarJsonConImagen(anyString(), anyString(), anyString(), anyInt(), anyDouble()))
                .thenReturn(Optional.ofNullable(respuestaIa));
        return service.sugerirEtiquetas(new SugerenciasEtiquetasRequest("/9j/abc", "", ""), usuarioId, false);
    }

    @Test
    void usaElTituloLaDescripcionYLasEtiquetasDeLaIa() {
        SugerenciasEtiquetasResponse r = sugerirConRespuesta("""
                <think>miro la imagen</think>
                {"tituloSugerido": "Medusa nocturna", "descripcionSugerida": "Tinta azul sobre negro.",
                 "etiquetas": ["#tinta", "mar", "mar", "noche", "azul", "medusa", "oceano", "extra"]}
                """);

        assertThat(r.tituloSugerido()).isEqualTo("Medusa nocturna");
        assertThat(r.descripcionSugerida()).isEqualTo("Tinta azul sobre negro.");
        assertThat(r.etiquetas()).containsExactly("tinta", "mar", "noche", "azul", "medusa", "oceano");
    }

    @Test
    void loQueFaltaEnLaRespuestaSeCompletaConElRespaldo() {
        SugerenciasEtiquetasResponse r = sugerirConRespuesta("{\"tituloSugerido\": \"Ciudad en acuarela\"}");

        assertThat(r.tituloSugerido()).isEqualTo("Ciudad en acuarela");
        assertThat(r.descripcionSugerida()).isNotBlank();
        assertThat(r.etiquetas()).isNotEmpty();
    }

    @Test
    void sinIaOConRespuestaIlegibleUsaElRespaldo() {
        assertThat(sugerirConRespuesta(null).tituloSugerido()).startsWith("Composición Visual #");
        assertThat(sugerirConRespuesta("no es json").tituloSugerido()).startsWith("Composición Visual #");
    }

    @Test
    void mandaLaImagenComoDataUriConSuTipo() {
        service.sugerirEtiquetas(new SugerenciasEtiquetasRequest("/9j/abc", null, null), usuarioId, false);

        verify(groq).completarJsonConImagen(anyString(), anyString(), eq("data:image/jpeg;base64,/9j/abc"), anyInt(), anyDouble());
        assertThat(AnalisisIaService.aDataUri("iVBORw0")).startsWith("data:image/png;base64,");
        assertThat(AnalisisIaService.aDataUri("data:image/webp;base64,x")).isEqualTo("data:image/webp;base64,x");
    }
}
