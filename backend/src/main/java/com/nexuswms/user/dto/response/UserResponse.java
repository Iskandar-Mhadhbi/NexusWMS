package com.nexuswms.user.dto.response;

import com.nexuswms.user.entity.User;

public record UserResponse(
        String employeeId,
        String email,
        String name,
        String role,
        String status
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getEmployeeId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}