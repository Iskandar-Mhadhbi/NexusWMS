// OrderLineRequest.java
package com.nexuswms.fulfillment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineRequest(

    @NotNull(message = "SKU ID is required")
    UUID skuId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantityOrdered,

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    BigDecimal unitPrice
) {}