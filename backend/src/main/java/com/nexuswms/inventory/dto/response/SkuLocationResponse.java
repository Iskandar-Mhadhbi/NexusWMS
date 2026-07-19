package com.nexuswms.inventory.dto.response;

import com.nexuswms.inventory.entity.SkuLocation;

import java.time.LocalDate;
import java.util.UUID;

public record SkuLocationResponse(
        UUID id,
        UUID skuId,
        String skuCode,
        UUID shelfId,
        String shelfCode,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        String batchId,
        LocalDate expiryDate
) {
    public static SkuLocationResponse from(SkuLocation loc) {
        return new SkuLocationResponse(
                loc.getId(),
                loc.getSku().getId(),
                loc.getSku().getSkuCode(),
                loc.getShelf().getId(),
                loc.getShelf().getCode(),
                loc.getQuantity(),
                loc.getReservedQuantity(),
                loc.getQuantity() - loc.getReservedQuantity(),
                loc.getBatchId(),
                loc.getExpiryDate()
        );
    }
}