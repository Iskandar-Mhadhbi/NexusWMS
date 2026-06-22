package com.nexuswms.auth.dto;
 
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email or employee ID is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {}