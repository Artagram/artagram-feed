package com.artagram.feed.service;

import com.artagram.feed.common.ApiException;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.UsoIaRepository;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Limites del plan gratis y del plan PRO. Se decide con el claim premium del JWT. */
@Service
public class PlanService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final String CODIGO = "PLAN_LIMITE";

    private final PlanProperties limites;
    private final PublicacionRepository publicacionRepo;
    private final UsoIaRepository usoIaRepo;

    public PlanService(PlanProperties limites, PublicacionRepository publicacionRepo, UsoIaRepository usoIaRepo) {
        this.limites = limites;
        this.publicacionRepo = publicacionRepo;
        this.usoIaRepo = usoIaRepo;
    }

    public void verificarCupoPublicaciones(UUID autorId, boolean premium) {
        int limite = premium ? limites.getPublicacionesPro() : limites.getPublicacionesGratis();
        if (publicacionRepo.countByAutorId(autorId) < limite) {
            return;
        }
        if (premium) {
            throw ApiException.forbidden("TOPE_PUBLICACIONES", "Llegaste al limite de " + limite + " obras publicadas");
        }
        throw ApiException.forbidden(CODIGO, "Llegaste al limite de " + limite
                + " obras del plan gratis. Pasate a PRO para publicar hasta " + limites.getPublicacionesPro());
    }

    public void verificarCupoDestacadas(UUID autorId, boolean premium) {
        int limite = premium ? limites.getDestacadasPro() : limites.getDestacadasGratis();
        if (publicacionRepo.countByAutorIdAndDestacadaTrue(autorId) < limite) {
            return;
        }
        if (premium) {
            throw ApiException.forbidden("TOPE_DESTACADAS", "Puedes destacar hasta " + limite + " obras. Quita una para destacar otra");
        }
        throw ApiException.forbidden(CODIGO, "El plan gratis permite destacar " + limite
                + " obras. Quita una o pasate a PRO para destacar hasta " + limites.getDestacadasPro());
    }

    public void verificarTamanoImagen(long bytes, boolean premium) {
        int limiteMb = premium ? limites.getImagenMbPro() : limites.getImagenMbGratis();
        if (bytes <= limiteMb * 1024L * 1024L) {
            return;
        }
        if (premium) {
            throw ApiException.badRequest("IMAGEN_MUY_GRANDE", "Cada imagen puede pesar hasta " + limiteMb + " MB");
        }
        throw ApiException.forbidden(CODIGO, "El plan gratis admite imagenes de hasta " + limiteMb
                + " MB. Pasate a PRO para subir hasta " + limites.getImagenMbPro() + " MB");
    }

    public void consumirIa(UUID usuarioId, boolean premium) {
        if (premium) {
            return;
        }
        int limite = limites.getIaGratisPorMes();
        if (!usoIaRepo.consumir(usuarioId, YearMonth.now(ZONA).toString(), limite)) {
            throw ApiException.forbidden(CODIGO, "Ya usaste los " + limite
                    + " analisis con IA de este mes. Pasate a PRO para usarlos sin limite");
        }
    }
}
