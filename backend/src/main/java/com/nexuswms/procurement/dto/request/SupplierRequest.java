package com.nexuswms.procurement.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record SupplierRequest(

    @NotBlank
    String name,

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Code must be uppercase alphanumeric")
    String code,

    Map<String, Object> contactInfo,

    @Min(0)
    Integer paymentTerms,

    @DecimalMin("0.0") @DecimalMax("5.0")
    BigDecimal rating
) {}