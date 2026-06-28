package com.nexuswms.procurement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderRequest(

    @NotNull
    UUID supplierId,

    LocalDate expectedDelivery,

    @NotNull @Size(min = 1, message = "At least one line is required")
    @Valid
    List<PurchaseOrderLineRequest> lines
) {}