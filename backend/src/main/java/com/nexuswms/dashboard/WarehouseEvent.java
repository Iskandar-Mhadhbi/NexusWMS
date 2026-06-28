package com.nexuswms.dashboard;

import java.time.Instant;

/**
 * Represents a warehouse operation event published to Redis pub/sub
 * and broadcast to connected WebSocket clients.
 */
public record WarehouseEvent(
        String type,
        String referenceId,
        String status,
        String workerId,
        String zone,
        Instant timestamp
) {
    public static WarehouseEvent of(String type, String referenceId, String status, String workerId, String zone) {
        return new WarehouseEvent(type, referenceId, status, workerId, zone, Instant.now());
    }
}