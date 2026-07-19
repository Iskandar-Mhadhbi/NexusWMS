// OrderLineResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.OrderLine;
import com.nexuswms.fulfillment.entity.OrderLineStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
    UUID id,
    UUID skuId,
    String skuCode,
    String skuName,
    Integer quantityOrdered,
    Integer quantityPicked,
    Integer quantityPacked,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    OrderLineStatus status
) {
    public static OrderLineResponse from(OrderLine line, String skuCode, String skuName) {
        return new OrderLineResponse(
                line.getId(),
                line.getSkuId(),
                skuCode,
                skuName,
                line.getQuantityOrdered(),
                line.getQuantityPicked(),
                line.getQuantityPacked(),
                line.getUnitPrice(),
                line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantityOrdered())),
                line.getStatus()
        );
    }
}