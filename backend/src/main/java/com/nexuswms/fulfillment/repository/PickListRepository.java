// PickListRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.PickList;
import com.nexuswms.fulfillment.entity.PickListStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PickListRepository extends JpaRepository<PickList, UUID> {
    List<PickList> findByAssignedTo(UUID workerId);
    List<PickList> findByStatus(PickListStatus status);
    List<PickList> findByFulfillmentRequest_Id(UUID fulfillmentRequestId);
}