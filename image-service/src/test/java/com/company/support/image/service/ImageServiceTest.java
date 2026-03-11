package com.company.support.image.service;

import com.company.support.image.entity.Image;
import com.company.support.image.repository.ImageRepository;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    ImageRepository imageRepository;

    @Mock
    MinioService minioService;

    @InjectMocks
    ImageService imageService;

    // --- upload ---

    @Test
    void upload_shouldSaveImageMetadataAfterMinioUpload() {
        byte[] data = "fake-image".getBytes();
        var saved = Image.builder()
                .id(1L).incidentId(10L).fileName("photo.jpg")
                .minioKey("uuid_photo.jpg").contentType("image/jpeg")
                .size((long) data.length).uploadedAt(LocalDateTime.now()).build();

        when(minioService.upload("photo.jpg", "image/jpeg", data))
                .thenReturn(Uni.createFrom().item("uuid_photo.jpg"));
        when(imageRepository.persistAndFlush(any(Image.class)))
                .thenReturn(Uni.createFrom().item(saved));

        var result = imageService.upload(10L, "photo.jpg", "image/jpeg", data).await().indefinitely();

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMinioKey()).isEqualTo("uuid_photo.jpg");
        verify(minioService).upload("photo.jpg", "image/jpeg", data);
        verify(imageRepository).persistAndFlush(any(Image.class));
    }

    // --- findByIncidentId ---

    @Test
    void findByIncidentId_shouldReturnImages() {
        var images = List.of(
                Image.builder().id(1L).incidentId(5L).fileName("a.jpg").build(),
                Image.builder().id(2L).incidentId(5L).fileName("b.jpg").build()
        );

        when(imageRepository.findByIncidentId(5L)).thenReturn(Uni.createFrom().item(images));

        var result = imageService.findByIncidentId(5L).await().indefinitely();

        assertThat(result).hasSize(2);
    }

    @Test
    void findByIncidentId_shouldReturnEmptyListWhenNoImages() {
        when(imageRepository.findByIncidentId(99L)).thenReturn(Uni.createFrom().item(List.of()));

        var result = imageService.findByIncidentId(99L).await().indefinitely();

        assertThat(result).isEmpty();
    }

    // --- findById ---

    @Test
    void findById_shouldReturnImage() {
        var image = Image.builder().id(1L).fileName("photo.jpg").build();

        when(imageRepository.findById(1L)).thenReturn(Uni.createFrom().item(image));

        var result = imageService.findById(1L).await().indefinitely();

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_shouldThrowNotFoundWhenImageDoesNotExist() {
        when(imageRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> imageService.findById(999L).await().indefinitely())
                .isInstanceOf(NotFoundException.class);
    }

    // --- delete ---

    @Test
    void delete_shouldRemoveFromMinioAndRepository() {
        var image = Image.builder().id(1L).minioKey("uuid_photo.jpg").build();

        when(imageRepository.findById(1L)).thenReturn(Uni.createFrom().item(image));
        when(minioService.delete("uuid_photo.jpg")).thenReturn(Uni.createFrom().voidItem());
        when(imageRepository.delete(image)).thenReturn(Uni.createFrom().voidItem());

        imageService.delete(1L).await().indefinitely();

        verify(minioService).delete("uuid_photo.jpg");
        verify(imageRepository).delete(image);
    }

    @Test
    void delete_shouldThrowNotFoundWhenImageDoesNotExist() {
        when(imageRepository.findById(999L)).thenReturn(Uni.createFrom().nullItem());

        assertThatThrownBy(() -> imageService.delete(999L).await().indefinitely())
                .isInstanceOf(NotFoundException.class);
    }

    // --- deleteAllByIncidentId ---

    @Test
    void deleteAllByIncidentId_shouldDeleteAllImagesAndMinioFiles() {
        var images = List.of(
                Image.builder().id(1L).minioKey("key1").build(),
                Image.builder().id(2L).minioKey("key2").build()
        );

        when(imageRepository.findByIncidentId(5L)).thenReturn(Uni.createFrom().item(images));
        when(minioService.delete(anyString())).thenReturn(Uni.createFrom().voidItem());
        when(imageRepository.deleteByIncidentId(5L)).thenReturn(Uni.createFrom().item(2L));

        imageService.deleteAllByIncidentId(5L).await().indefinitely();

        verify(minioService).delete("key1");
        verify(minioService).delete("key2");
        verify(imageRepository).deleteByIncidentId(5L);
    }

    @Test
    void deleteAllByIncidentId_shouldDoNothingWhenNoImages() {
        when(imageRepository.findByIncidentId(99L)).thenReturn(Uni.createFrom().item(List.of()));

        imageService.deleteAllByIncidentId(99L).await().indefinitely();

        verify(minioService, never()).delete(anyString());
        verify(imageRepository, never()).deleteByIncidentId(anyLong());
    }
}