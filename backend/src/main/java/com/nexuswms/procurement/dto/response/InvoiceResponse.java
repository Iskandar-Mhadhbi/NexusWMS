package com.nexuswms.procurement.dto.response;

import com.nexuswms.procurement.entity.InvoiceStatus;
import com.nexuswms.procurement.entity.ThreeWayMatchStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    UUID purchaseOrderId,
    String poNumber,
    UUID supplierId,
    String supplierName,
    String invoiceNumber,
    BigDecimal invoiceAmount,
    InvoiceStatus status,
    ThreeWayMatchStatus threeWayMatchStatus,
    LocalDateTime createdAt
) {}