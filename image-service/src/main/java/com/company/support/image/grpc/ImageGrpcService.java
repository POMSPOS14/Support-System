package com.company.support.image.grpc;

import com.company.support.image.service.ImageService;
import com.google.protobuf.ByteString;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

import java.util.List;

@GrpcService
public class ImageGrpcService extends MutinyImageServiceGrpc.ImageServiceImplBase {

    @Inject
    ImageService imageService;

    @Override
    public Uni<ImageResponse> uploadImage(UploadImageRequest request) {
        return imageService.upload(
                        request.getIncidentId(),
                        request.getFileName(),
                        request.getContentType(),
                        request.getData().toByteArray())
                .map(image -> ImageResponse.newBuilder()
                        .setId(image.getId())
                        .setIncidentId(image.getIncidentId())
                        .setFileName(image.getFileName())
                        .setMediaType(image.getContentType() != null ? image.getContentType() : "")
                        .setSize(image.getSize() != null ? image.getSize() : 0)
                        .setUrl(image.getMinioKey())
                        .build());
    }

    @Override
    public Uni<ImageListResponse> getImagesByIncidentId(GetImagesByIncidentIdRequest request) {
        return imageService.findByIncidentId(request.getIncidentId())
                .map(images -> {
                    List<ImageResponse> responses = images.stream()
                            .map(image -> ImageResponse.newBuilder()
                                    .setId(image.getId())
                                    .setIncidentId(image.getIncidentId())
                                    .setFileName(image.getFileName())
                                    .setMediaType(image.getContentType() != null ? image.getContentType() : "")
                                    .setSize(image.getSize() != null ? image.getSize() : 0)
                                    .setUrl(image.getMinioKey())
                                    .build())
                            .toList();
                    return ImageListResponse.newBuilder()
                            .addAllImages(responses)
                            .build();
                });
    }

    @Override
    public Uni<DeleteImageResponse> deleteImage(DeleteImageRequest request) {
        return imageService.delete(request.getId())
                .map(v -> DeleteImageResponse.newBuilder().setSuccess(true).build())
                .onFailure().recoverWithItem(
                        DeleteImageResponse.newBuilder().setSuccess(false).build());
    }

    @Override
    public Uni<DownloadImageResponse> downloadImage(DownloadImageRequest request) {
        return imageService.findById(request.getId())
                .flatMap(image -> imageService.downloadFile(image.getMinioKey())
                        .map(bytes -> DownloadImageResponse.newBuilder()
                                .setFileName(image.getFileName())
                                .setContentType(image.getContentType() != null ? image.getContentType() : "application/octet-stream")
                                .setData(ByteString.copyFrom(bytes))
                                .build()));
    }
}