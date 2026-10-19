# artagram-feed

Microservicio de Feed, Publicaciones, Versiones, Comentarios y Likes para ArtaGram.

## Tecnologías
- Java 21, Spring Boot 3.3.4
- Spring Data JPA + PostgreSQL (`db_feed`)
- Spring Data Redis (Caché en Redis db 1)
- Spring AMQP (RabbitMQ: publica `post.published`, `like.updated`, `comment.created`, etc.)
- AWS SDK v2 para almacenamiento de imágenes en S3/MinIO con URLs prefirmadas

## Endpoints
- `GET /api/publicaciones`
- `GET /api/publicaciones/{id}`
- `POST /api/publicaciones`
- `PUT /api/publicaciones/{id}`
- `DELETE /api/publicaciones/{id}`
- `POST /api/publicaciones/{id}/like`
- `GET /api/publicaciones/{id}/comentarios`
- `POST /api/publicaciones/{id}/comentarios`
- `DELETE /api/publicaciones/{id}/comentarios/{comentarioId}`
- `GET /api/publicaciones/{id}/versiones`
- `POST /api/publicaciones/{id}/versiones`
- `GET /api/feed/siguiendo`
- `GET /api/feed/explorar`
- `POST /api/media/presign`
- `GET /media/**`
- `GET /api/publicaciones/{id}/analisis`
- `POST /api/publicaciones/{id}/analisis`
- `POST /api/ia/sugerencias`

## Variables de Entorno
Ver `.env.example`.
