package com.nexuswms.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record SkuRequest(
        @NotBlank(message = "SKU code is required")
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU code must be uppercase alphanumeric")
        String skuCode,

        @NotBlank(message = "SKU name is required")
        String name,

        UUID categoryId,

        @PositiveOrZero
        BigDecimal weightKg,

        Map<String, Object> dimensions,

        @NotBlank(message = "Unit is required")
        String unit,

        @PositiveOrZero
        @NotNull
        Integer reorderPoint,

        @PositiveOrZero
        @NotNull
        Integer reorderQuantity
) {}