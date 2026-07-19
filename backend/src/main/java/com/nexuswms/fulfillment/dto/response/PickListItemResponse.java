// PickListItemResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.PickListItem;
import com.nexuswms.fulfillment.entity.PickListItemStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PickListItemResponse(
    UUID id,
    UUID skuId,
    UUID shelfId,
    String shelfCode,
    Integer quantityToPick,
    Integer quantityPicked,
    String batchId,
    PickListItemStatus status,
    LocalDateTime pickedAt
) {
    public static PickListItemResponse from(PickListItem item) {
        return new PickListItemResponse(
                item.getId(),
                item.getSkuId(),
                item.getShelfId(),
                item.getShelfCode(),
                item.getQuantityToPick(),
                item.getQuantityPicked(),
                item.getBatchId(),
                item.getStatus(),
                item.getPickedAt()
        );
    }
}