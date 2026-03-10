package com.company.support.incident.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String role;
    private String userFullName;
    private String userLogin;
    private String phoneNumber;
    private String email;
    private String workplace;
}
