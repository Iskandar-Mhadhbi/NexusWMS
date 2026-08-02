package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.Shipment;
import com.nexuswms.fulfillment.entity.ShipmentStatus;
import com.nexuswms.user.dto.response.UserSummaryResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentResponse(
    UUID id,
    UUID parcelId,
    String trackingNumber,
    UUID carrierId,
    String carrierName,
    String carrierCode,
    String carrierTrackingNumber,
    UserSummaryResponse dispatchedBy,
    LocalDateTime dispatchedAt,
    LocalDate estimatedDelivery,
    ShipmentStatus status
) {
    public static ShipmentResponse from(Shipment shipment, UserSummaryResponse dispatchedBy) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getParcel().getId(),
                shipment.getParcel().getTrackingNumber(),
                shipment.getCarrier().getId(),
                shipment.getCarrier().getName(),
                shipment.getCarrier().getCode(),
                shipment.getCarrierTrackingNumber(),
                dispatchedBy,
                shipment.getDispatchedAt(),
                shipment.getEstimatedDelivery(),
                shipment.getStatus()
        );
    }
}