package com.company.support.image.service;

import com.company.support.image.config.MinioConfig;
import io.minio.*;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@ApplicationScoped
public class MinioService {

    private static final Logger LOG = Logger.getLogger(MinioService.class);

    @Inject
    MinioConfig minioConfig;

    @Inject
    Vertx vertx;

    public Uni<String> upload(String fileName, String contentType, byte[] data) {
        String key = UUID.randomUUID() + "_" + fileName;
        return vertx.executeBlocking(Uni.createFrom().item(() -> {
            try {
                minioConfig.getClient().putObject(
                        PutObjectArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .object(key)
                                .stream(new ByteArrayInputStream(data), data.length, -1)
                                .contentType(contentType)
                                .build());
                LOG.infof("Uploaded file to MinIO: %s", key);
                return key;
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload file to MinIO: " + e.getMessage(), e);
            }
        }));
    }

    public Uni<Void> delete(String key) {
        return vertx.executeBlocking(Uni.createFrom().item(() -> {
            try {
                minioConfig.getClient().removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .object(key)
                                .build());
                LOG.infof("Deleted file from MinIO: %s", key);
                return (Void) null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete file from MinIO: " + e.getMessage(), e);
            }
        }));
    }

    public Uni<byte[]> download(String key) {
        return vertx.executeBlocking(Uni.createFrom().item(() -> {
            try {
                var stream = minioConfig.getClient().getObject(
                        GetObjectArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .object(key)
                                .build());
                byte[] bytes = stream.readAllBytes();
                stream.close();
                LOG.infof("Downloaded file from MinIO: %s", key);
                return bytes;
            } catch (Exception e) {
                throw new RuntimeException("Failed to download file from MinIO: " + e.getMessage(), e);
            }
        }));
    }

    public String buildUrl(String key) {
        return minioConfig.getBucket() + "/" + key;
    }
}