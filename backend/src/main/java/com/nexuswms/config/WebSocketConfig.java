package com.nexuswms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket configuration for the manager dashboard's live feed.
 *
 * <p>Authentication happens once, at the STOMP CONNECT frame, via
 * {@link StompAuthChannelInterceptor} — see
 * {@link #configureClientInboundChannel}. The HTTP handshake itself is
 * intentionally unauthenticated (SecurityConfig permits "/ws/**").</p>
 *
 * <p><b>CORS note:</b> Spring's SockJS support (the /info, /xhr-streaming,
 * etc. sub-endpoints under /ws/dashboard/**) writes its own CORS headers —
 * entirely separate from the CorsConfigurationSource bean the REST API uses
 * (see SecurityConfig). sockjs-client's handshake requests are sent with
 * credentials (withCredentials: true), and a browser requires
 * Access-Control-Allow-Credentials to be the literal string "true" on any
 * credentialed response — which can never legally coexist with a wildcard
 * origin. Using the same explicit, environment-configured origin list the
 * REST CORS bean uses (via AppProperties) instead of "*" is what lets
 * Spring's SockJS layer legitimately emit that header.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final AppProperties appProperties;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor,
                            AppProperties appProperties) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.appProperties = appProperties;
    }

    /* ----- Broker Configuration ----------------------------------------- */

    /**
     * Configures the in-memory message broker.
     * /topic → broadcast channels (one publisher, many subscribers)
     * /app  → routes client messages to @MessageMapping controller methods
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /* ----- STOMP Endpoint ------------------------------------------------ */

    /**
     * Registers the WebSocket handshake endpoint at /ws/dashboard.
     * Uses the explicit allowed-origins list (not "*") — required for
     * SockJS to emit a valid Access-Control-Allow-Credentials: true
     * response, since sockjs-client's handshake requests are credentialed.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = appProperties.getCors().getAllowedOrigins().toArray(new String[0]);

        registry
            .addEndpoint("/ws/dashboard")
            .setAllowedOrigins(origins)
            .withSockJS();
    }

    /* ----- Inbound Channel Interceptor ------------------------------------ */

    /**
     * Registers the STOMP-level auth interceptor on the client-to-server
     * message channel — this authenticates every connection via the STOMP
     * CONNECT frame's Authorization header (see StompAuthChannelInterceptor).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}