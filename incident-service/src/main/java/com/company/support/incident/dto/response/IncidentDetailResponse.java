package com.company.support.incident.dto.response;

import com.company.support.incident.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDetailResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime dateCreate;
    private LocalDateTime dateClosed;
    private IncidentStatus status;
    private IncidentPriority priority;
    private IncidentCategory category;
    private ResponsibleService responsibleService;
    private UserResponse analyst;
    private UserResponse initiator;
    private List<ImageResponse> images;
}
