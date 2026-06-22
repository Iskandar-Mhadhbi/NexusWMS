package com.nexuswms.auth.dto;

public record AuthResponse(
        String token,
        String employeeId,
        String email,
        String name,
        String role,
        String status
) {}