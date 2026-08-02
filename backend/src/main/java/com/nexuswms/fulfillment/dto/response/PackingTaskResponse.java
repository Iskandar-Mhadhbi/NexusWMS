package com.nexuswms.fulfillment.dto.response;

import com.nexuswms.fulfillment.entity.PackingTask;
import com.nexuswms.fulfillment.entity.PackingTaskStatus;
import com.nexuswms.user.dto.response.UserSummaryResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record PackingTaskResponse(
    UUID id,
    UUID pickListId,
    String taskNumber,
    UserSummaryResponse assignedTo,
    UserSummaryResponse startedBy,
    UUID stationId,
    String stationCode,
    PackingTaskStatus status,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {
    public static PackingTaskResponse from(PackingTask task, UserSummaryResponse assignedTo,
                                            UserSummaryResponse startedBy) {
        return new PackingTaskResponse(
                task.getId(),
                task.getPickList().getId(),
                task.getTaskNumber(),
                assignedTo,
                startedBy,
                task.getStation() != null ? task.getStation().getId() : null,
                task.getStation() != null ? task.getStation().getCode() : null,
                task.getStatus(),
                task.getStartedAt(),
                task.getCompletedAt()
        );
    }
}