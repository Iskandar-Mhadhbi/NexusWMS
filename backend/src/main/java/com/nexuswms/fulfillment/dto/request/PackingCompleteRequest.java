// PackingCompleteRequest.java
package com.nexuswms.fulfillment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record PackingCompleteRequest(

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be greater than 0")
    BigDecimal weightKg,

    Map<String, Object> dimensions
) {}