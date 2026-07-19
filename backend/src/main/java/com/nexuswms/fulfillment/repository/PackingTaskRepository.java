// PackingTaskRepository.java
package com.nexuswms.fulfillment.repository;

import com.nexuswms.fulfillment.entity.PackingTask;
import com.nexuswms.fulfillment.entity.PackingTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackingTaskRepository extends JpaRepository<PackingTask, UUID> {
    List<PackingTask> findByAssignedTo(UUID workerId);
    List<PackingTask> findByStatus(PackingTaskStatus status);
    List<PackingTask> findByPickList_Id(UUID pickListId);
}