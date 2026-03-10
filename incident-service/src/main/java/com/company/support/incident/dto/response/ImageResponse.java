package com.company.support.incident.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
    private Long id;
    private Long incidentId;
    private String url;
    private String fileName;
    private Long size;
    private String mediaType;
}
