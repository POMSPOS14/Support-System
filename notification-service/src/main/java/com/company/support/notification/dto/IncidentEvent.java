package com.company.support.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentEvent {

    private String eventType;
    private Long incidentId;
    private String incidentName;
}
