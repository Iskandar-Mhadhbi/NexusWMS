// PickListResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.PickList;
import com.nexuswms.fulfillment.entity.PickListStatus;
import com.nexuswms.user.dto.response.UserSummaryResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PickListResponse(
    UUID id,
    UUID fulfillmentRequestId,
    UserSummaryResponse generatedBy,
    UserSummaryResponse assignedTo,
    PickListStatus status,
    List<PickListItemResponse> items,
    LocalDateTime generatedAt,
    LocalDateTime completedAt
) {
    public static PickListResponse from(PickList pickList, List<PickListItemResponse> items,UserSummaryResponse generatedBy,
            UserSummaryResponse assignedTo) {
        return new PickListResponse(
                pickList.getId(),
                pickList.getFulfillmentRequest().getId(),
                generatedBy,
                assignedTo,
                pickList.getStatus(),
                items,
                pickList.getGeneratedAt(),
                pickList.getCompletedAt()
        );
    }
}