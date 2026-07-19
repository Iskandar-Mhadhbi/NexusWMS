package com.nexuswms.user.dto.request;

import com.nexuswms.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating a user's account status.
 */
public record UpdateStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status
) {}