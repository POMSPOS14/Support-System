package com.company.support.user.dto.response;

import com.company.support.user.entity.UserRole;
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
    private UserRole role;
    private String userFullName;
    private String userLogin;
    private String phoneNumber;
    private String email;
    private String workplace;
}
