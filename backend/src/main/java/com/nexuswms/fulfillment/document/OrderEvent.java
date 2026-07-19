package com.nexuswms.fulfillment.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document representing a single order lifecycle event.
 * Append-only — one document per status change, never updated.
 * Provides full immutable audit trail per order.
 */
@Document(collection = "order_events")
public class OrderEvent {

    @Id
    private String id;

    private String orderId;
    private String orderNumber;
    private String eventType;
    private String status;
    private String actorId;
    private Instant timestamp;

    public OrderEvent(String orderId, String orderNumber, String eventType,
                      String status, String actorId) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.eventType = eventType;
        this.status = status;
        this.actorId = actorId;
        this.timestamp = Instant.now();
    }

    /* ----- Getters ------------------------------------------------------ */

    public String getId()          { return id; }
    public String getOrderId()     { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public String getEventType()   { return eventType; }
    public String getStatus()      { return status; }
    public String getActorId()     { return actorId; }
    public Instant getTimestamp()  { return timestamp; }
}