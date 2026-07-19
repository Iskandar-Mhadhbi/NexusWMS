
package com.nexuswms.procurement.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineRequest(

    @NotNull
    UUID skuId,

    @Min(1)
    int quantityOrdered,

    @NotNull @DecimalMin("0.01")
    BigDecimal unitPrice
) {}