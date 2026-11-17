package com.artagram.feed.ia;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Con dos constructores, Spring debe saber cuál usar al crear el bean. */
class GroqClientContextoTest {

    @Test
    void springCreaElBeanConElConstructorPublico() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(ObjectMapper.class, () -> new ObjectMapper());
            ctx.register(GroqClient.class);
            ctx.refresh();
            assertThat(ctx.getBean(GroqClient.class).completarJsonConImagen("s", "i", "d", 1, 0)).isEmpty();
        }
    }
}
