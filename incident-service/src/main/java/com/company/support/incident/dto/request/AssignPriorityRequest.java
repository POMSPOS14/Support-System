package com.company.support.incident.dto.request;

import com.company.support.incident.entity.IncidentPriority;
import lombok.Data;

@Data
public class AssignPriorityRequest {
    private IncidentPriority priority;
}
