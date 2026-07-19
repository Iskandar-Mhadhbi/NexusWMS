package com.nexuswms.procurement.dto.response;

import com.nexuswms.procurement.entity.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(
    UUID id,
    String poNumber,
    UUID supplierId,
    String supplierName,
    UUID requestedBy,
    UUID approvedBy,
    PurchaseOrderStatus status,
    LocalDate expectedDelivery,
    BigDecimal totalAmount,
    LocalDateTime createdAt,
    List<PurchaseOrderLineResponse> lines
) {}