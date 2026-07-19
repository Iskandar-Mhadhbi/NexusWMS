package com.nexuswms.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "employee Id is required")
        String employeeId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = "^(?=.*\\d).+$",
            message = "Password must contain at least one number"
        )
        String password
) {}