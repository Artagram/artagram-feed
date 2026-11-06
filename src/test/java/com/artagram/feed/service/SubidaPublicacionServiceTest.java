package com.artagram.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.dto.CrearPublicacionRequest;
import com.artagram.feed.storage.StorageService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class SubidaPublicacionServiceTest {

    private final UUID autorId = UUID.randomUUID();
    private PublicacionService publicaciones;
    private PlanService plan;
    private StorageService storage;
    private SubidaPublicacionService service;

    @BeforeEach
    void setUp() {
        publicaciones = mock(PublicacionService.class);
        plan = mock(PlanService.class);
        storage = mock(StorageService.class);
        service = new SubidaPublicacionService(publicaciones, plan, storage);
    }

    private MultipartFile imagen(String tipo) {
        return new MockMultipartFile("obraFinal", "obra", tipo, new byte[]{1, 2, 3});
    }

    @Test
    void guardaLasImagenesYCreaLaPublicacion() {
        when(storage.guardar(eq("publicaciones"), anyString(), any(), anyString()))
                .thenReturn("publicaciones/final.png", "publicaciones/paso.jpg");

        service.publicar(autorId, false, " Jardines ", "desc", null, List.of("acuarela"),
                imagen("image/png"), List.of(imagen("image/jpeg")));

        ArgumentCaptor<CrearPublicacionRequest> req = ArgumentCaptor.forClass(CrearPublicacionRequest.class);
        verify(publicaciones).crear(eq(autorId), req.capture());
        assertThat(req.getValue().titulo()).isEqualTo("Jardines");
        assertThat(req.getValue().imagenUrl()).isEqualTo("publicaciones/final.png");
        assertThat(req.getValue().imagenesProcesoUrls()).containsExactly("publicaciones/paso.jpg");
    }

    @Test
    void siSeAcaboElCupoNoSubeNingunArchivo() {
        doThrow(ApiException.forbidden("PLAN_LIMITE", "tope")).when(plan).verificarCupoPublicaciones(autorId, false);

        assertThatThrownBy(() -> service.publicar(autorId, false, "Obra", null, null, null, imagen("image/png"), null))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(storage, publicaciones);
    }

    @Test
    void unaImagenDelProcesoDemasiadoGrandeTambienFrenaLaSubida() {
        MultipartFile enorme = mock(MultipartFile.class);
        when(enorme.getContentType()).thenReturn("image/png");
        when(enorme.getSize()).thenReturn(99L);
        doThrow(ApiException.forbidden("PLAN_LIMITE", "pesada")).when(plan).verificarTamanoImagen(99L, false);

        assertThatThrownBy(() -> service.publicar(autorId, false, "Obra", null, null, null, imagen("image/png"), List.of(enorme)))
                .isInstanceOf(ApiException.class);
        verify(storage, never()).guardar(any(), any(), any(), any());
    }

    @Test
    void rechazaFormatosQueNoSonImagenSegura() {
        assertThatThrownBy(() -> service.publicar(autorId, true, "Obra", null, null, null, imagen("image/svg+xml"), null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCodigo()).isEqualTo("FORMATO_NO_SOPORTADO"));
    }

    @Test
    void exigeTituloEImagen() {
        assertThatThrownBy(() -> service.publicar(autorId, true, "  ", null, null, null, imagen("image/png"), null))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.publicar(autorId, true, "Obra", null, null, null, null, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void noAdmiteMasDeDiezImagenesDeProceso() {
        List<MultipartFile> once = Collections.nCopies(11, imagen("image/png"));

        assertThatThrownBy(() -> service.publicar(autorId, true, "Obra", null, null, null, imagen("image/png"), once))
                .isInstanceOf(ApiException.class);
    }
}
