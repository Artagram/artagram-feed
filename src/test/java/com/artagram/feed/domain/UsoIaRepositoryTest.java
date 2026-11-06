package com.artagram.feed.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class UsoIaRepositoryTest {

    private final UUID usuarioId = UUID.randomUUID();
    private JdbcTemplate jdbc;
    private UsoIaRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:uso_ia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("DROP TABLE IF EXISTS uso_ia");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V2__uso_ia.sql")));
        repository = new UsoIaRepository(jdbc);
    }

    @Test
    void permiteHastaElLimiteYNiUnoMas() {
        for (int i = 0; i < 5; i++) {
            assertThat(repository.consumir(usuarioId, "2026-11", 5)).isTrue();
        }

        assertThat(repository.consumir(usuarioId, "2026-11", 5)).isFalse();
        assertThat(jdbc.queryForObject("SELECT cantidad FROM uso_ia", Integer.class)).isEqualTo(5);
    }

    @Test
    void cadaMesEmpiezaDeCero() {
        repository.consumir(usuarioId, "2026-11", 1);

        assertThat(repository.consumir(usuarioId, "2026-11", 1)).isFalse();
        assertThat(repository.consumir(usuarioId, "2026-12", 1)).isTrue();
    }
}
