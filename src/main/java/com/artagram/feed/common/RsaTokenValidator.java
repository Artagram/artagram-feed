package com.artagram.feed.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class RsaTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(RsaTokenValidator.class);

    private final ResourceLoader resourceLoader;
    private final String publicKeyPath;
    private PublicKey publicKey;

    public RsaTokenValidator(ResourceLoader resourceLoader,
                             @Value("${artagram.jwt.public-key-path}") String publicKeyPath) {
        this.resourceLoader = resourceLoader;
        this.publicKeyPath = publicKeyPath;
    }

    @PostConstruct
    public void init() {
        try {
            String pem;
            if (publicKeyPath.startsWith("classpath:")) {
                Resource resource = resourceLoader.getResource(publicKeyPath);
                try (InputStream is = resource.getInputStream()) {
                    pem = new String(is.readAllBytes());
                }
            } else {
                pem = Files.readString(Path.of(publicKeyPath));
            }
            pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.publicKey = kf.generatePublic(spec);
            log.info("RsaTokenValidator inicializado con llave pública RSA.");
        } catch (Exception ex) {
            log.warn("No se pudo cargar la llave pública RSA desde {}: {}", publicKeyPath, ex.getMessage());
        }
    }

    public Claims validarYExtraer(String token) {
        if (publicKey == null) {
            throw ApiException.unauthorized("LLAVE_NO_CONFIGURADA", "Llave pública no disponible");
        }
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
