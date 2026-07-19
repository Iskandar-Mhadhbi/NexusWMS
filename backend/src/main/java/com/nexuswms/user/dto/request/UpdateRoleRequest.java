package com.nexuswms.user.dto.request; 

import com.nexuswms.user.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating a user's assigned role.
 */
public record UpdateRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}