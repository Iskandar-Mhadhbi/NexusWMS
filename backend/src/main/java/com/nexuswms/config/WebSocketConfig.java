package com.nexuswms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    /* ----- Broker Configuration ----------------------------------------- */

    /**
     * Configures the in-memory message broker.
     * /topic → broadcast channels (one publisher, many subscribers)
     * /app  → routes client messages to @MessageMapping controller methods
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic","/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /* ----- STOMP Endpoint ------------------------------------------------ */

    /**
     * Registers the WebSocket handshake endpoint.
     * Clients connect to: ws://localhost:8080/ws?token=<jwt>
     * SockJS fallback enabled for browsers without native WebSocket support.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .addInterceptors(webSocketAuthInterceptor)
            .withSockJS();
    }
}