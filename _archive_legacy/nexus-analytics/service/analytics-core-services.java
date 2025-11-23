package com.nexusai.analytics.core.service;

import com.nexusai.analytics.core.model.*;
import com.nexusai.analytics.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * ANALYTICS CORE - SERVICES MÉTIER
 * 
 * Services pour la gestion des événements, métriques et rapports.
 * Chaque service est indépendant et peut être modifié par une
 * équipe différente.
 * ═══════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════
// 1. EVENT SERVICE - Gestion des événements utilisateur
// ═══════════════════════════════════════════════════════════════

/**
 * Service de gestion des événements utilisateur.
 * 
 * Responsabilités :
 * - Enregistrement des événements
 * - Récupération et filtrage des événements
 * - Analyse des patterns d'utilisation
 * 
 * @author Équipe Analytics - Sous-équipe Events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {
    
    private final EventRepository eventRepository;
    
    /**
     * Enregistre un événement utilisateur.
     * 
     * @param event L'événement à enregistrer
     * @return L'événement enregistré avec son ID
     */
    @Transactional
    public UserEvent recordEvent(UserEvent event) {
        log.debug("Recording event: type={}, userId={}", 
            event.getEventType(), event.getUserId());
        
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
        
        log.info("Event recorded successfully: eventId={}", savedEvent.getEventId());
        
        return savedEvent;
    }
    
    /**
     * Enregistre plusieurs événements en batch.
     * Plus performant pour l'insertion en masse.
     * 
     * @param events Liste des événements à enregistrer
     * @return Nombre d'événements enregistrés
     */
    @Transactional
    public int recordEventsBatch(List<UserEvent> events) {
        log.debug("Recording batch of {} events", events.size());
        
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
        
        log.info("Batch recorded successfully: {} events", count);
        
        return count;
    }
    
    /**
     * Récupère les événements d'un utilisateur sur une période.
     * 
     * @param userId ID de l'utilisateur
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Liste des événements
     */
    public List<UserEvent> getUserEvents(UUID userId, Instant startTime, Instant endTime) {
        log.debug("Fetching events for user: userId={}, period={} to {}", 
            userId, startTime, endTime);
        
        return eventRepository.findByUserIdAndTimestampBetween(userId, startTime, endTime);
    }
    
    /**
     * Récupère les événements par type sur une période.
     * 
     * @param eventType Type d'événement
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Liste des événements
     */
    public List<UserEvent> getEventsByType(String eventType, Instant startTime, Instant endTime) {
        log.debug("Fetching events by type: type={}, period={} to {}", 
            eventType, startTime, endTime);
        
        return eventRepository.findByEventTypeAndTimestampBetween(eventType, startTime, endTime);
    }
    
    /**
     * Compte les événements d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Nombre d'événements
     */
    @Cacheable(value = "eventCounts", key = "#userId + '_' + #startTime + '_' + #endTime")
    public long countUserEvents(UUID userId, Instant startTime, Instant endTime) {
        return eventRepository.countByUserIdAndTimestampBetween(userId, startTime, endTime);
    }
    
    /**
     * Analyse les événements les plus fréquents sur une période.
     * 
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @param limit Nombre de résultats
     * @return Map des types d'événements et leur nombre d'occurrences
     */
    public Map<String, Long> getTopEvents(Instant startTime, Instant endTime, int limit) {
        log.debug("Analyzing top events: period={} to {}, limit={}", 
            startTime, endTime, limit);
        
        return eventRepository.findTopEventTypes(startTime, endTime, limit);
    }
    
    /**
     * Analyse les sessions utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Statistiques de session
     */
    public SessionStatistics getUserSessionStats(UUID userId, Instant startTime, Instant endTime) {
        List<UserEvent> events = getUserEvents(userId, startTime, endTime);
        
        // Grouper par session
        Map<UUID, List<UserEvent>> eventsBySession = events.stream()
            .collect(Collectors.groupingBy(UserEvent::getSessionId));
        
        // Calculer les statistiques
        int totalSessions = eventsBySession.size();
        double avgEventsPerSession = events.size() / (double) totalSessions;
        
        // Durée moyenne des sessions
        double avgSessionDuration = eventsBySession.values().stream()
            .mapToLong(this::calculateSessionDuration)
            .average()
            .orElse(0.0);
        
        return SessionStatistics.builder()
            .totalSessions(totalSessions)
            .totalEvents(events.size())
            .avgEventsPerSession(avgEventsPerSession)
            .avgSessionDurationSeconds(avgSessionDuration)
            .build();
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
    }
    
    private void enrichEventData(UserEvent event) {
        // Ajouter des métadonnées automatiques si absentes
        if (event.getEventData() == null) {
            event.setEventData(new HashMap<>());
        }
        
        // Ajouter timestamp si absent
        event.getEventData().putIfAbsent("recorded_at", Instant.now().toString());
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
}

