package com.nexuswms.procurement.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptLineRequest(

    @NotNull
    UUID poLineId,

    @NotNull
    UUID skuId,

    @Min(1)
    int quantityReceived,

    String batchId,

    LocalDate expiryDate,

    @NotNull
    UUID shelfId
) {}