ALTER TABLE publicaciones ADD COLUMN oculta BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS reportes_publicacion (
    id UUID PRIMARY KEY,
    publicacion_id UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    reportante_id UUID NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    detalle VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (publicacion_id, reportante_id)
);

CREATE INDEX IF NOT EXISTS idx_reportes_estado ON reportes_publicacion(estado, creado_en DESC);
