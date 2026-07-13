package com.nexuswms.auth.dto.response;

public record AuthResponse(
        String token,
        String employeeId,
        String email,
        String name,
        String role,
        String status
) {}