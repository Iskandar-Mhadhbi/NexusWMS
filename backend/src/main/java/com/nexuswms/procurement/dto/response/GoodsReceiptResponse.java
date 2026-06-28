package com.nexuswms.procurement.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(
    UUID id,
    String grNumber,
    UUID purchaseOrderId,
    String poNumber,
    UUID receivedBy,
    LocalDateTime receivedAt,
    String notes,
    List<GoodsReceiptLineResponse> lines
) {}