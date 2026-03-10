package com.company.support.incident.dto.request;

import com.company.support.incident.entity.ResponsibleService;
import lombok.Data;

@Data
public class AssignResponsibleServiceRequest {
    private ResponsibleService responsibleService;
}
