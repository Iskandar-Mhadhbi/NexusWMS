package com.nexuswms.procurement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.nexuswms.procurement.entity.SupplierStatus;

public record SupplierResponse(
    UUID id,
    String name,
    String code,
    Map<String, Object> contactInfo,
    Integer paymentTerms,
    BigDecimal rating,
    SupplierStatus status,
    LocalDateTime createdAt
) {}