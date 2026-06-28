package com.nexuswms.procurement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record GoodsReceiptRequest(

    @NotNull
    UUID purchaseOrderId,

    String notes,

    @NotNull @Size(min = 1, message = "At least one line is required")
    @Valid
    List<GoodsReceiptLineRequest> lines
) {}