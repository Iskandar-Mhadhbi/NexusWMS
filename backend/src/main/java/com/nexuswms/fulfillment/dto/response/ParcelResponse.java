// ParcelResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.Parcel;
import com.nexuswms.fulfillment.entity.PackageStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ParcelResponse(
    UUID id,
    UUID orderId,
    String orderNumber,
    String trackingNumber,
    String barcode,
    BigDecimal weightKg,
    Map<String, Object> dimensions,
    PackageStatus status,
    LocalDateTime packedAt
) {
    public static ParcelResponse from(Parcel parcel) {
        return new ParcelResponse(
                parcel.getId(),
                parcel.getOrder().getId(),
                parcel.getOrder().getOrderNumber(),
                parcel.getTrackingNumber(),
                parcel.getBarcode(),
                parcel.getWeightKg(),
                parcel.getDimensions(),
                parcel.getStatus(),
                parcel.getPackedAt()
        );
    }
}