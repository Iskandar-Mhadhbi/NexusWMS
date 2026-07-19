package com.nexuswms.inventory.dto.response;

import com.nexuswms.inventory.entity.StockMovement;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        String skuCode,
        String shelfCode,
        Integer quantity,
        String movementType,
        String workerName,
        String batchId,
        String notes,
        LocalDateTime movedAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getSku().getSkuCode(),
                movement.getShelf() != null ? movement.getShelf().getCode() : null,
                movement.getQuantity(),
                movement.getMovementType(),
                movement.getWorker() != null ? movement.getWorker().getName() : null,
                movement.getBatchId(),
                movement.getNotes(),
                movement.getMovedAt()
        );
    }
}