package com.nexuswms.auth.security;

import com.nexuswms.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(AppProperties appProperties) {
        this.secretKey = Keys.hmacShaKeyFor(
                appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
        this.expirationMs = appProperties.getJwt().getExpirationMs();
    }

    public String generateToken(UUID userId, String email, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * Calculates the remaining lifetime of a token from the current moment.
     * Used to set the Redis blocklist TTL on logout or user suspension,
     * ensuring the blocklist entry expires when the token would have expired.
     *
     * @param token The raw JWT string.
     * @return Duration representing the remaining validity window.
     */
    public Duration extractRemainingTtl(String token) {
        Date expiration = extractClaims(token).getExpiration();
        long remainingMs = expiration.getTime() - System.currentTimeMillis();
        return Duration.ofMillis(Math.max(remainingMs, 0));
    }
}