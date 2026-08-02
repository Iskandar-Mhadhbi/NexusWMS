// OrderLineRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderLineRepository extends JpaRepository<OrderLine, UUID> {
    List<OrderLine> findByOrder_Id(UUID orderId);
    List<OrderLine> findByOrder_IdIn(List<UUID> orderIds);
}