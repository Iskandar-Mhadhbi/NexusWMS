package com.nexuswms.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ZoneRequest(
        @NotBlank(message = "Zone name is required")
        String name,

        @NotBlank(message = "Zone type is required")
        String type,

        @Positive(message = "Capacity must be positive")
        Integer capacity
) {}