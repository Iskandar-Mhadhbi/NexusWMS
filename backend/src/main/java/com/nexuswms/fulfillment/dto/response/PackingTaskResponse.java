package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.PackingTask;
import com.nexuswms.fulfillment.entity.PackingTaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PackingTaskResponse(
    UUID id,
    UUID pickListId,
    UUID assignedTo,
    UUID startedBy,
    UUID stationId,
    String stationCode,
    PackingTaskStatus status,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {
    public static PackingTaskResponse from(PackingTask task) {
        return new PackingTaskResponse(
                task.getId(),
                task.getPickList().getId(),
                task.getAssignedTo(),
                task.getStartedBy(),
                task.getStation() != null ? task.getStation().getId() : null,
                task.getStation() != null ? task.getStation().getCode() : null,
                task.getStatus(),
                task.getStartedAt(),
                task.getCompletedAt()
        );
    }
}