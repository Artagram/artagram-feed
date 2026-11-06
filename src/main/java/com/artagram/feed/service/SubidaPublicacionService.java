package com.artagram.feed.service;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.dto.CrearPublicacionRequest;
import com.artagram.feed.dto.PublicacionDto;
import com.artagram.feed.storage.StorageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Recibe la publicacion con sus archivos. Va aparte de PublicacionService para
 * que la subida a S3 no retenga una conexion de base de datos.
 */
@Service
public class SubidaPublicacionService {

    private static final int MAX_TITULO = 150;
    private static final int MAX_TECNICA = 50;
    private static final int MAX_IMAGENES_PROCESO = 10;
    private static final Map<String, String> EXTENSIONES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final PublicacionService publicacionService;
    private final PlanService planService;
    private final StorageService storageService;

    public SubidaPublicacionService(PublicacionService publicacionService, PlanService planService, StorageService storageService) {
        this.publicacionService = publicacionService;
        this.planService = planService;
        this.storageService = storageService;
    }

    public PublicacionDto publicar(UUID autorId, boolean premium, String titulo, String descripcion, String tecnica,
                                   List<String> etiquetas, MultipartFile obraFinal, List<MultipartFile> imagenesProceso) {
        List<MultipartFile> proceso = imagenesProceso == null
                ? List.of()
                : imagenesProceso.stream().filter(f -> !f.isEmpty()).toList();
        validar(titulo, tecnica, obraFinal, proceso);

        planService.verificarCupoPublicaciones(autorId, premium);
        verificarArchivo(obraFinal, premium);
        proceso.forEach(f -> verificarArchivo(f, premium));

        String ruta = guardar(obraFinal);
        List<String> rutasProceso = new ArrayList<>();
        proceso.forEach(f -> rutasProceso.add(guardar(f)));

        return publicacionService.crear(autorId, new CrearPublicacionRequest(
                titulo.trim(), descripcion, tecnica, ruta, rutasProceso, etiquetas));
    }

    private void validar(String titulo, String tecnica, MultipartFile obraFinal, List<MultipartFile> proceso) {
        if (titulo == null || titulo.isBlank()) {
            throw ApiException.badRequest("DATOS_INVALIDOS", "El título es obligatorio");
        }
        if (titulo.trim().length() > MAX_TITULO) {
            throw ApiException.badRequest("DATOS_INVALIDOS", "El título admite hasta " + MAX_TITULO + " caracteres");
        }
        if (tecnica != null && tecnica.length() > MAX_TECNICA) {
            throw ApiException.badRequest("DATOS_INVALIDOS", "La técnica admite hasta " + MAX_TECNICA + " caracteres");
        }
        if (obraFinal == null || obraFinal.isEmpty()) {
            throw ApiException.badRequest("DATOS_INVALIDOS", "La imagen es obligatoria");
        }
        if (proceso.size() > MAX_IMAGENES_PROCESO) {
            throw ApiException.badRequest("DATOS_INVALIDOS", "Puedes subir hasta " + MAX_IMAGENES_PROCESO + " imágenes del proceso");
        }
    }

    private void verificarArchivo(MultipartFile archivo, boolean premium) {
        if (!EXTENSIONES.containsKey(archivo.getContentType())) {
            throw ApiException.badRequest("FORMATO_NO_SOPORTADO", "Solo se admiten imágenes JPG, PNG, WEBP o GIF");
        }
        planService.verificarTamanoImagen(archivo.getSize(), premium);
    }

    private String guardar(MultipartFile archivo) {
        String tipo = archivo.getContentType();
        try {
            return storageService.guardar("publicaciones", "obra" + EXTENSIONES.get(tipo), archivo.getBytes(), tipo);
        } catch (IOException ex) {
            throw ApiException.badRequest("ERROR_STORAGE", "No se pudo leer el archivo");
        }
    }
}
