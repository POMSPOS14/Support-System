package com.company.support.incident.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_create", nullable = false)
    private LocalDateTime dateCreate;

    @Column(name = "date_closed")
    private LocalDateTime dateClosed;

    @Column(name = "analyst_id")
    private Long analystId;

    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsible_service")
    private ResponsibleService responsibleService;
}
