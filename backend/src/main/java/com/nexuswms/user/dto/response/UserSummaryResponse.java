package com.nexuswms.user.dto.response; 

import java.util.UUID;

import com.nexuswms.user.entity.Role;
import com.nexuswms.user.entity.User;
import com.nexuswms.user.entity.UserStatus;

public record UserSummaryResponse(
    UUID id,
    String employeeId,
    String name,
    String email,
    Role role,
    UserStatus status
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getEmployeeId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
    }
}