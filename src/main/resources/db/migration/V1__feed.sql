CREATE TABLE IF NOT EXISTS usuarios_proyeccion (
    id UUID PRIMARY KEY,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    nombre_artista VARCHAR(100),
    foto_url VARCHAR(500),
    premium BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS seguidores_proyeccion (
    seguidor_id UUID NOT NULL,
    seguido_id UUID NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (seguidor_id, seguido_id)
);

CREATE TABLE IF NOT EXISTS publicaciones (
    id UUID PRIMARY KEY,
    autor_id UUID NOT NULL REFERENCES usuarios_proyeccion(id) ON DELETE CASCADE,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    tecnica VARCHAR(50),
    imagen_url VARCHAR(500) NOT NULL,
    miniatura_url VARCHAR(500),
    blurhash VARCHAR(100),
    contador_likes INT NOT NULL DEFAULT 0,
    contador_comentarios INT NOT NULL DEFAULT 0,
    contador_vistas INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP
);

CREATE TABLE IF NOT EXISTS etiquetas_publicacion (
    publicacion_id UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    etiqueta VARCHAR(50) NOT NULL,
    PRIMARY KEY (publicacion_id, etiqueta)
);

CREATE TABLE IF NOT EXISTS versiones_publicacion (
    id UUID PRIMARY KEY,
    publicacion_id UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    numero_version INT NOT NULL,
    imagen_url VARCHAR(500) NOT NULL,
    nota_cambio TEXT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS likes_publicacion (
    publicacion_id UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios_proyeccion(id) ON DELETE CASCADE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (publicacion_id, usuario_id)
);

CREATE TABLE IF NOT EXISTS comentarios_publicacion (
    id UUID PRIMARY KEY,
    publicacion_id UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    autor_id UUID NOT NULL REFERENCES usuarios_proyeccion(id) ON DELETE CASCADE,
    contenido TEXT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analisis_ia (
    id UUID PRIMARY KEY,
    publicacion_id UUID NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL,
    diagnostico_general TEXT,
    paleta_detectada TEXT,
    composicion TEXT,
    puntos_mejora TEXT,
    sugerencias TEXT,
    puntuacion_armonia INT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pub_creado_en ON publicaciones(creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_pub_autor_creado ON publicaciones(autor_id, creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_pub_likes ON publicaciones(contador_likes DESC, creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_etiquetas_etiqueta ON etiquetas_publicacion(etiqueta);
CREATE INDEX IF NOT EXISTS idx_comentarios_pub_creado ON comentarios_publicacion(publicacion_id, creado_en ASC);
CREATE INDEX IF NOT EXISTS idx_versiones_pub_num ON versiones_publicacion(publicacion_id, numero_version ASC);
CREATE INDEX IF NOT EXISTS idx_seguidores_seguidor ON seguidores_proyeccion(seguidor_id);
CREATE INDEX IF NOT EXISTS idx_seguidores_seguido ON seguidores_proyeccion(seguido_id);
