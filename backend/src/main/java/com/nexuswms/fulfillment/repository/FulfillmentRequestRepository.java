// FulfillmentRequestRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.FulfillmentRequest;
import com.nexuswms.fulfillment.entity.FulfillmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FulfillmentRequestRepository extends JpaRepository<FulfillmentRequest, UUID> {
    Optional<FulfillmentRequest> findByOrder_Id(UUID orderId);
    List<FulfillmentRequest> findByStatus(FulfillmentStatus status);
}