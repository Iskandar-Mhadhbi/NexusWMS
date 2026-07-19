// PickListItemRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.PickListItem;
import com.nexuswms.fulfillment.entity.PickListItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PickListItemRepository extends JpaRepository<PickListItem, UUID> {
    List<PickListItem> findByPickList_Id(UUID pickListId);
    List<PickListItem> findByPickList_IdAndStatus(UUID pickListId, PickListItemStatus status);
}