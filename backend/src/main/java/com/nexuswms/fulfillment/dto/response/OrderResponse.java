// OrderResponse.java
package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.Order;
import com.nexuswms.fulfillment.entity.OrderPriority;
import com.nexuswms.fulfillment.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String orderNumber,
    String customerName,
    Map<String, Object> customerAddress,
    OrderStatus status,
    OrderPriority priority,
    String notes,
    List<OrderLineResponse> lines,
    BigDecimal totalAmount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static OrderResponse from(Order order, List<OrderLineResponse> lines) {
        BigDecimal total = lines.stream()
                                .map(l -> l.lineTotal() != null ? l.lineTotal() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerAddress(),
                order.getStatus(),
                order.getPriority(),
                order.getNotes(),
                lines,
                total,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}