package com.nexuswms.procurement.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptLineResponse(
    UUID id,
    UUID poLineId,
    UUID skuId,
    String skuCode,
    Integer quantityReceived,
    String batchId,
    LocalDate expiryDate,
    UUID shelfId,
    String shelfCode
) {}