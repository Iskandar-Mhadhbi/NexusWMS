package com.nexuswms.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class TokenBlocklistService {

    private static final String BLOCKLIST_PREFIX = "blocklist:user:";

    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlocklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blockUser(UUID userId, Duration tokenRemainingTtl, String reason) {
        String key = BLOCKLIST_PREFIX + userId.toString();
        redisTemplate.opsForValue().set(key, reason, tokenRemainingTtl);
    }

    public boolean isUserBlocked(String userId) {
        String key = BLOCKLIST_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public String getBlockReason(String userId) {
        String key = BLOCKLIST_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }
}