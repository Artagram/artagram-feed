ALTER TABLE publicaciones
    ADD COLUMN destacada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN destacada_en TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_pub_destacadas ON publicaciones(autor_id, destacada_en DESC) WHERE destacada;
