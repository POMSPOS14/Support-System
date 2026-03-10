package com.company.support.image.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "minio_key", nullable = false, unique = true)
    private String minioKey;

    @Column(name = "content_type")
    private String contentType;

    @Column
    private Long size;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
