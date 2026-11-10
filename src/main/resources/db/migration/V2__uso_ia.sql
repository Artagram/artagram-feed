CREATE TABLE IF NOT EXISTS uso_ia (
    usuario_id UUID NOT NULL,
    mes VARCHAR(7) NOT NULL,
    cantidad INT NOT NULL DEFAULT 0,
    PRIMARY KEY (usuario_id, mes)
);
