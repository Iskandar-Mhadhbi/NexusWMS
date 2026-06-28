package com.nexuswms.inventory.dto.response;

import com.nexuswms.inventory.entity.Sku;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record SkuResponse(
        UUID id,
        String skuCode,
        String name,
        UUID categoryId,
        String categoryName,
        BigDecimal weightKg,
        Map<String, Object> dimensions,
        String unit,
        Integer reorderPoint,
        Integer reorderQuantity
) {
    public static SkuResponse from(Sku sku) {
        return new SkuResponse(
                sku.getId(),
                sku.getSkuCode(),
                sku.getName(),
                sku.getCategory() != null ? sku.getCategory().getId() : null,
                sku.getCategory() != null ? sku.getCategory().getName() : null,
                sku.getWeightKg(),
                sku.getDimensions(),
                sku.getUnit(),
                sku.getReorderPoint(),
                sku.getReorderQuantity()
        );
    }
}