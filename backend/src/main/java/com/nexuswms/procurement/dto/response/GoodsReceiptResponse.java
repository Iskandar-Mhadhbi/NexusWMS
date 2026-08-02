package com.nexuswms.procurement.dto.response;

import com.nexuswms.user.dto.response.UserSummaryResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(
    UUID id,
    String grNumber,
    UUID purchaseOrderId,
    String poNumber,
    UserSummaryResponse receivedBy,
    LocalDateTime receivedAt,
    String notes,
    List<GoodsReceiptLineResponse> lines
) {}