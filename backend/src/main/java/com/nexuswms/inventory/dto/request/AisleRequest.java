package com.nexuswms.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AisleRequest(
        @NotNull(message = "Zone ID is required")
        UUID zoneId,

        @NotBlank(message = "Aisle code is required")
        String code
) {}