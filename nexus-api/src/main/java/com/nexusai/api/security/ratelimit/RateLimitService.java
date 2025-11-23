package com.nexusai.api.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final ProxyManager<String> proxyManager;
    private final Map<String, BucketConfiguration> configCache = new ConcurrentHashMap<>();

    private static final String KEY_PREFIX = "rate_limit:";

    public RateLimitResult checkRateLimit(String key, BucketConfiguration config) {
        String bucketKey = KEY_PREFIX + key;

        Bucket bucket = proxyManager.builder()
                .build(bucketKey, () -> config);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return new RateLimitResult(true, probe.getRemainingTokens(),
                    probe.getNanosToWaitForRefill() / 1_000_000_000);
        } else {
            log.warn("Rate limit exceeded for key: {}", key);
            return new RateLimitResult(false, 0,
                    probe.getNanosToWaitForRefill() / 1_000_000_000);
        }
    }

    public RateLimitResult checkApiRateLimit(UUID userId) {
        return checkRateLimit("api:" + userId, RateLimitConfig.getDefaultApiConfig());
    }

    public RateLimitResult checkAuthRateLimit(String ipAddress) {
        return checkRateLimit("auth:" + ipAddress, RateLimitConfig.getAuthConfig());
    }

    public RateLimitResult checkMessageRateLimit(UUID userId) {
        return checkRateLimit("message:" + userId, RateLimitConfig.getMessageConfig());
    }

    public RateLimitResult checkUploadRateLimit(UUID userId) {
        return checkRateLimit("upload:" + userId, RateLimitConfig.getUploadConfig());
    }

    public RateLimitResult checkPremiumRateLimit(UUID userId) {
        return checkRateLimit("premium:" + userId, RateLimitConfig.getPremiumConfig());
    }

    public RateLimitResult checkEndpointRateLimit(String endpoint, String identifier, int requestsPerMinute) {
        BucketConfiguration config = configCache.computeIfAbsent(
                endpoint + ":" + requestsPerMinute,
                k -> BucketConfiguration.builder()
                        .addLimit(io.github.bucket4j.Bandwidth.simple(requestsPerMinute,
                                java.time.Duration.ofMinutes(1)))
                        .build()
        );
        return checkRateLimit(endpoint + ":" + identifier, config);
    }

    public record RateLimitResult(boolean allowed, long remainingTokens, long retryAfterSeconds) {}
}
