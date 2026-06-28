package com.nexuswms.inventory.dto.response;

import java.util.UUID;

public record StockSummaryResponse(
        UUID skuId,
        String skuCode,
        String skuName,
        Integer totalQuantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer reorderPoint,
        boolean needsReorder
) {}