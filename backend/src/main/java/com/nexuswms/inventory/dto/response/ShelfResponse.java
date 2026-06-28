package com.nexuswms.inventory.dto.response;

import com.nexuswms.inventory.entity.Shelf;

import java.math.BigDecimal;
import java.util.UUID;

public record ShelfResponse(
        UUID id,
        UUID aisleId,
        String aisleCode,
        String level,
        String code,
        BigDecimal maxWeight,
        BigDecimal currentWeight
) {
    public static ShelfResponse from(Shelf shelf) {
        return new ShelfResponse(
                shelf.getId(),
                shelf.getAisle().getId(),
                shelf.getAisle().getCode(),
                shelf.getLevel(),
                shelf.getCode(),
                shelf.getMaxWeight(),
                shelf.getCurrentWeight()
        );
    }
}