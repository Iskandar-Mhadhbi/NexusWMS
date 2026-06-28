// PickItemRequest.java
package com.nexuswms.fulfillment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PickItemRequest(

    @NotNull(message = "Quantity picked is required")
    @Min(value = 1, message = "Quantity picked must be at least 1")
    Integer quantityPicked
) {}