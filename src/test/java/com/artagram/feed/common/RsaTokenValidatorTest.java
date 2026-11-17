package com.artagram.feed.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

class RsaTokenValidatorTest {

    private static KeyPair par;
    private static String pem;

    @TempDir
    Path dir;

    @BeforeAll
    static void llaves() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        par = gen.generateKeyPair();
        pem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder().encodeToString(par.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
    }

    private static RsaTokenValidator validador(String path, String pemInline) {
        RsaTokenValidator v = new RsaTokenValidator(new DefaultResourceLoader(), path, pemInline);
        v.init();
        return v;
    }

    private static String token(UUID id) {
        return Jwts.builder().subject(id.toString()).signWith(par.getPrivate()).compact();
    }

    @Test
    void cargaLaLlaveEnLineaDesdeArchivoOClasspath() throws Exception {
        UUID id = UUID.randomUUID();
        Path archivo = Files.writeString(dir.resolve("pub.pem"), pem);

        assertThat(validador("ignorado", pem).validarYExtraer(token(id)).getSubject()).isEqualTo(id.toString());
        assertThat(validador(archivo.toString(), "").validarYExtraer(token(id)).getSubject()).isEqualTo(id.toString());
        RsaTokenValidator desdeClasspath = validador("classpath:no-existe.pem", null);
        assertThatThrownBy(() -> desdeClasspath.validarYExtraer(token(id)))
                .isInstanceOf(ApiException.class).extracting("codigo").isEqualTo("LLAVE_NO_CONFIGURADA");
    }

    @Test
    void sinLlaveValidaRechazaLosTokens() {
        RsaTokenValidator v = validador(dir.resolve("no.pem").toString(), "");
        assertThatThrownBy(() -> v.validarYExtraer("x")).isInstanceOf(ApiException.class);
    }
}
