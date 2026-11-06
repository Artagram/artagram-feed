package com.artagram.feed.domain;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsoIaRepository {

    private final JdbcTemplate jdbc;

    public UsoIaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Suma un uso solo si queda cupo. La condicion va en el UPDATE para que dos
     * peticiones simultaneas no puedan pasarse del limite.
     *
     * @return {@code false} si el usuario ya gasto todo el cupo del mes
     */
    public boolean consumir(UUID usuarioId, String mes, int limite) {
        jdbc.update("INSERT INTO uso_ia (usuario_id, mes, cantidad) VALUES (?, ?, 0) ON CONFLICT DO NOTHING", usuarioId, mes);
        return jdbc.update("UPDATE uso_ia SET cantidad = cantidad + 1 WHERE usuario_id = ? AND mes = ? AND cantidad < ?",
                usuarioId, mes, limite) == 1;
    }
}
