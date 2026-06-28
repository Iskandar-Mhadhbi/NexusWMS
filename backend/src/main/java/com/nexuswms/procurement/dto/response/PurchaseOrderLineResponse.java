package com.nexuswms.procurement.dto.response;

import com.nexuswms.procurement.entity.PurchaseOrderLineStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineResponse(
    UUID id,
    UUID skuId,
    String skuCode,
    String skuName,
    Integer quantityOrdered,
    Integer quantityReceived,
    BigDecimal unitPrice,
    BigDecimal lineTotal,           // quantityOrdered * unitPrice
    PurchaseOrderLineStatus status
) {}