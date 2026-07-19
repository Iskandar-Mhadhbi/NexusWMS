package com.nexuswms.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ShelfRequest(
        @NotNull(message = "Aisle ID is required")
        UUID aisleId,

        @NotBlank(message = "Level is required")
        String level,

        @NotBlank(message = "Shelf code is required")
        String code,

        @PositiveOrZero
        BigDecimal maxWeight
) {}