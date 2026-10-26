package com.artagram.feed.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@ConditionalOnProperty(name = "artagram.storage.type", havingValue = "s3", matchIfMissing = true)
public class S3StorageProvider implements StorageProvider, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(S3StorageProvider.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3StorageProvider(
            @Value("${artagram.storage.endpoint:http://localhost:9000}") String endpoint,
            @Value("${artagram.storage.region:us-east-1}") String regionName,
            @Value("${artagram.storage.bucket:artagram-uploads}") String bucket,
            @Value("${artagram.storage.access-key:minioadmin}") String accessKey,
            @Value("${artagram.storage.secret-key:minioadmin}") String secretKey,
            @Value("${artagram.storage.path-style-access:true}") boolean pathStyleAccess) {
        this.bucket = bucket;
        AwsCredentialsProvider creds = (accessKey != null && !accessKey.isBlank())
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                : DefaultCredentialsProvider.create();
        var region = Region.of(regionName);

        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(region)
                .credentialsProvider(creds)
                .forcePathStyle(pathStyleAccess);
        if (endpoint != null && !endpoint.isBlank()) {
            clientBuilder.endpointOverride(URI.create(endpoint));
        }
        this.s3Client = clientBuilder.build();

        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(region)
                .credentialsProvider(creds)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build());
        if (endpoint != null && !endpoint.isBlank()) {
            presignerBuilder.endpointOverride(URI.create(endpoint));
        }
        this.s3Presigner = presignerBuilder.build();

        asegurarBucket();
    }

    private void asegurarBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            log.info("Creando bucket S3: {}", bucket);
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            } catch (Exception e) {
                log.warn("No se pudo auto-crear bucket S3 {}: {}", bucket, e.getMessage());
            }
        } catch (Exception ex) {
            log.warn("No se pudo verificar el bucket S3 {}: {}", bucket, ex.getMessage());
        }
    }

    @Override
    public void guardar(String ruta, InputStream contenido, long tamanoBytes, String contentType) throws IOException {
        String key = limpiarClave(ruta);
        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength(tamanoBytes);
        if (contentType != null && !contentType.isBlank()) {
            builder.contentType(contentType);
        }
        s3Client.putObject(builder.build(), RequestBody.fromInputStream(contenido, tamanoBytes));
    }

    @Override
    public boolean existe(String ruta) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(limpiarClave(ruta)).build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public Resource leer(String ruta) throws IOException {
        try {
            String key = limpiarClave(ruta);
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return new ByteArrayResource(bytes.asByteArray(), key);
        } catch (NoSuchKeyException ex) {
            throw new IOException("Archivo no encontrado en S3: " + ruta, ex);
        }
    }

    @Override
    public void eliminar(String ruta) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(limpiarClave(ruta)).build());
        } catch (Exception ex) {
            log.warn("Error eliminando clave S3 {}: {}", ruta, ex.getMessage());
        }
    }

    @Override
    public Optional<String> urlPrefirmadaSubida(String ruta, String contentType, Duration ttl) {
        PutObjectRequest.Builder porb = PutObjectRequest.builder().bucket(bucket).key(limpiarClave(ruta));
        if (contentType != null && !contentType.isBlank()) {
            porb.contentType(contentType);
        }
        PutObjectPresignRequest popr = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(porb.build())
                .build();
        return Optional.of(s3Presigner.presignPutObject(popr).url().toString());
    }

    @Override
    public Optional<String> urlPrefirmadaLectura(String ruta, Duration ttl) {
        GetObjectPresignRequest gopr = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(limpiarClave(ruta)).build())
                .build();
        return Optional.of(s3Presigner.presignGetObject(gopr).url().toString());
    }

    private String limpiarClave(String ruta) {
        if (ruta == null) return "";
        return ruta.replace('\\', '/').replaceAll("^/+", "");
    }

    @Override
    public void close() {
        try { s3Presigner.close(); } catch (Exception ignored) {}
        try { s3Client.close(); } catch (Exception ignored) {}
    }
}
