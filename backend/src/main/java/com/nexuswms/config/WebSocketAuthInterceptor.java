package com.nexuswms.config;

import com.nexuswms.auth.security.JwtUtil;
import com.nexuswms.auth.service.TokenBlocklistService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlocklistService tokenBlocklistService;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, TokenBlocklistService tokenBlocklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlocklistService = tokenBlocklistService;
    }

    /* ----- Before Handshake --------------------------------------------- */

    /**
     * Extracts and validates the JWT from the ?token= query parameter.
     * If valid, stores the userId in the WebSocket session attributes
     * so downstream handlers can identify the connected user.
     * Returning false rejects the handshake with 403.
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        String query = request.getURI().getQuery();

        if (query == null || !query.contains("token=")) {
            return false;
        }

        String token = extractToken(query);

        if (!jwtUtil.isTokenValid(token)) {
            return false;
        }

        String userId = jwtUtil.extractUserId(token);

        if (tokenBlocklistService.isUserBlocked(userId)) {
            return false;
        }

        attributes.put("userId", userId);
        return true;
    }

    /* ----- After Handshake ---------------------------------------------- */

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    /* ----- Helpers -------------------------------------------------------- */

    private String extractToken(String query) {
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring("token=".length());
            }
        }
        return "";
    }
}