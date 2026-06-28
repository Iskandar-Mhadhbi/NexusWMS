package com.nexuswms.dashboard.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class DashboardWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardWebSocketHandler.class);

    private static final String DESTINATION_DASHBOARD = "/topic/dashboard";
    private static final String DESTINATION_ALERTS    = "/topic/alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public DashboardWebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /* ----- Redis Message Receiver --------------------------------------- */

    /**
     * Called by RedisMessageListenerContainer whenever a message arrives
     * on warehouse.events or alerts channels.
     * Broadcasts the raw JSON payload to all subscribed WebSocket clients.
     *
     * @param message the JSON string published to Redis
     * @param channel the Redis channel the message arrived on ("warehouse.events" or "alerts")
     */
    public void handleMessage(String message, String channel) {
        log.debug("Received message on channel [{}]: {}", channel, message);

        if ("alerts".equals(channel)) {
            messagingTemplate.convertAndSend(DESTINATION_ALERTS, message);
            log.debug("Broadcast to {}", DESTINATION_ALERTS);
        } else {
            messagingTemplate.convertAndSend(DESTINATION_DASHBOARD, message);
            log.debug("Broadcast to {}", DESTINATION_DASHBOARD);
        }
    }
}