package com.nexuswms.procurement.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceRequest(

    @NotNull
    UUID purchaseOrderId,

    @NotBlank
    String invoiceNumber,

    @NotNull @DecimalMin("0.01")
    BigDecimal invoiceAmount
) {}