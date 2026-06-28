// CarrierResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.Carrier;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record CarrierResponse(
    UUID id,
    String name,
    String code,
    Map<String, Object> contactInfo,
    Boolean isActive,
    LocalDateTime createdAt
) {
    public static CarrierResponse from(Carrier carrier) {
        return new CarrierResponse(
                carrier.getId(),
                carrier.getName(),
                carrier.getCode(),
                carrier.getContactInfo(),
                carrier.getIsActive(),
                carrier.getCreatedAt()
        );
    }
}