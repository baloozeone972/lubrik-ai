package com.nexusai.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.analytics.dto.AnalyticsEventDTO;
import com.nexusai.analytics.dto.TrackEventRequest;
import com.nexusai.analytics.entity.AnalyticsEvent;
import com.nexusai.analytics.repository.AnalyticsEventRepository;  // ← CORRECTION: Repository LOCAL
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for tracking and querying analytics events.
 * Uses local AnalyticsEvent entity (createdAt field).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final AnalyticsEventRepository eventRepository;  // Local repository
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String EVENT_CACHE_PREFIX = "analytics:event:";
    private static final String EVENT_COUNT_PREFIX = "analytics:count:";
    private static final String ANALYTICS_TOPIC = "analytics-events";

    /**
     * Track an event.
     */
    @Transactional
    public AnalyticsEventDTO trackEvent(UUID userId, TrackEventRequest request) {
        // Validate event
        validateEvent(request);

        // Create event entity
        AnalyticsEvent event = AnalyticsEvent.builder()
                .userId(userId)
                .sessionId(request.getSessionId())
                .eventType(request.getEventType())
                .eventCategory(request.getEventCategory())
                .eventAction(request.getEventAction())
                .eventLabel(request.getEventLabel())
                .eventValue(request.getEventValue())
                .properties(serializeProperties(request.getProperties()))
                .build();

        event = eventRepository.save(event);

        // Update real-time counters
        incrementEventCounter(request.getEventType());

        // Publish to Kafka for real-time processing
        publishEvent(event);

        log.debug("Tracked event {} for user {}", request.getEventType(), userId);
        return mapToDTO(event);
    }

    /**
     * Track event asynchronously.
     */
    public void trackEventAsync(UUID userId, String eventType, String category, Map<String, Object> properties) {
        TrackEventRequest request = TrackEventRequest.builder()
                .eventType(eventType)
                .eventCategory(category)
                .properties(properties)
                .build();

        try {
            String eventJson = objectMapper.writeValueAsString(Map.of(
                    "userId", userId.toString(),
                    "request", request,
                    "timestamp", LocalDateTime.now().toString()
            ));
            kafkaTemplate.send(ANALYTICS_TOPIC, eventJson);
        } catch (Exception e) {
            log.error("Failed to publish async event", e);
        }
    }

    /**
     * Get user events.
     */
    @Transactional(readOnly = true)
    public Page<AnalyticsEventDTO> getUserEvents(UUID userId, Pageable pageable) {
        return eventRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get events by type and date range.
     */
    @Transactional(readOnly = true)
    public Page<AnalyticsEventDTO> getEventsByType(String eventType, LocalDateTime from,
                                                   LocalDateTime to, Pageable pageable) {
        return eventRepository.findByEventTypeAndCreatedAtBetween(eventType, from, to, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Get event counts.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEventCounts(LocalDateTime from, LocalDateTime to) {
        return eventRepository.countEventsByType(from, to);
    }

    /**
     * Get realtime event count from Redis.
     */
    public long getRealtimeEventCount(String eventType) {
        String countKey = EVENT_COUNT_PREFIX + eventType + ":" + LocalDateTime.now().toLocalDate();
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateEvent(TrackEventRequest request) {
        if (request.getEventType() == null || request.getEventType().isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (request.getEventCategory() == null || request.getEventCategory().isBlank()) {
            throw new IllegalArgumentException("Event category is required");
        }
    }

    private String serializeProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (Exception e) {
            log.error("Failed to serialize properties", e);
            return null;
        }
    }

    private void incrementEventCounter(String eventType) {
        String date = LocalDateTime.now().toLocalDate().toString();
        String countKey = EVENT_COUNT_PREFIX + eventType + ":" + date;

        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.increment(countKey);
    }

    private void publishEvent(AnalyticsEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ANALYTICS_TOPIC, event.getUserId().toString(), eventJson);
        } catch (Exception e) {
            log.error("Failed to publish event to Kafka", e);
        }
    }

    private AnalyticsEventDTO mapToDTO(AnalyticsEvent event) {
        return AnalyticsEventDTO.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .eventType(event.getEventType())
                .eventCategory(event.getEventCategory())
                .eventAction(event.getEventAction())
                .eventLabel(event.getEventLabel())
                .eventValue(event.getEventValue())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
