package com.nexuswms.inventory.dto.response;

import com.nexuswms.inventory.entity.Zone;

import java.util.UUID;

public record ZoneResponse(
        UUID id,
        String name,
        String type,
        Integer capacity,
        Integer currentOccupancy,
        Integer availableCapacity
) {
    public static ZoneResponse from(Zone zone) {
        return new ZoneResponse(
                zone.getId(),
                zone.getName(),
                zone.getType(),
                zone.getCapacity(),
                zone.getCurrentOccupancy(),
                zone.getCapacity() - zone.getCurrentOccupancy()
        );
    }
}