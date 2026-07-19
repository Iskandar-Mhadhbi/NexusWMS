package com.nexuswms.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class WarehouseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WarehouseEventPublisher.class);

    private static final String CHANNEL_EVENTS = "warehouse.events";
    private static final String CHANNEL_ALERTS = "alerts";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public WarehouseEventPublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /* ----- Publish Operational Events ----------------------------------- */

    /**
     * Publishes an event to the warehouse.events Redis channel.
     * Subscribed by DashboardWebSocketHandler which broadcasts to /topic/dashboard.
     */
    public void publish(WarehouseEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL_EVENTS, payload);
            log.debug("Published event [{}] for reference [{}]", event.type(), event.referenceId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize warehouse event: {}", e.getMessage());
        }
    }

    /* ----- Publish Reorder Alerts --------------------------------------- */

    /**
     * Publishes a reorder alert to the alerts Redis channel.
     * Subscribed by DashboardWebSocketHandler which broadcasts to /topic/alerts.
     */
    public void publishAlert(WarehouseEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL_ALERTS, payload);
            log.debug("Published alert [{}] for SKU [{}]", event.type(), event.referenceId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize alert event: {}", e.getMessage());
        }
    }
}