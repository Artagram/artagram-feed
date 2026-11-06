package com.artagram.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.UsoIaRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlanServiceTest {

    private static final long MB = 1024L * 1024L;

    private final UUID usuarioId = UUID.randomUUID();
    private PublicacionRepository publicaciones;
    private UsoIaRepository usoIa;
    private PlanService service;

    @BeforeEach
    void setUp() {
        publicaciones = mock(PublicacionRepository.class);
        usoIa = mock(UsoIaRepository.class);
        service = new PlanService(new PlanProperties(), publicaciones, usoIa);
    }

    private void esLimiteDePlan(Runnable accion) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(e.getCodigo()).isEqualTo("PLAN_LIMITE");
        });
    }

    @Test
    void gratisSeDetieneEnCincuentaObras() {
        when(publicaciones.countByAutorId(usuarioId)).thenReturn(49L);
        assertThatCode(() -> service.verificarCupoPublicaciones(usuarioId, false)).doesNotThrowAnyException();

        when(publicaciones.countByAutorId(usuarioId)).thenReturn(50L);
        esLimiteDePlan(() -> service.verificarCupoPublicaciones(usuarioId, false));
    }

    @Test
    void proLlegaHastaQuinientasObrasSinOfrecerMejora() {
        when(publicaciones.countByAutorId(usuarioId)).thenReturn(499L);
        assertThatCode(() -> service.verificarCupoPublicaciones(usuarioId, true)).doesNotThrowAnyException();

        when(publicaciones.countByAutorId(usuarioId)).thenReturn(500L);
        assertThatThrownBy(() -> service.verificarCupoPublicaciones(usuarioId, true))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("TOPE_PUBLICACIONES"));
    }

    @Test
    void tamanoGratisHastaDiezMegas() {
        assertThatCode(() -> service.verificarTamanoImagen(10 * MB, false)).doesNotThrowAnyException();
        esLimiteDePlan(() -> service.verificarTamanoImagen(10 * MB + 1, false));
    }

    @Test
    void tamanoProHastaVeinticincoMegas() {
        assertThatCode(() -> service.verificarTamanoImagen(25 * MB, true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.verificarTamanoImagen(25 * MB + 1, true))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("IMAGEN_MUY_GRANDE"));
    }

    @Test
    void iaGratisConsumeCupoYSeBloqueaAlAgotarlo() {
        when(usoIa.consumir(eq(usuarioId), anyString(), eq(5))).thenReturn(true, false);

        assertThatCode(() -> service.consumirIa(usuarioId, false)).doesNotThrowAnyException();
        esLimiteDePlan(() -> service.consumirIa(usuarioId, false));
    }

    @Test
    void iaProNoConsumeCupo() {
        service.consumirIa(usuarioId, true);

        verifyNoInteractions(usoIa);
    }
}
