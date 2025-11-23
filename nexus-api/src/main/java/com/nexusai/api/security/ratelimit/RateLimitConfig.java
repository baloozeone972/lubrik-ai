package com.nexusai.api.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public ProxyManager<String> proxyManager() {
        RedisClient redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        return LettuceBasedProxyManager.builderFor(connection)
                .build();
    }

    @Bean
    public RateLimitService rateLimitService(ProxyManager<String> proxyManager) {
        return new RateLimitService(proxyManager);
    }

    // Default rate limit configurations
    public static BucketConfiguration getDefaultApiConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(1000, Duration.ofHours(1)))
                .build();
    }

    public static BucketConfiguration getAuthConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(20, Duration.ofHours(1)))
                .build();
    }

    public static BucketConfiguration getMessageConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(30, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(500, Duration.ofHours(1)))
                .build();
    }

    public static BucketConfiguration getUploadConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(100, Duration.ofHours(1)))
                .build();
    }

    public static BucketConfiguration getPremiumConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(300, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(5000, Duration.ofHours(1)))
                .build();
    }
}
