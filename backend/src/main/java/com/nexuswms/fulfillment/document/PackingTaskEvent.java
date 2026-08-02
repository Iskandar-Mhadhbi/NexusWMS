package com.nexuswms.fulfillment.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representing a single packing task lifecycle event.
 * Append-only — one document per status change, never updated.
 * Provides full immutable audit trail per packing task.
 */
@Document(collection = "packing_task_events")
public class PackingTaskEvent {

    @Id
    private String id;

    private String taskId;
    private String taskNumber;
    private String eventType;
    private String status;
    private String actorId;
    private Instant timestamp;

    public PackingTaskEvent(String taskId, String taskNumber, String eventType,
                             String status, String actorId) {
        this.taskId = taskId;
        this.taskNumber = taskNumber;
        this.eventType = eventType;
        this.status = status;
        this.actorId = actorId;
        this.timestamp = Instant.now();
    }

    /* ----- Getters ------------------------------------------------------ */

    public String getId()         { return id; }
    public String getTaskId()     { return taskId; }
    public String getTaskNumber() { return taskNumber; }
    public String getEventType()  { return eventType; }
    public String getStatus()     { return status; }
    public String getActorId()    { return actorId; }
    public Instant getTimestamp() { return timestamp; }
}