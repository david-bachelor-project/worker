package com.paperless.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;

    // Consider injecting via @Value("${minio.bucket}") if you want configurability
    private final String bucketName = "documents";

    public byte[] getPdf(String objectName) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        )) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF bytes from MinIO", e);
        } catch (Exception e) {
            throw new RuntimeException("MinIO getObject failed", e);
        }
    }
}
