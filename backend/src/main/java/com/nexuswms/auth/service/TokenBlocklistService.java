package com.nexuswms.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Service responsible for managing the JWT token blocklist in Redis.
 *
 * <p>JWTs are stateless by design — once issued they cannot be invalidated
 * until expiry. This service maintains a Redis-backed blocklist keyed by
 * user ID, with a TTL matching the token's remaining lifetime. The
 * {@code JwtAuthFilter} checks this on every request, providing immediate
 * revocation without storing tokens in the database. TTL-based auto-cleanup
 * requires zero maintenance.</p>
 */
@Service
@RequiredArgsConstructor
public class TokenBlocklistService {

    private static final String BLOCKLIST_PREFIX = "blocklist:user:";

    private final RedisTemplate<String, String> redisTemplate;

    /* ----- Write Operations ----- */

    /**
     * Adds a user to the blocklist with a TTL equal to the token's remaining lifetime.
     * The value stored is the reason for blocking (e.g. LOGOUT, TERMINATED, INACTIVE).
     *
     * @param userId            The UUID of the user to block.
     * @param tokenRemainingTtl Duration until the user's current token expires.
     * @param reason            Human-readable reason stored as the Redis value.
     */
    public void blockUser(UUID userId, Duration tokenRemainingTtl, String reason) {
        String key = BLOCKLIST_PREFIX + userId;
        redisTemplate.opsForValue().set(key, reason, tokenRemainingTtl);
    }

    /**
     * Removes a user from the blocklist.
     * Called when an admin re-activates a previously suspended user,
     * allowing them to authenticate again with a fresh token.
     *
     * @param userId The UUID of the user to unblock.
     */
    public void unblockUser(UUID userId) {
        redisTemplate.delete(BLOCKLIST_PREFIX + userId);
    }

    /* ----- Read Operations ----- */

    /**
     * Checks whether the given user ID is present in the blocklist.
     *
     * @param userId The user's UUID as a string (extracted from JWT subject claim).
     * @return {@code true} if the user is blocked, {@code false} otherwise.
     */
    public boolean isUserBlocked(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLOCKLIST_PREFIX + userId));
    }

    /**
     * Returns the block reason for the given user, or {@code null} if not blocked.
     *
     * @param userId The user's UUID as a string.
     * @return The block reason string, or {@code null}.
     */
    public String getBlockReason(String userId) {
        return redisTemplate.opsForValue().get(BLOCKLIST_PREFIX + userId);
    }
}