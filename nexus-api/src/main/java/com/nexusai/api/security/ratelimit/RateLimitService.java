package com.nexusai.api.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@Slf4j
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitResult checkRateLimit(String key, Supplier<BucketConfiguration> configSupplier) {
        Bucket bucket = buckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(configSupplier.get().getBandwidths()[0])
                        .build()
        );

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
        String key = endpoint + ":" + identifier;
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
        return checkRateLimit(key, configSupplier);
    }

    public record RateLimitResult(boolean allowed, long remainingTokens, long retryAfterSeconds) {}
}
