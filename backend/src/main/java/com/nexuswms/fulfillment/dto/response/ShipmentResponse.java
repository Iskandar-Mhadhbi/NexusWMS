// ShipmentResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.Shipment;
import com.nexuswms.fulfillment.entity.ShipmentStatus;

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
    UUID dispatchedBy,
    LocalDateTime dispatchedAt,
    LocalDate estimatedDelivery,
    ShipmentStatus status
) {
    public static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getParcel().getId(),
                shipment.getParcel().getTrackingNumber(),
                shipment.getCarrier().getId(),
                shipment.getCarrier().getName(),
                shipment.getCarrier().getCode(),
                shipment.getCarrierTrackingNumber(),
                shipment.getDispatchedBy(),
                shipment.getDispatchedAt(),
                shipment.getEstimatedDelivery(),
                shipment.getStatus()
        );
    }
}