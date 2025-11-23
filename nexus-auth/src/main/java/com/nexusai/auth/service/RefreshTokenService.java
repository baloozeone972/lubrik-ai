package com.nexusai.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    private final StringRedisTemplate redisTemplate;

    @Value("${nexusai.security.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    public void storeRefreshToken(UUID userId, String token) {
        String tokenHash = hashToken(token);
        String tokenKey = REFRESH_TOKEN_PREFIX + tokenHash;
        String userKey = USER_TOKENS_PREFIX + userId;

        Duration expiration = Duration.ofMillis(refreshTokenExpiration);

        // Store token -> userId mapping
        redisTemplate.opsForValue().set(tokenKey, userId.toString(), expiration);

        // Add to user's token set
        redisTemplate.opsForSet().add(userKey, tokenHash);
        redisTemplate.expire(userKey, expiration);

        log.debug("Stored refresh token for user: {}", userId);
    }

    public boolean isValidRefreshToken(UUID userId, String token) {
        String tokenHash = hashToken(token);
        String tokenKey = REFRESH_TOKEN_PREFIX + tokenHash;

        String storedUserId = redisTemplate.opsForValue().get(tokenKey);
        return userId.toString().equals(storedUserId);
    }

    public void revokeRefreshToken(UUID userId, String token) {
        String tokenHash = hashToken(token);
        String tokenKey = REFRESH_TOKEN_PREFIX + tokenHash;
        String userKey = USER_TOKENS_PREFIX + userId;

        redisTemplate.delete(tokenKey);
        redisTemplate.opsForSet().remove(userKey, tokenHash);

        log.debug("Revoked refresh token for user: {}", userId);
    }

    public void revokeAllTokens(UUID userId) {
        String userKey = USER_TOKENS_PREFIX + userId;

        Set<String> tokenHashes = redisTemplate.opsForSet().members(userKey);
        if (tokenHashes != null) {
            for (String tokenHash : tokenHashes) {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + tokenHash);
            }
        }
        redisTemplate.delete(userKey);

        log.info("Revoked all refresh tokens for user: {}", userId);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
