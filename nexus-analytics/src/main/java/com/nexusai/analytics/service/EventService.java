package com.nexusai.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.analytics.dto.AnalyticsEventDTO;
import com.nexusai.analytics.dto.TrackEventRequest;
import com.nexusai.analytics.entity.AnalyticsEvent;
import com.nexusai.core.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final AnalyticsEventRepository eventRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String EVENT_CACHE_PREFIX = "analytics:event:";
    private static final String EVENT_COUNT_PREFIX = "analytics:count:";
    private static final String ANALYTICS_TOPIC = "analytics-events";

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

    @Transactional(readOnly = true)
    public Page<AnalyticsEventDTO> getUserEvents(UUID userId, Pageable pageable) {
        return eventRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public Page<AnalyticsEventDTO> getEventsByType(String eventType, LocalDateTime from,
                                                    LocalDateTime to, Pageable pageable) {
        return eventRepository.findByEventTypeAndCreatedAtBetween(eventType, from, to, pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEventCounts(LocalDateTime from, LocalDateTime to) {
        return eventRepository.countEventsByType(from, to);
    }

    public long getRealtimeEventCount(String eventType) {
        String countKey = EVENT_COUNT_PREFIX + eventType + ":" + LocalDateTime.now().toLocalDate();
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0;
    }

    private void validateEvent(TrackEventRequest request) {
        // Validate event type
        if (request.getEventType() == null || request.getEventType().isBlank()) {
            throw new IllegalArgumentException("Event type is required");
        }

        // Validate category
        if (request.getEventCategory() == null || request.getEventCategory().isBlank()) {
            throw new IllegalArgumentException("Event category is required");
        }

        // Sanitize event type
        if (!request.getEventType().matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid event type format");
        }
    }

    private void incrementEventCounter(String eventType) {
        String countKey = EVENT_COUNT_PREFIX + eventType + ":" + LocalDateTime.now().toLocalDate();
        redisTemplate.opsForValue().increment(countKey);
        redisTemplate.expire(countKey, Duration.ofDays(7));
    }

    private void publishEvent(AnalyticsEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(mapToDTO(event));
            kafkaTemplate.send(ANALYTICS_TOPIC, event.getEventType(), eventJson);
        } catch (Exception e) {
            log.error("Failed to publish event to Kafka", e);
        }
    }

    private String serializeProperties(Map<String, Object> properties) {
        if (properties == null) return null;
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (Exception e) {
            log.error("Failed to serialize event properties", e);
            return null;
        }
    }

    private AnalyticsEventDTO mapToDTO(AnalyticsEvent event) {
        Map<String, Object> properties = null;
        if (event.getProperties() != null) {
            try {
                properties = objectMapper.readValue(event.getProperties(), Map.class);
            } catch (Exception ignored) {}
        }

        return AnalyticsEventDTO.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .eventType(event.getEventType())
                .eventCategory(event.getEventCategory())
                .eventAction(event.getEventAction())
                .eventLabel(event.getEventLabel())
                .eventValue(event.getEventValue())
                .properties(properties)
                .createdAt(event.getCreatedAt())
                .build();
    }
}
