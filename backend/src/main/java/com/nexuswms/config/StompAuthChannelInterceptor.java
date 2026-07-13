package com.nexuswms.config;

import com.nexuswms.auth.security.JwtUtil;
import com.nexuswms.auth.service.TokenBlocklistService;
import com.nexuswms.user.entity.Role;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP-level authentication interceptor.
 *
 * <p>Runs on every inbound STOMP frame, but only performs a check on
 * {@code CONNECT} frames — that's the one moment a client authenticates
 * for the whole WebSocket session. The JWT travels in the STOMP frame's
 * own {@code Authorization} header (set via connectHeaders on the frontend
 * STOMP client), never in the handshake URL — avoiding the token ever
 * appearing in server/proxy access logs or browser history, which a
 * query-param token would.</p>
 *
 * <p>This replaces {@code WebSocketAuthInterceptor} (HTTP handshake-level
 * auth via a HandshakeInterceptor). That interceptor validated a
 * query-param token before the WebSocket even upgraded; now the handshake
 * itself is unauthenticated (already permitted via SecurityConfig's
 * "/ws/**" permitAll) and this single interceptor is the sole
 * authentication point for the whole STOMP session.</p>
 *
 * <p>On success, the resolved authentication is attached to the STOMP
 * frame via {@code accessor.setUser(...)} — this is what makes
 * {@code Principal} injectable in {@code @MessageMapping} handler methods,
 * and is required (not optional) for Spring's user-destination support.</p>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenBlocklistService tokenBlocklistService;

    public StompAuthChannelInterceptor(JwtUtil jwtUtil, TokenBlocklistService tokenBlocklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlocklistService = tokenBlocklistService;
    }

    /* ----- Inbound Message Interception ---------------------------------- */

    /**
     * Validates the JWT carried on a STOMP CONNECT frame's Authorization
     * header. Any other frame type (SEND, SUBSCRIBE, DISCONNECT...) passes
     * through untouched — auth happens once, at CONNECT, for the whole
     * session.
     *
     * <p>Throwing rather than silently returning null matters here: a
     * dropped message with no signal would leave the client's onConnect
     * callback hanging forever. Throwing converts into a STOMP ERROR frame,
     * which the frontend's onStompError callback receives.</p>
     *
     * @param message the inbound STOMP message
     * @param channel the channel it's being sent on
     * @return the original message, unmodified, once auth succeeds (or
     *         immediately for any non-CONNECT frame)
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or malformed Authorization header on STOMP CONNECT");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired JWT on STOMP CONNECT");
        }

        String userId = jwtUtil.extractUserId(token);

        if (tokenBlocklistService.isUserBlocked(userId)) {
            throw new IllegalArgumentException("User is blocked");
        }

        String roleStr = jwtUtil.extractRole(token);
        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role on JWT: " + roleStr);
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        // Makes the connected user identifiable in @MessageMapping handlers
        // via a Principal parameter, and is required for user-destinations.
        accessor.setUser(authentication);

        return message;
    }
}