// PickListResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.PickList;
import com.nexuswms.fulfillment.entity.PickListStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PickListResponse(
    UUID id,
    UUID fulfillmentRequestId,
    UUID assignedTo,
    PickListStatus status,
    List<PickListItemResponse> items,
    LocalDateTime generatedAt,
    LocalDateTime completedAt
) {
    public static PickListResponse from(PickList pickList, List<PickListItemResponse> items) {
        return new PickListResponse(
                pickList.getId(),
                pickList.getFulfillmentRequest().getId(),
                pickList.getAssignedTo(),
                pickList.getStatus(),
                items,
                pickList.getGeneratedAt(),
                pickList.getCompletedAt()
        );
    }
}