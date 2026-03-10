package com.company.support.incident.dto.request;

import com.company.support.incident.entity.IncidentCategory;
import lombok.Data;

@Data
public class AssignCategoryRequest {
    private IncidentCategory category;
}
