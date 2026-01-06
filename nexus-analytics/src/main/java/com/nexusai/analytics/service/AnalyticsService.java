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

/**
 * Service de collecte et analyse d'événements analytics.
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
     * Track un événement de manière asynchrone.
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
     * Track un événement.
     */
    @Transactional
    public void trackEvent(EventDTO event) {
        log.debug("Tracking event: {} for user: {}", event.getEventType(), event.getUserId());

        try {
            // Créer l'événement
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

            // Sauvegarder en DB (asynchrone via Kafka)
            String eventJson = objectMapper.writeValueAsString(analyticsEvent);
            kafkaTemplate.send(KAFKA_TOPIC, event.getUserId().toString(), eventJson);

            // Mettre à jour les métriques temps réel en Redis
            updateRealtimeMetrics(event);

            log.debug("Event tracked successfully: {}", event.getEventType());

        } catch (Exception e) {
            log.error("Error tracking event", e);
        }
    }

    /**
     * Met à jour les métriques temps réel dans Redis.
     */
    private void updateRealtimeMetrics(EventDTO event) {
        String date = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).toString();
        
        // Incrémenter compteurs globaux
        redisTemplate.opsForValue().increment(REDIS_METRICS_PREFIX + "events:total:" + date);
        redisTemplate.opsForValue().increment(
                REDIS_METRICS_PREFIX + "events:" + event.getEventType() + ":" + date);
        
        // Incrémenter compteurs utilisateur
        redisTemplate.opsForValue().increment(
                REDIS_METRICS_PREFIX + "user:" + event.getUserId() + ":events:" + date);
        
        // Ajouter utilisateur actif du jour (Set)
        redisTemplate.opsForSet().add(
                REDIS_METRICS_PREFIX + "active_users:" + date, 
                event.getUserId().toString());
        
        // Expiration 30 jours
        redisTemplate.expire(REDIS_METRICS_PREFIX + "events:total:" + date, 30, TimeUnit.DAYS);
    }

    /**
     * Récupère les métriques d'un utilisateur.
     */
    public MetricsDTO getUserMetrics(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching metrics for user: {} from {} to {}", userId, startDate, endDate);

        List<AnalyticsEvent> events = eventRepository.findByUserIdAndTimestampBetween(
                userId, startDate, endDate);

        Map<String, Long> eventCounts = new HashMap<>();
        int totalMessages = 0;
        int totalTokensUsed = 0;

        for (AnalyticsEvent event : events) {
            // Compter par type d'événement
            eventCounts.merge(event.getEventType(), 1L, Long::sum);

            // Compter messages
            if (event.isMessageEvent()) {
                totalMessages++;
            }

            // Parser les données pour tokens
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
                .userId(userId)
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
     * Récupère les métriques globales de la plateforme.
     */
    public Map<String, Object> getPlatformMetrics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching platform metrics from {} to {}", startDate, endDate);

        Map<String, Object> metrics = new HashMap<>();

        // Total utilisateurs actifs
        long activeUsers = eventRepository.countDistinctUsersByTimestampBetween(startDate, endDate);
        metrics.put("active_users", activeUsers);

        // Total événements
        long totalEvents = eventRepository.countByTimestampBetween(startDate, endDate);
        metrics.put("total_events", totalEvents);

        // Événements par type
        Map<String, Long> eventsByType = eventRepository.countEventsByType(startDate, endDate);
        metrics.put("events_by_type", eventsByType);

        // Métriques temps réel depuis Redis
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
     * Récupère les top événements.
     */
    public List<Map<String, Object>> getTopEvents(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        return eventRepository.findTopEventTypes(startDate, endDate, limit);
    }

    /**
     * Récupère les utilisateurs les plus actifs.
     */
    public List<Map<String, Object>> getTopUsers(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        return eventRepository.findTopUsers(startDate, endDate, limit);
    }

    /**
     * Récupère les métriques de conversion.
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
     * Récupère les événements par heure (dernières 24h).
     */
    public List<Map<String, Object>> getEventsByHour() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(24);

        return eventRepository.countEventsByHour(startDate, endDate);
    }

    /**
     * Événements prédéfinis.
     */
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

    /**
     * Calcule la moyenne par jour.
     */
    private double calculateAvgPerDay(int total, LocalDateTime start, LocalDateTime end) {
        long days = ChronoUnit.DAYS.between(start, end);
        return days > 0 ? (double) total / days : total;
    }

    /**
     * Sérialise les données d'événement.
     */
    private String serializeEventData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Error serializing event data", e);
            return null;
        }
    }
}
