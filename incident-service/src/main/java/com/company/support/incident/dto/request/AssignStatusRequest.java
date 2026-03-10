package com.company.support.incident.dto.request;

import com.company.support.incident.entity.IncidentStatus;
import lombok.Data;

@Data
public class AssignStatusRequest {
    private IncidentStatus status;
}
