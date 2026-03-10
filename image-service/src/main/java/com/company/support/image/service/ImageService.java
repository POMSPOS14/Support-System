package com.company.support.image.service;

import com.company.support.image.entity.Image;
import com.company.support.image.repository.ImageRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ImageService {

    private static final Logger LOG = Logger.getLogger(ImageService.class);

    @Inject
    ImageRepository imageRepository;

    @Inject
    MinioService minioService;

    @WithTransaction
    public Uni<Image> upload(Long incidentId, String fileName, String contentType, byte[] data) {
        return minioService.upload(fileName, contentType, data)
                .flatMap(key -> {
                    var image = Image.builder()
                            .incidentId(incidentId)
                            .fileName(fileName)
                            .minioKey(key)
                            .contentType(contentType)
                            .size((long) data.length)
                            .uploadedAt(LocalDateTime.now())
                            .build();
                    return imageRepository.persistAndFlush(image);
                });
    }

    @WithSession
    public Uni<List<Image>> findByIncidentId(Long incidentId) {
        return imageRepository.findByIncidentId(incidentId);
    }

    @WithSession
    public Uni<Image> findById(Long id) {
        return imageRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Image not found: " + id));
    }

    public Uni<byte[]> downloadFile(String minioKey) {
        return minioService.download(minioKey);
    }

    @WithTransaction
    public Uni<Void> delete(Long id) {
        return imageRepository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Image not found: " + id))
                .flatMap(image -> minioService.delete(image.getMinioKey())
                        .replaceWith(imageRepository.delete(image)));
    }

    @WithTransaction
    public Uni<Void> deleteAllByIncidentId(Long incidentId) {
        LOG.infof("Deleting all images for incident #%d", incidentId);
        return imageRepository.findByIncidentId(incidentId)
                .flatMap(images -> {
                    if (images.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    // Delete all files from MinIO in parallel, then delete DB records
                    var minioDeletes = images.stream()
                            .map(img -> minioService.delete(img.getMinioKey()))
                            .toList();
                    return Uni.join().all(minioDeletes).andFailFast()
                            .flatMap(v -> imageRepository.deleteByIncidentId(incidentId))
                            .replaceWithVoid();
                });
    }
}