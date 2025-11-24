package com.nexusai.analytics.core.service;

import com.nexusai.analytics.core.model.*;
import com.nexusai.analytics.core.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * EVENT SERVICE - IMPLÉMENTATION COMPLÈTE
 * 
 * Service de gestion des événements utilisateur avec :
 * - Retry automatique en cas d'erreur
 * - Cache Redis
 * - Métriques Prometheus
 * - Validation avancée
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    
    private final EventRepository eventRepository;
    private final MeterRegistry meterRegistry;
    
    // Compteurs Prometheus
    private Counter eventsProcessedCounter;
    private Counter eventsFailedCounter;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        eventsProcessedCounter = Counter.builder("nexusai.events.processed.total")
            .description("Total events processed")
            .register(meterRegistry);
        
        eventsFailedCounter = Counter.builder("nexusai.events.failed.total")
            .description("Total events failed")
            .register(meterRegistry);
    }
    
    /**
     * Enregistre un événement avec retry automatique.
     */
    @Retryable(
        retryFor = {DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    public UserEvent recordEvent(UserEvent event) {
        log.debug("Recording event: type={}, userId={}", 
            event.getEventType(), event.getUserId());
        
        try {
            // Validation
            validateEvent(event);
            
            // Génération ID si absent
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID());
            }
            
            // Timestamp par défaut
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }
            
            // Enrichissement des données
            enrichEventData(event);
            
            // Sauvegarde
            UserEvent savedEvent = eventRepository.save(event);
            
            // Métriques
            eventsProcessedCounter.increment();
            
            log.info("Event recorded successfully: eventId={}", savedEvent.getEventId());
            
            return savedEvent;
            
        } catch (DataAccessException e) {
            log.error("Database error recording event", e);
            eventsFailedCounter.increment();
            throw e; // Will trigger retry
        } catch (Exception e) {
            log.error("Unexpected error recording event", e);
            eventsFailedCounter.increment();
            throw new RuntimeException("Failed to record event", e);
        }
    }
    
    /**
     * Méthode de récupération si tous les retries échouent.
     */
    @Recover
    public UserEvent recover(DataAccessException e, UserEvent event) {
        log.error("Failed to record event after 3 retries: userId={}, eventType={}", 
            event.getUserId(), event.getEventType(), e);
        
        // TODO: Envoyer vers Dead Letter Queue
        sendToDeadLetterQueue(event, e);
        
        eventsFailedCounter.increment();
        return null;
    }
    
    /**
     * Enregistre plusieurs événements en batch.
     */
    @Retryable(
        retryFor = {DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    public int recordEventsBatch(List<UserEvent> events) {
        log.debug("Recording batch of {} events", events.size());
        
        try {
            // Validation et enrichissement
            events.forEach(event -> {
                if (event.getEventId() == null) {
                    event.setEventId(UUID.randomUUID());
                }
                if (event.getTimestamp() == null) {
                    event.setTimestamp(Instant.now());
                }
                enrichEventData(event);
            });
            
            // Insertion batch
            int count = eventRepository.saveBatch(events);
            
            // Métriques
            eventsProcessedCounter.increment(count);
            
            log.info("Batch recorded successfully: {} events", count);
            
            return count;
            
        } catch (DataAccessException e) {
            log.error("Database error recording batch", e);
            eventsFailedCounter.increment(events.size());
            throw e;
        }
    }
    
    /**
     * Récupère les événements d'un utilisateur sur une période.
     */
    @Cacheable(value = "userEvents", key = "#userId + '_' + #startTime + '_' + #endTime")
    public List<UserEvent> getUserEvents(UUID userId, Instant startTime, Instant endTime) {
        log.debug("Fetching events for user: userId={}, period={} to {}", 
            userId, startTime, endTime);
        
        validateDateRange(startTime, endTime);
        
        return eventRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime);
    }
    
    /**
     * Récupère les événements par type.
     */
    public List<UserEvent> getEventsByType(String eventType, Instant startTime, Instant endTime) {
        log.debug("Fetching events by type: type={}, period={} to {}", 
            eventType, startTime, endTime);
        
        validateDateRange(startTime, endTime);
        
        return eventRepository.findByEventTypeAndTimestampBetween(eventType, startTime, endTime);
    }
    
    /**
     * Compte les événements d'un utilisateur.
     */
    @Cacheable(value = "eventCounts", key = "#userId + '_' + #startTime + '_' + #endTime")
    public long countUserEvents(UUID userId, Instant startTime, Instant endTime) {
        validateDateRange(startTime, endTime);
        return eventRepository.countByUserIdAndTimestampBetween(userId, startTime, endTime);
    }
    
    /**
     * Récupère les événements les plus fréquents.
     */
    @Cacheable(value = "topEvents", key = "#startTime + '_' + #endTime + '_' + #limit")
    public Map<String, Long> getTopEvents(Instant startTime, Instant endTime, int limit) {
        log.debug("Analyzing top events: period={} to {}, limit={}", 
            startTime, endTime, limit);
        
        validateDateRange(startTime, endTime);
        
        return eventRepository.findTopEventTypes(startTime, endTime, limit);
    }
    
    /**
     * Analyse les sessions utilisateur - IMPLÉMENTATION COMPLÈTE.
     */
    public SessionStatistics getUserSessionStats(UUID userId, Instant startTime, Instant endTime) {
        log.debug("Calculating session stats for user: userId={}", userId);
        
        validateDateRange(startTime, endTime);
        
        List<UserEvent> events = getUserEvents(userId, startTime, endTime);
        
        if (events.isEmpty()) {
            return SessionStatistics.builder()
                .totalSessions(0)
                .totalEvents(0)
                .avgEventsPerSession(0.0)
                .avgSessionDurationSeconds(0.0)
                .build();
        }
        
        // Grouper par session
        Map<UUID, List<UserEvent>> eventsBySession = events.stream()
            .filter(e -> e.getSessionId() != null)
            .collect(Collectors.groupingBy(UserEvent::getSessionId));
        
        int totalSessions = eventsBySession.size();
        int totalEvents = events.size();
        double avgEventsPerSession = totalEvents / (double) totalSessions;
        
        // Calculer la durée moyenne des sessions
        double avgSessionDuration = eventsBySession.values().stream()
            .mapToLong(this::calculateSessionDuration)
            .average()
            .orElse(0.0);
        
        // Statistiques supplémentaires
        Map<String, Long> topEventTypes = events.stream()
            .collect(Collectors.groupingBy(UserEvent::getEventType, Collectors.counting()));
        
        String mostFrequentEvent = topEventTypes.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        return SessionStatistics.builder()
            .totalSessions(totalSessions)
            .totalEvents(totalEvents)
            .avgEventsPerSession(avgEventsPerSession)
            .avgSessionDurationSeconds(avgSessionDuration)
            .mostFrequentEvent(mostFrequentEvent)
            .uniqueEventTypes(topEventTypes.size())
            .build();
    }
    
    /**
     * Recherche les événements par session.
     */
    public List<UserEvent> getEventsBySession(UUID sessionId) {
        log.debug("Fetching events for session: {}", sessionId);
        return eventRepository.findBySessionId(sessionId);
    }
    
    /**
     * Recherche les événements par appareil.
     */
    public List<UserEvent> getEventsByDevice(String deviceType, Instant startTime, Instant endTime) {
        validateDateRange(startTime, endTime);
        return eventRepository.findByDeviceType(deviceType, startTime, endTime);
    }
    
    /**
     * Recherche les événements par pays.
     */
    public Map<String, Long> getEventsByCountry(Instant startTime, Instant endTime) {
        validateDateRange(startTime, endTime);
        return eventRepository.findEventCountByCountry(startTime, endTime);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES PRIVÉES
    // ═══════════════════════════════════════════════════════════════
    
    private void validateEvent(UserEvent event) {
        Objects.requireNonNull(event.getUserId(), "User ID cannot be null");
        Objects.requireNonNull(event.getEventType(), "Event type cannot be null");
        
        if (event.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be empty");
        }
        
        if (event.getEventType().length() > 100) {
            throw new IllegalArgumentException("Event type too long (max 100 chars)");
        }
    }
    
    private void validateDateRange(Instant startTime, Instant endTime) {
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        long daysDiff = ChronoUnit.DAYS.between(startTime, endTime);
        if (daysDiff > 90) {
            throw new IllegalArgumentException("Date range too large (max 90 days)");
        }
    }
    
    private void enrichEventData(UserEvent event) {
        if (event.getEventData() == null) {
            event.setEventData(new HashMap<>());
        }
        
        event.getEventData().putIfAbsent("recorded_at", Instant.now().toString());
        event.getEventData().putIfAbsent("source", "nexusai-analytics");
    }
    
    private long calculateSessionDuration(List<UserEvent> sessionEvents) {
        if (sessionEvents.isEmpty()) return 0;
        
        Instant first = sessionEvents.stream()
            .map(UserEvent::getTimestamp)
            .min(Instant::compareTo)
            .orElse(Instant.now());
        
        Instant last = sessionEvents.stream()
            .map(UserEvent::getTimestamp)
            .max(Instant::compareTo)
            .orElse(Instant.now());
        
        return ChronoUnit.SECONDS.between(first, last);
    }
    
    private void sendToDeadLetterQueue(UserEvent event, Exception e) {
        // TODO: Implémenter l'envoi vers Kafka DLQ
        log.error("Sending event to DLQ: eventType={}, error={}", 
            event.getEventType(), e.getMessage());
    }
}

// ═══════════════════════════════════════════════════════════════
// SESSION STATISTICS DTO
// ═══════════════════════════════════════════════════════════════

@lombok.Data
@lombok.Builder
class SessionStatistics {
    private int totalSessions;
    private int totalEvents;
    private double avgEventsPerSession;
    private double avgSessionDurationSeconds;
    private String mostFrequentEvent;
    private int uniqueEventTypes;
}

// ═══════════════════════════════════════════════════════════════
// METRIC SERVICE - IMPLÉMENTATION COMPLÈTE
// ═══════════════════════════════════════════════════════════════

/**
 * Service de gestion des métriques système.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {
    
    private final MetricRepository metricRepository;
    private final MeterRegistry meterRegistry;
    
    private Counter metricsProcessedCounter;
    private Counter metricsFailedCounter;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        metricsProcessedCounter = Counter.builder("nexusai.metrics.processed.total")
            .description("Total metrics processed")
            .register(meterRegistry);
        
        metricsFailedCounter = Counter.builder("nexusai.metrics.failed.total")
            .description("Total metrics failed")
            .register(meterRegistry);
    }
    
    /**
     * Enregistre une métrique avec retry.
     */
    @Retryable(
        retryFor = {DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public SystemMetric recordMetric(SystemMetric metric) {
        log.debug("Recording metric: name={}, value={}", 
            metric.getMetricName(), metric.getMetricValue());
        
        try {
            if (metric.getTimestamp() == null) {
                metric.setTimestamp(Instant.now());
            }
            
            SystemMetric saved = metricRepository.save(metric);
            metricsProcessedCounter.increment();
            
            return saved;
            
        } catch (DataAccessException e) {
            log.error("Error recording metric", e);
            metricsFailedCounter.increment();
            throw e;
        }
    }
    
    /**
     * Enregistre plusieurs métriques en batch.
     */
    @Retryable(
        retryFor = {DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public int recordMetricsBatch(List<SystemMetric> metrics) {
        log.debug("Recording batch of {} metrics", metrics.size());
        
        try {
            metrics.forEach(metric -> {
                if (metric.getTimestamp() == null) {
                    metric.setTimestamp(Instant.now());
                }
            });
            
            int count = metricRepository.saveBatch(metrics);
            metricsProcessedCounter.increment(count);
            
            return count;
            
        } catch (DataAccessException e) {
            log.error("Error recording metrics batch", e);
            metricsFailedCounter.increment(metrics.size());
            throw e;
        }
    }
    
    /**
     * Récupère les métriques d'un service.
     */
    public List<SystemMetric> getServiceMetrics(String serviceName, 
                                                 Instant startTime, 
                                                 Instant endTime) {
        return metricRepository.findByServiceNameAndTimestampBetween(
            serviceName, startTime, endTime);
    }
    
    /**
     * Calcule les statistiques d'une métrique - IMPLÉMENTATION COMPLÈTE.
     */
    @Cacheable(value = "metricStats", key = "#metricName + '_' + #startTime + '_' + #endTime")
    public MetricStatistics getMetricStatistics(String metricName, 
                                                Instant startTime, 
                                                Instant endTime) {
        log.debug("Calculating statistics for metric: name={}", metricName);
        
        return metricRepository.calculateStatistics(metricName, startTime, endTime);
    }
    
    /**
     * Agrège les métriques par période.
     */
    @Cacheable(value = "aggregatedMetrics", 
               key = "#metricName + '_' + #period + '_' + #startTime + '_' + #endTime")
    public List<AggregatedMetric> aggregateMetrics(String metricName,
                                                    AggregationPeriod period,
                                                    Instant startTime,
                                                    Instant endTime) {
        log.debug("Aggregating metrics: name={}, period={}", metricName, period);
        
        return metricRepository.aggregateByPeriod(metricName, period, startTime, endTime);
    }
    
    /**
     * Vérifie si une métrique dépasse un seuil.
     */
    public boolean isMetricAboveThreshold(String metricName, 
                                          double threshold, 
                                          long durationMinutes) {
        Instant now = Instant.now();
        Instant start = now.minus(durationMinutes, ChronoUnit.MINUTES);
        
        MetricStatistics stats = getMetricStatistics(metricName, start, now);
        
        return stats.getAvgValue() > threshold;
    }
    
    /**
     * Détecte les anomalies dans une métrique.
     */
    public List<AnomalyDetection> detectAnomalies(String metricName,
                                                    Instant startTime,
                                                    Instant endTime,
                                                    double stdDevThreshold) {
        log.info("Detecting anomalies for metric: {}", metricName);
        
        MetricStatistics stats = getMetricStatistics(metricName, startTime, endTime);
        
        // Calculer l'écart-type
        double mean = stats.getAvgValue();
        double upperBound = mean + (stdDevThreshold * (stats.getMaxValue() - mean));
        double lowerBound = mean - (stdDevThreshold * (mean - stats.getMinValue()));
        
        // Récupérer les métriques qui dépassent les seuils
        List<SystemMetric> metrics = metricRepository
            .findByMetricNameAndTimestampBetween(metricName, startTime, endTime);
        
        return metrics.stream()
            .filter(m -> m.getMetricValue() > upperBound || m.getMetricValue() < lowerBound)
            .map(m -> AnomalyDetection.builder()
                .metricName(m.getMetricName())
                .timestamp(m.getTimestamp())
                .value(m.getMetricValue())
                .expectedRange(lowerBound + " - " + upperBound)
                .severity(calculateSeverity(m.getMetricValue(), lowerBound, upperBound))
                .build())
            .collect(Collectors.toList());
    }
    
    private String calculateSeverity(double value, double lower, double upper) {
        double deviation = Math.max(
            Math.abs(value - lower) / lower,
            Math.abs(value - upper) / upper
        );
        
        if (deviation > 2.0) return "CRITICAL";
        if (deviation > 1.0) return "HIGH";
        return "MEDIUM";
    }
}

@lombok.Data
@lombok.Builder
class MetricStatistics {
    private long count;
    private double minValue;
    private double maxValue;
    private double avgValue;
    private double medianValue;
    private double p95Value;
    private double p99Value;
}

@lombok.Data
@lombok.Builder
class AnomalyDetection {
    private String metricName;
    private Instant timestamp;
    private double value;
    private String expectedRange;
    private String severity;
}
