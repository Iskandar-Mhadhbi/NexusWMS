package com.nexuswms.inventory.dto.response;

import com.nexuswms.inventory.entity.Aisle;

import java.util.UUID;

public record AisleResponse(
        UUID id,
        UUID zoneId,
        String zoneName,
        String code
) {
    public static AisleResponse from(Aisle aisle) {
        return new AisleResponse(
                aisle.getId(),
                aisle.getZone().getId(),
                aisle.getZone().getName(),
                aisle.getCode()
        );
    }
}