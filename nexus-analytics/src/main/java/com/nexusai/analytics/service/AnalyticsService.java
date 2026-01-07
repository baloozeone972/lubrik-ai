package com.nexusai.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.core.entity.AnalyticsEvent;
import com.nexusai.core.repository.AnalyticsEventRepository;
import com.nexusai.analytics.dto.EventDTO;
import com.nexusai.analytics.dto.MetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for analytics and metrics (platform-wide).
 * Uses core AnalyticsEvent entity (timestamp field).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsEventRepository eventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KAFKA_TOPIC = "analytics-events";
    private static final String REDIS_METRICS_PREFIX = "metrics:";

    /**
     * Track an event asynchronously.
     */
    @Async
    public void trackEventAsync(EventDTO event) {
        try {
            trackEvent(event);
        } catch (Exception e) {
            log.error("Error tracking event async", e);
        }
    }

    /**
     * Track an event.
     */
    @Transactional
    public void trackEvent(EventDTO event) {
        log.debug("Tracking event: {} for user: {}", event.getEventType(), event.getUserId());

        try {
            // Create event
            AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                    .userId(event.getUserId())
                    .sessionId(event.getSessionId())
                    .eventType(event.getEventType())
                    .eventCategory(event.getEventCategory())
                    .eventData(serializeEventData(event.getEventData()))
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .platform(event.getPlatform())
                    .deviceType(event.getDeviceType())
                    .build();

            // Save to DB (async via Kafka)
            String eventJson = objectMapper.writeValueAsString(analyticsEvent);
            kafkaTemplate.send(KAFKA_TOPIC, event.getUserId().toString(), eventJson);

            // Update real-time metrics in Redis
            updateRealtimeMetrics(event);

            log.debug("Event tracked successfully: {}", event.getEventType());

        } catch (Exception e) {
            log.error("Error tracking event", e);
        }
    }

    /**
     * Update real-time metrics in Redis.
     */
    private void updateRealtimeMetrics(EventDTO event) {
        String date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).toString();

        // Increment global counters
        redisTemplate.opsForValue().increment(REDIS_METRICS_PREFIX + "events:total:" + date);
        redisTemplate.opsForValue().increment(
                REDIS_METRICS_PREFIX + "events:" + event.getEventType() + ":" + date);

        // Increment user counters
        redisTemplate.opsForValue().increment(
                REDIS_METRICS_PREFIX + "user:" + event.getUserId() + ":events:" + date);

        // Add active user (Set)
        redisTemplate.opsForSet().add(
                REDIS_METRICS_PREFIX + "active_users:" + date,
                event.getUserId().toString());

        // Set expiration (30 days)
        redisTemplate.expire(REDIS_METRICS_PREFIX + "events:total:" + date, 30, TimeUnit.DAYS);
    }

    /**
     * Get user metrics.
     */
    public MetricsDTO getUserMetrics(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching metrics for user: {} from {} to {}", userId, startDate, endDate);

        List<AnalyticsEvent> events = eventRepository.findByUserIdAndTimestampBetween(
                userId, startDate, endDate);

        Map<String, Long> eventCounts = new HashMap<>();
        int totalMessages = 0;
        int totalTokensUsed = 0;

        for (AnalyticsEvent event : events) {
            // Count by event type
            eventCounts.merge(event.getEventType(), 1L, Long::sum);

            // Count messages
            if (event.isMessageEvent()) {
                totalMessages++;
            }

            // Parse data for tokens
            if ("message_sent".equals(event.getEventType()) && event.getEventData() != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(
                            event.getEventData(), Map.class);
                    if (data.containsKey("tokens_used")) {
                        totalTokensUsed += ((Number) data.get("tokens_used")).intValue();
                    }
                } catch (Exception e) {
                    log.warn("Error parsing event data", e);
                }
            }
        }

        return MetricsDTO.builder()
                // ← CORRECTION: Pas de userId() ici
                .startDate(startDate)
                .endDate(endDate)
                .totalEvents(events.size())
                .eventCounts(eventCounts)
                .totalMessages(totalMessages)
                .totalTokensUsed(totalTokensUsed)
                .avgMessagesPerDay(calculateAvgPerDay(totalMessages, startDate, endDate))
                .build();
    }

    /**
     * Get platform metrics.
     */
    public Map<String, Object> getPlatformMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching platform metrics from {} to {}", startDate, endDate);

        Map<String, Object> metrics = new HashMap<>();

        // Active users
        long activeUsers = eventRepository.countDistinctUsersByTimestampBetween(startDate, endDate);
        metrics.put("active_users", activeUsers);

        // Total events
        long totalEvents = eventRepository.countByTimestampBetween(startDate, endDate);
        metrics.put("total_events", totalEvents);

        // Events by type - CORRECTION: Conversion List<Map> → Map<String, Long>
        List<Map<String, Object>> eventTypesList = eventRepository.countEventsByType(startDate, endDate);
        Map<String, Long> eventsByType = eventTypesList.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("type"),
                        m -> ((Number) m.get("count")).longValue()
                ));
        metrics.put("events_by_type", eventsByType);

        // Real-time metrics from Redis
        String today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).toString();
        Long todayEvents = (Long) redisTemplate.opsForValue().get(
                REDIS_METRICS_PREFIX + "events:total:" + today);
        metrics.put("today_events", todayEvents != null ? todayEvents : 0);

        Long activeUsersToday = redisTemplate.opsForSet().size(
                REDIS_METRICS_PREFIX + "active_users:" + today);
        metrics.put("today_active_users", activeUsersToday != null ? activeUsersToday : 0);

        return metrics;
    }

    /**
     * Get top events.
     */
    public List<Map<String, Object>> getTopEvents(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        return eventRepository.findTopEventTypes(startDate, endDate, limit);
    }

    /**
     * Get top users.
     */
    public List<Map<String, Object>> getTopUsers(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        return eventRepository.findTopUsers(startDate, endDate, limit);
    }

    /**
     * Get conversion metrics.
     */
    public Map<String, Object> getConversionMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        long totalUsers = eventRepository.countDistinctUsersByTimestampBetween(startDate, endDate);

        long conversions = eventRepository.countByEventTypeAndTimestampBetween(
                "subscription_created", startDate, endDate);

        double conversionRate = totalUsers > 0 ? (double) conversions / totalUsers : 0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_users", totalUsers);
        metrics.put("conversions", conversions);
        metrics.put("conversion_rate", conversionRate);

        return metrics;
    }

    /**
     * Get events by hour (last 24h).
     */
    public List<Map<String, Object>> getEventsByHour() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(24);

        return eventRepository.countEventsByHour(startDate, endDate);
    }

    // ========== PREDEFINED EVENT TRACKING ==========

    public void trackUserLogin(UUID userId, String ipAddress, String userAgent) {
        trackEventAsync(EventDTO.builder()
                .userId(userId)
                .eventType("user_login")
                .eventCategory("auth")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build());
    }

    public void trackMessageSent(UUID userId, UUID conversationId, int tokensUsed) {
        Map<String, Object> data = new HashMap<>();
        data.put("conversation_id", conversationId.toString());
        data.put("tokens_used", tokensUsed);

        trackEventAsync(EventDTO.builder()
                .userId(userId)
                .eventType("message_sent")
                .eventCategory("conversation")
                .eventData(data)
                .build());
    }

    public void trackCompanionCreated(UUID userId, UUID companionId) {
        Map<String, Object> data = new HashMap<>();
        data.put("companion_id", companionId.toString());

        trackEventAsync(EventDTO.builder()
                .userId(userId)
                .eventType("companion_created")
                .eventCategory("companion")
                .eventData(data)
                .build());
    }

    public void trackSubscriptionCreated(UUID userId, String plan, double amount) {
        Map<String, Object> data = new HashMap<>();
        data.put("plan", plan);
        data.put("amount", amount);

        trackEventAsync(EventDTO.builder()
                .userId(userId)
                .eventType("subscription_created")
                .eventCategory("payment")
                .eventData(data)
                .build());
    }

    public void trackMediaUploaded(UUID userId, String mediaType, long fileSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("media_type", mediaType);
        data.put("file_size", fileSize);

        trackEventAsync(EventDTO.builder()
                .userId(userId)
                .eventType("media_uploaded")
                .eventCategory("media")
                .eventData(data)
                .build());
    }

    // ========== PRIVATE HELPER METHODS ==========

    private double calculateAvgPerDay(int total, LocalDateTime start, LocalDateTime end) {
        long days = ChronoUnit.DAYS.between(start, end);
        return days > 0 ? (double) total / days : total;
    }

    private String serializeEventData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Error serializing event data", e);
            return null;
        }
    }
}