// ═══════════════════════════════════════════════════════════════
// 2. METRIC SERVICE - Gestion des métriques système
// ═══════════════════════════════════════════════════════════════

/**
 * Service de gestion des métriques système.
 * 
 * Responsabilités :
 * - Enregistrement des métriques
 * - Agrégation des métriques
 * - Calcul de statistiques
 * 
 * @author Équipe Analytics - Sous-équipe Metrics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {
    
    private final MetricRepository metricRepository;
    
    /**
     * Enregistre une métrique système.
     * 
     * @param metric La métrique à enregistrer
     * @return La métrique enregistrée
     */
    public SystemMetric recordMetric(SystemMetric metric) {
        log.debug("Recording metric: name={}, value={}", 
            metric.getMetricName(), metric.getMetricValue());
        
        // Timestamp par défaut
        if (metric.getTimestamp() == null) {
            metric.setTimestamp(Instant.now());
        }
        
        return metricRepository.save(metric);
    }
    
    /**
     * Enregistre plusieurs métriques en batch.
     * 
     * @param metrics Liste des métriques
     * @return Nombre de métriques enregistrées
     */
    public int recordMetricsBatch(List<SystemMetric> metrics) {
        log.debug("Recording batch of {} metrics", metrics.size());
        
        metrics.forEach(metric -> {
            if (metric.getTimestamp() == null) {
                metric.setTimestamp(Instant.now());
            }
        });
        
        return metricRepository.saveBatch(metrics);
    }
    
    /**
     * Récupère les métriques d'un service sur une période.
     * 
     * @param serviceName Nom du service
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Liste des métriques
     */
    public List<SystemMetric> getServiceMetrics(String serviceName, 
                                                 Instant startTime, 
                                                 Instant endTime) {
        return metricRepository.findByServiceNameAndTimestampBetween(
            serviceName, startTime, endTime);
    }
    
    /**
     * Calcule les statistiques d'une métrique sur une période.
     * 
     * @param metricName Nom de la métrique
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Statistiques calculées
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
     * 
     * @param metricName Nom de la métrique
     * @param period Période d'agrégation
     * @param startTime Début
     * @param endTime Fin
     * @return Liste des métriques agrégées
     */
    public List<AggregatedMetric> aggregateMetrics(String metricName,
                                                    AggregationPeriod period,
                                                    Instant startTime,
                                                    Instant endTime) {
        log.debug("Aggregating metrics: name={}, period={}", metricName, period);
        
        return metricRepository.aggregateByPeriod(metricName, period, startTime, endTime);
    }
    
    /**
     * Vérifie si une métrique dépasse un seuil.
     * 
     * @param metricName Nom de la métrique
     * @param threshold Seuil
     * @param duration Durée pendant laquelle le seuil doit être dépassé
     * @return true si le seuil est dépassé
     */
    public boolean isMetricAboveThreshold(String metricName, 
                                          double threshold, 
                                          long duration) {
        Instant now = Instant.now();
        Instant start = now.minus(duration, ChronoUnit.MINUTES);
        
        MetricStatistics stats = getMetricStatistics(metricName, start, now);
        
        return stats.getAvgValue() > threshold;
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. AGGREGATION SERVICE - Agrégation des données
// ═══════════════════════════════════════════════════════════════

/**
 * Service d'agrégation des données analytics.
 * 
 * Responsabilités :
 * - Agrégation périodique des événements
 * - Agrégation périodique des métriques
 * - Pré-calcul des statistiques pour dashboards
 * 
 * @author Équipe Analytics - Sous-équipe Aggregation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {
    
    private final EventRepository eventRepository;
    private final MetricRepository metricRepository;
    private final AggregatedMetricRepository aggregatedMetricRepository;
    
    /**
     * Agrège les événements pour une période donnée.
     * Exécuté périodiquement par un scheduler.
     * 
     * @param period Période d'agrégation
     * @param startTime Début de la période
     * @param endTime Fin de la période
     */
    @Transactional
    public void aggregateEventsForPeriod(AggregationPeriod period, 
                                         Instant startTime, 
                                         Instant endTime) {
        log.info("Starting event aggregation: period={}, range={} to {}", 
            period, startTime, endTime);
        
        try {
            // Agrégation par type d'événement
            Map<String, Long> eventCounts = eventRepository
                .findTopEventTypes(startTime, endTime, Integer.MAX_VALUE);
            
            // Sauvegarder les métriques agrégées
            eventCounts.forEach((eventType, count) -> {
                AggregatedMetric metric = AggregatedMetric.builder()
                    .metricType("event_count_" + eventType)
                    .period(period)
                    .periodStart(startTime)
                    .periodEnd(endTime)
                    .count(count)
                    .tags(Map.of("event_type", eventType))
                    .build();
                
                aggregatedMetricRepository.save(metric);
            });
            
            log.info("Event aggregation completed: {} event types processed", 
                eventCounts.size());
            
        } catch (Exception e) {
            log.error("Error during event aggregation", e);
            throw new RuntimeException("Aggregation failed", e);
        }
    }
    
    /**
     * Agrège les métriques système pour une période donnée.
     * 
     * @param period Période d'agrégation
     * @param startTime Début de la période
     * @param endTime Fin de la période
     */
    @Transactional
    public void aggregateMetricsForPeriod(AggregationPeriod period, 
                                          Instant startTime, 
                                          Instant endTime) {
        log.info("Starting metric aggregation: period={}, range={} to {}", 
            period, startTime, endTime);
        
        try {
            // Liste des métriques à agréger
            List<String> metricNames = List.of(
                "http_requests_total",
                "http_request_duration_seconds",
                "cpu_usage",
                "memory_usage",
                "active_users"
            );
            
            // Agréger chaque métrique
            metricNames.forEach(metricName -> {
                MetricStatistics stats = metricRepository
                    .calculateStatistics(metricName, startTime, endTime);
                
                AggregatedMetric aggregated = AggregatedMetric.builder()
                    .metricType(metricName)
                    .period(period)
                    .periodStart(startTime)
                    .periodEnd(endTime)
                    .count(stats.getCount())
                    .minValue(stats.getMinValue())
                    .maxValue(stats.getMaxValue())
                    .avgValue(stats.getAvgValue())
                    .p95Value(stats.getP95Value())
                    .p99Value(stats.getP99Value())
                    .build();
                
                aggregatedMetricRepository.save(aggregated);
            });
            
            log.info("Metric aggregation completed: {} metrics processed", 
                metricNames.size());
            
        } catch (Exception e) {
            log.error("Error during metric aggregation", e);
            throw new RuntimeException("Aggregation failed", e);
        }
    }
    
    /**
     * Agrège les données de manière asynchrone.
     * 
     * @param period Période d'agrégation
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return CompletableFuture pour le suivi
     */
    public CompletableFuture<Void> aggregateAsync(AggregationPeriod period, 
                                                   Instant startTime, 
                                                   Instant endTime) {
        return CompletableFuture.runAsync(() -> {
            aggregateEventsForPeriod(period, startTime, endTime);
            aggregateMetricsForPeriod(period, startTime, endTime);
        });
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. DTOs & Classes Utilitaires
// ═══════════════════════════════════════════════════════════════

@Data
@Builder
class SessionStatistics {
    private int totalSessions;
    private int totalEvents;
    private double avgEventsPerSession;
    private double avgSessionDurationSeconds;
}

@Data
@Builder
class MetricStatistics {
    private long count;
    private double minValue;
    private double maxValue;
    private double avgValue;
    private double medianValue;
    private double p95Value;
    private double p99Value;
}
