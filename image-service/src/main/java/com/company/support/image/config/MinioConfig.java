package com.company.support.image.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MinioConfig {

    private static final Logger LOG = Logger.getLogger(MinioConfig.class);

    @ConfigProperty(name = "minio.url")
    String minioUrl;

    @ConfigProperty(name = "minio.access-key")
    String accessKey;

    @ConfigProperty(name = "minio.secret-key")
    String secretKey;

    @ConfigProperty(name = "minio.bucket")
    String bucket;

    private MinioClient minioClient;

    public MinioClient getClient() {
        if (minioClient == null) {
            minioClient = MinioClient.builder()
                    .endpoint(minioUrl)
                    .credentials(accessKey, secretKey)
                    .build();
        }
        return minioClient;
    }

    public String getBucket() {
        return bucket;
    }

    void onStart(@Observes StartupEvent event) {
        try {
            MinioClient client = getClient();
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                LOG.infof("MinIO bucket '%s' created", bucket);
            } else {
                LOG.infof("MinIO bucket '%s' already exists", bucket);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize MinIO bucket '%s'", bucket);
        }
    }
}
