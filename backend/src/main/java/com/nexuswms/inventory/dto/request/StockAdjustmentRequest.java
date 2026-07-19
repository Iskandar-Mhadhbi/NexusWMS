package com.nexuswms.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull(message = "SKU ID is required")
        UUID skuId,

        @NotNull(message = "Shelf ID is required")
        UUID shelfId,

        @NotNull(message = "Quantity is required")
        Integer quantity,

        String batchId,

        @NotBlank(message = "Reason is required")
        String reason
) {}