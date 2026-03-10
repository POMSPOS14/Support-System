package com.company.support.user.dto.request;

import com.company.support.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private UserRole role;
    private String userFullName;
    private String phoneNumber;
    @Email
    private String email;
    private String workplace;
}
