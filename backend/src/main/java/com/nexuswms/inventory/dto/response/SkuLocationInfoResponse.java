package com.nexuswms.inventory.dto.response;

import java.util.UUID;

public record SkuLocationInfoResponse(
    UUID shelfId,
    String shelfCode,
    String batchId,
    Integer availableQuantity
) {}