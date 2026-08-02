// FulfillmentRequestResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.FulfillmentRequest;
import com.nexuswms.fulfillment.entity.FulfillmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FulfillmentRequestResponse(
    UUID id,
    UUID orderId,
    String orderNumber,
    UUID generatedBy,
    String assignedZone,
    FulfillmentStatus status,
    LocalDateTime createdAt
) {
    public static FulfillmentRequestResponse from(FulfillmentRequest fr) {
        return new FulfillmentRequestResponse(
                fr.getId(),
                fr.getOrder().getId(),
                fr.getOrder().getOrderNumber(),
                fr.getGeneratedBy(),
                fr.getAssignedZone(),
                fr.getStatus(),
                fr.getCreatedAt()
        );
    }
}