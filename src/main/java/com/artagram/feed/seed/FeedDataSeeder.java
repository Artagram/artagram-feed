package com.artagram.feed.seed;

import com.artagram.feed.domain.ComentarioPublicacion;
import com.artagram.feed.domain.ComentarioPublicacionRepository;
import com.artagram.feed.domain.LikePublicacion;
import com.artagram.feed.domain.LikePublicacionRepository;
import com.artagram.feed.domain.Publicacion;
import com.artagram.feed.domain.PublicacionRepository;
import com.artagram.feed.domain.SeguidorProyeccion;
import com.artagram.feed.domain.SeguidorProyeccionRepository;
import com.artagram.feed.domain.UsuarioProyeccion;
import com.artagram.feed.domain.UsuarioProyeccionRepository;
import com.artagram.feed.domain.VersionPublicacion;
import com.artagram.feed.domain.VersionPublicacionRepository;
import com.artagram.feed.storage.StorageService;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@Profile("seed")
public class FeedDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FeedDataSeeder.class);

    private final UsuarioProyeccionRepository usuarioRepo;
    private final SeguidorProyeccionRepository seguidorRepo;
    private final PublicacionRepository publicacionRepo;
    private final VersionPublicacionRepository versionRepo;
    private final LikePublicacionRepository likeRepo;
    private final ComentarioPublicacionRepository comentarioRepo;
    private final StorageService storageService;
    private final ResourceLoader resourceLoader;

    public FeedDataSeeder(UsuarioProyeccionRepository usuarioRepo,
                          SeguidorProyeccionRepository seguidorRepo,
                          PublicacionRepository publicacionRepo,
                          VersionPublicacionRepository versionRepo,
                          LikePublicacionRepository likeRepo,
                          ComentarioPublicacionRepository comentarioRepo,
                          StorageService storageService,
                          ResourceLoader resourceLoader) {
        this.usuarioRepo = usuarioRepo;
        this.seguidorRepo = seguidorRepo;
        this.publicacionRepo = publicacionRepo;
        this.versionRepo = versionRepo;
        this.likeRepo = likeRepo;
        this.comentarioRepo = comentarioRepo;
        this.storageService = storageService;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        log.info("Iniciando seed de datos para artagram-feed...");

        List<UsuarioProyeccion> usuarios = List.of(
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001"), "lucia_acuarela", "Lucía Mora", "avatar_0.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000002"), "sam_pixel", "Sam Rivera", "avatar_1.jpg", true),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000003"), "noa_tinta", "Noa Delgado", "avatar_2.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000004"), "ivan_urbano", "Iván Rueda", "avatar_3.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000005"), "maria_paz_art", "María Paz", "avatar_4.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000006"), "diego_grabado", "Diego León", "avatar_5.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000007"), "pincel_artagram", "Curador ArtaGram", "avatar_0.jpg", true),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0001-4000-8000-000000000001"), "camila_oleo", "Camila Oleo", "avatar_0.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0002-4000-8000-000000000002"), "mateo_3d", "Mateo 3D", "avatar_1.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0003-4000-8000-000000000003"), "valeria_botanica", "Valeria Botánica", "avatar_2.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0004-4000-8000-000000000004"), "andres_concept", "Andrés Concept", "avatar_3.jpg", false),
            new UsuarioProyeccion(UUID.fromString("a1b2c3d4-0005-4000-8000-000000000005"), "elena_collage", "Elena Collage", "avatar_4.jpg", false)
        );

        for (var u : usuarios) {
            if (!usuarioRepo.existsById(u.getId())) {
                usuarioRepo.save(u);
            }
        }

        UUID luciaId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001");
        UUID samId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000002");
        UUID noaId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000003");
        UUID ivanId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000004");
        UUID mariaId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000005");
        UUID diegoId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000006");

        if (!seguidorRepo.existsById_SeguidorIdAndId_SeguidoId(luciaId, samId)) {
            seguidorRepo.save(new SeguidorProyeccion(luciaId, samId));
        }
        if (!seguidorRepo.existsById_SeguidorIdAndId_SeguidoId(samId, luciaId)) {
            seguidorRepo.save(new SeguidorProyeccion(samId, luciaId));
        }

        seederPublicacion(
                UUID.fromString("b0000000-0000-4000-8000-000000000001"), luciaId,
                "Jardín al amanecer", "Acuarela sobre papel, 30x40cm. Tercera vez que repito esta composición.", "Acuarela",
                "jardin_abstracto.jpg", List.of("jardin_abstracto_paso1.jpg", "jardin_abstracto_paso2.jpg"),
                Set.of("acuarela", "jardines", "paisaje")
        );

        seederPublicacion(
                UUID.fromString("b0000000-0000-4000-8000-000000000002"), samId,
                "Atardecer en 8-bit", "Práctica diaria #47. Paleta limitada a 6 colores.", "Pixel Art",
                "atardecer_coral.jpg", List.of("atardecer_coral_paso1.jpg", "atardecer_coral_paso2.jpg"),
                Set.of("pixelart", "digital")
        );

        seederPublicacion(
                UUID.fromString("b0000000-0000-4000-8000-000000000003"), noaId,
                "Estudio en violeta", "Tinta china y una gota de acuarela. Retrato de estudio.", "Tinta china",
                "estudio_violeta.jpg", List.of("estudio_violeta_paso1.jpg", "estudio_violeta_paso2.jpg"),
                Set.of("tinta", "retrato")
        );

        seederPublicacion(
                UUID.fromString("b0000000-0000-4000-8000-000000000004"), ivanId,
                "Mural cálido", "Boceto para el mural comunitario del barrio.", "Técnica mixta",
                "acuarela_calida.jpg", List.of(),
                Set.of("mural", "boceto")
        );

        seederPublicacion(
                UUID.fromString("b0000000-0000-4000-8000-000000000005"), mariaId,
                "Lienzo nocturno", "Ilustración para portada de fanzine.", "Digital",
                "lienzo_nocturno.jpg", List.of(),
                Set.of("ilustracion")
        );

        seederPublicacion(
                UUID.fromString("b0000000-0000-4000-8000-000000000006"), diegoId,
                "Brotes de color", "Grabado experimental, primera prueba de color.", "Grabado",
                "brotes_de_color.jpg", List.of(),
                Set.of("grabado", "experimental")
        );

        log.info("Seed de datos para artagram-feed completado exitosamente.");
    }

    private void seederPublicacion(UUID id, UUID autorId, String titulo, String desc, String tecnica,
                                   String imagenNombre, List<String> procesoNombres, Set<String> etiquetas) {
        if (publicacionRepo.existsById(id)) {
            return;
        }

        UsuarioProyeccion autor = usuarioRepo.findById(autorId).orElseThrow();
        String rutaImagen = guardarArchivoSiExiste(imagenNombre, "publicaciones");

        Publicacion p = new Publicacion(id, autor, titulo, desc, tecnica, rutaImagen, etiquetas);
        p.setContadorLikes(1);
        p.setContadorComentarios(1);
        p = publicacionRepo.save(p);

        int num = 1;
        for (String proc : procesoNombres) {
            String rutaProc = guardarArchivoSiExiste(proc, "versiones");
            versionRepo.save(new VersionPublicacion(UUID.randomUUID(), p, num++, rutaProc, "Paso #" + (num - 1)));
        }

        UUID samId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000002");
        UsuarioProyeccion uSam = usuarioRepo.findById(samId).orElse(autor);
        if (!likeRepo.existsById_PublicacionIdAndId_UsuarioId(p.getId(), uSam.getId())) {
            likeRepo.save(new LikePublicacion(p, uSam));
        }

        comentarioRepo.save(new ComentarioPublicacion(UUID.randomUUID(), p, uSam, "¡Excelente manejo de la luz y el color!"));
    }

    private String guardarArchivoSiExiste(String nombre, String categoria) {
        try {
            Resource r = resourceLoader.getResource("classpath:seed/images/" + nombre);
            if (r.exists()) {
                try (InputStream is = r.getInputStream()) {
                    byte[] bytes = is.readAllBytes();
                    return storageService.guardar(categoria, nombre, bytes, "image/jpeg");
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo cargar imagen seed {}: {}", nombre, ex.getMessage());
        }
        return "publicaciones/" + nombre;
    }
}
