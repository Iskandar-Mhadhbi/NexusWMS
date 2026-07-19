// DispatchRequest.java
package com.nexuswms.fulfillment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record DispatchRequest(

    @NotNull(message = "Carrier ID is required")
    UUID carrierId,

    LocalDate estimatedDelivery
) {}