package com.company.support.user.dto.request;

import com.company.support.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Data
@Schema(description = "Request to create a new user")
public class CreateUserRequest {

    @NotNull
    @Schema(description = "User role", example = "ADMIN")
    private UserRole role;

    @NotBlank
    @Schema(description = "Full name", example = "Ivan Petrov")
    private String userFullName;

    @NotBlank
    @Size(min = 3, max = 255)
    @Schema(description = "Login (min 3 chars)", example = "ivan.petrov")
    private String userLogin;

    @Schema(description = "Phone number", example = "+79001234567")
    private String phoneNumber;

    @Email
    @NotBlank
    @Schema(description = "Email address", example = "ivan.petrov@company.com")
    private String email;

    @Schema(description = "Workplace", example = "Office 301")
    private String workplace;

    @NotBlank
    @Schema(description = "Password", example = "SecurePass123!")
    private String password;
}