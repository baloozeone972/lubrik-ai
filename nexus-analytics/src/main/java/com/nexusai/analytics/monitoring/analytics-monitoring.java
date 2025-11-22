package com.nexusai.analytics.monitoring;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ═══════════════════════════════════════════════════════════════
 * ANALYTICS MONITORING - PROMETHEUS & ALERTING
 * 
 * Système de monitoring avec Prometheus pour :
 * - Métriques custom (collecte, traitement, performance)
 * - Health checks
 * - Alerting automatique
 * - Dashboards temps réel
 * ═══════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════
// 1. CUSTOM METRICS - Métriques personnalisées
// ═══════════════════════════════════════════════════════════════

/**
 * Service de métriques personnalisées pour Prometheus.
 * 
 * Expose les métriques suivantes :
 * - nexusai_events_processed_total : Nombre total d'événements traités
 * - nexusai_events_failed_total : Nombre d'événements en erreur
 * - nexusai_events_buffer_size : Taille du buffer d'événements
 * - nexusai_metrics_processed_total : Nombre de métriques traitées
 * - nexusai_collection_duration_seconds : Durée de collecte
 * - nexusai_aggregation_duration_seconds : Durée d'agrégation
 * 
 * @author Équipe Analytics - Sous-équipe Monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsMetricsService implements MeterBinder {
    
    private final MeterRegistry meterRegistry;
    
    // Compteurs
    private Counter eventsProcessedCounter;
    private Counter eventsFailedCounter;
    private Counter metricsProcessedCounter;
    private Counter metricsFailedCounter;
    
    // Gauges
    private AtomicInteger eventsBufferSize = new AtomicInteger(0);
    private AtomicInteger metricsBufferSize = new AtomicInteger(0);
    private AtomicInteger activeCollectors = new AtomicInteger(0);
    
    // Timers
    private Timer collectionTimer;
    private Timer aggregationTimer;
    private Timer queryTimer;
    
    // Distribution summaries
    private DistributionSummary batchSizeDistribution;
    
    @Override
    public void bindTo(MeterRegistry registry) {
        // Compteurs
        eventsProcessedCounter = Counter.builder("nexusai.events.processed.total")
            .description("Total number of events processed")
            .tag("module", "analytics")
            .register(registry);
        
        eventsFailedCounter = Counter.builder("nexusai.events.failed.total")
            .description("Total number of failed events")
            .tag("module", "analytics")
            .register(registry);
        
        metricsProcessedCounter = Counter.builder("nexusai.metrics.processed.total")
            .description("Total number of metrics processed")
            .tag("module", "analytics")
            .register(registry);
        
        metricsFailedCounter = Counter.builder("nexusai.metrics.failed.total")
            .description("Total number of failed metrics")
            .tag("module", "analytics")
            .register(registry);
        
        // Gauges
        Gauge.builder("nexusai.events.buffer.size", eventsBufferSize, AtomicInteger::get)
            .description("Current size of events buffer")
            .register(registry);
        
        Gauge.builder("nexusai.metrics.buffer.size", metricsBufferSize, AtomicInteger::get)
            .description("Current size of metrics buffer")
            .register(registry);
        
        Gauge.builder("nexusai.collectors.active", activeCollectors, AtomicInteger::get)
            .description("Number of active collectors")
            .register(registry);
        
        // Timers
        collectionTimer = Timer.builder("nexusai.collection.duration")
            .description("Duration of data collection")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        aggregationTimer = Timer.builder("nexusai.aggregation.duration")
            .description("Duration of data aggregation")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        queryTimer = Timer.builder("nexusai.query.duration")
            .description("Duration of database queries")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        // Distribution Summary
        batchSizeDistribution = DistributionSummary.builder("nexusai.batch.size")
            .description("Size of batch inserts")
            .baseUnit("events")
            .register(registry);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES PUBLIQUES POUR ENREGISTRER LES MÉTRIQUES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Enregistre un événement traité.
     */
    public void recordEventProcessed() {
        eventsProcessedCounter.increment();
    }
    
    /**
     * Enregistre plusieurs événements traités.
     */
    public void recordEventsProcessed(int count) {
        eventsProcessedCounter.increment(count);
    }
    
    /**
     * Enregistre un événement en erreur.
     */
    public void recordEventFailed() {
        eventsFailedCounter.increment();
    }
    
    /**
     * Met à jour la taille du buffer d'événements.
     */
    public void updateEventsBufferSize(int size) {
        eventsBufferSize.set(size);
    }
    
    /**
     * Met à jour la taille du buffer de métriques.
     */
    public void updateMetricsBufferSize(int size) {
        metricsBufferSize.set(size);
    }
    
    /**
     * Enregistre la durée d'une collecte.
     */
    public void recordCollectionDuration(Duration duration) {
        collectionTimer.record(duration);
    }
    
    /**
     * Enregistre la durée d'une agrégation.
     */
    public void recordAggregationDuration(Duration duration) {
        aggregationTimer.record(duration);
    }
    
    /**
     * Enregistre la durée d'une requête.
     */
    public void recordQueryDuration(Duration duration) {
        queryTimer.record(duration);
    }
    
    /**
     * Enregistre la taille d'un batch.
     */
    public void recordBatchSize(int size) {
        batchSizeDistribution.record(size);
    }
    
    /**
     * Incrémente le nombre de collecteurs actifs.
     */
    public void incrementActiveCollectors() {
        activeCollectors.incrementAndGet();
    }
    
    /**
     * Décrémente le nombre de collecteurs actifs.
     */
    public void decrementActiveCollectors() {
        activeCollectors.decrementAndGet();
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. HEALTH INDICATORS - Indicateurs de santé
// ═══════════════════════════════════════════════════════════════

/**
 * Health indicator pour ClickHouse.
 * 
 * @author Équipe Analytics - Sous-équipe Monitoring
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickHouseHealthIndicator implements HealthIndicator {
    
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    @Override
    public Health health() {
        try {
            // Test de connexion simple
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // Vérifier l'espace disque
            Long diskSpace = jdbcTemplate.queryForObject(
                "SELECT sum(bytes_on_disk) FROM system.parts", 
                Long.class
            );
            
            return Health.up()
                .withDetail("database", "ClickHouse")
                .withDetail("disk_space_bytes", diskSpace)
                .build();
                
        } catch (Exception e) {
            log.error("ClickHouse health check failed", e);
            return Health.down()
                .withDetail("database", "ClickHouse")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

/**
 * Health indicator pour les buffers.
 * 
 * @author Équipe Analytics - Sous-équipe Monitoring
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BufferHealthIndicator implements HealthIndicator {
    
    private final com.nexusai.analytics.collector.listener.EventBuffer eventBuffer;
    private final com.nexusai.analytics.collector.listener.MetricBuffer metricBuffer;
    
    // Seuils d'alerte
    private static final int WARNING_THRESHOLD = 5000;
    private static final int CRITICAL_THRESHOLD = 8000;
    
    @Override
    public Health health() {
        int eventBufferSize = eventBuffer.size();
        int metricBufferSize = metricBuffer.size();
        
        int maxBufferSize = Math.max(eventBufferSize, metricBufferSize);
        
        Health.Builder builder = Health.up();
        
        if (maxBufferSize >= CRITICAL_THRESHOLD) {
            builder = Health.down();
        } else if (maxBufferSize >= WARNING_THRESHOLD) {
            builder = Health.status("WARNING");
        }
        
        return builder
            .withDetail("event_buffer_size", eventBufferSize)
            .withDetail("metric_buffer_size", metricBufferSize)
            .withDetail("warning_threshold", WARNING_THRESHOLD)
            .withDetail("critical_threshold", CRITICAL_THRESHOLD)
            .build();
    }
}

/**
 * Health indicator pour Kafka.
 * 
 * @author Équipe Analytics - Sous-équipe Monitoring
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {
    
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Health health() {
        try {
            // Vérifier la connectivité Kafka
            // Note: Cette méthode peut être bloquante
            var metrics = kafkaTemplate.metrics();
            
            boolean isHealthy = !metrics.isEmpty();
            
            if (isHealthy) {
                return Health.up()
                    .withDetail("kafka", "connected")
                    .withDetail("metrics_count", metrics.size())
                    .build();
            } else {
                return Health.down()
                    .withDetail("kafka", "no metrics available")
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return Health.down()
                .withDetail("kafka", "connection failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. ALERT SERVICE - Système d'alerting
// ═══════════════════════════════════════════════════════════════

/**
 * Service de gestion des alertes.
 * 
 * Surveille les métriques et déclenche des alertes quand
 * les seuils sont dépassés.
 * 
 * @author Équipe Analytics - Sous-équipe Monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    
    private final com.nexusai.analytics.core.model.Alert alertModel;
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;
    
    // Cache des alertes actives pour éviter les doublons
    private final Map<String, com.nexusai.analytics.core.model.Alert> activeAlerts = 
        new ConcurrentHashMap<>();
    
    /**
     * Vérifie les seuils et déclenche des alertes si nécessaire.
     * Exécuté toutes les minutes.
     */
    @Scheduled(fixedDelay = 60000) // 1 minute
    public void checkThresholds() {
        log.debug("Checking alert thresholds");
        
        // Vérifier le taux d'erreur
        checkErrorRate();
        
        // Vérifier la taille des buffers
        checkBufferSizes();
        
        // Vérifier la latence
        checkLatency();
        
        // Vérifier les services down
        checkServicesDown();
    }
    
    /**
     * Vérifie le taux d'erreur.
     */
    private void checkErrorRate() {
        // Récupérer les compteurs
        Counter eventsProcessed = meterRegistry.find("nexusai.events.processed.total")
            .counter();
        Counter eventsFailed = meterRegistry.find("nexusai.events.failed.total")
            .counter();
        
        if (eventsProcessed != null && eventsFailed != null) {
            double processed = eventsProcessed.count();
            double failed = eventsFailed.count();
            
            if (processed > 0) {
                double errorRate = (failed / processed) * 100;
                
                // Seuil: 5%
                if (errorRate > 5.0) {
                    triggerAlert(
                        "high_error_rate",
                        com.nexusai.analytics.core.model.AlertSeverity.CRITICAL,
                        String.format("High error rate detected: %.2f%%", errorRate),
                        "analytics-collector",
                        "error_rate",
                        errorRate,
                        5.0
                    );
                } else {
                    resolveAlert("high_error_rate");
                }
            }
        }
    }
    
    /**
     * Vérifie la taille des buffers.
     */
    private void checkBufferSizes() {
        Gauge eventsBufferGauge = meterRegistry.find("nexusai.events.buffer.size")
            .gauge();
        
        if (eventsBufferGauge != null) {
            double bufferSize = eventsBufferGauge.value();
            
            // Seuil critique: 8000
            if (bufferSize > 8000) {
                triggerAlert(
                    "buffer_overflow",
                    com.nexusai.analytics.core.model.AlertSeverity.CRITICAL,
                    String.format("Event buffer overflow: %d events", (int) bufferSize),
                    "analytics-collector",
                    "buffer_size",
                    bufferSize,
                    8000.0
                );
            } else if (bufferSize > 5000) {
                // Seuil warning: 5000
                triggerAlert(
                    "buffer_high",
                    com.nexusai.analytics.core.model.AlertSeverity.WARNING,
                    String.format("Event buffer high: %d events", (int) bufferSize),
                    "analytics-collector",
                    "buffer_size",
                    bufferSize,
                    5000.0
                );
            } else {
                resolveAlert("buffer_overflow");
                resolveAlert("buffer_high");
            }
        }
    }
    
    /**
     * Vérifie la latence P95.
     */
    private void checkLatency() {
        Timer collectionTimer = meterRegistry.find("nexusai.collection.duration")
            .timer();
        
        if (collectionTimer != null) {
            // P95 en secondes
            double p95 = collectionTimer.percentile(0.95);
            
            // Seuil: 2 secondes
            if (p95 > 2.0) {
                triggerAlert(
                    "high_latency",
                    com.nexusai.analytics.core.model.AlertSeverity.WARNING,
                    String.format("High collection latency: %.2fs (P95)", p95),
                    "analytics-collector",
                    "collection_duration_p95",
                    p95,
                    2.0
                );
            } else {
                resolveAlert("high_latency");
            }
        }
    }
    
    /**
     * Vérifie si des services sont down.
     */
    private void checkServicesDown() {
        Gauge activeCollectorsGauge = meterRegistry.find("nexusai.collectors.active")
            .gauge();
        
        if (activeCollectorsGauge != null) {
            double activeCollectors = activeCollectorsGauge.value();
            
            // On devrait avoir au moins 2 collecteurs actifs
            if (activeCollectors < 2) {
                triggerAlert(
                    "collectors_down",
                    com.nexusai.analytics.core.model.AlertSeverity.CRITICAL,
                    String.format("Low number of active collectors: %d", (int) activeCollectors),
                    "analytics-collector",
                    "active_collectors",
                    activeCollectors,
                    2.0
                );
            } else {
                resolveAlert("collectors_down");
            }
        }
    }
    
    /**
     * Déclenche une alerte.
     */
    private void triggerAlert(String alertName,
                               com.nexusai.analytics.core.model.AlertSeverity severity,
                               String message,
                               String serviceName,
                               String triggerMetric,
                               double triggerValue,
                               double threshold) {
        
        // Vérifier si l'alerte existe déjà
        if (activeAlerts.containsKey(alertName)) {
            return; // Alerte déjà active
        }
        
        log.warn("Triggering alert: {} - {}", alertName, message);
        
        com.nexusai.analytics.core.model.Alert alert = 
            com.nexusai.analytics.core.model.Alert.builder()
                .alertId(UUID.randomUUID())
                .alertName(alertName)
                .severity(severity)
                .message(message)
                .serviceName(serviceName)
                .triggerMetric(triggerMetric)
                .triggerValue(triggerValue)
                .threshold(threshold)
                .triggeredAt(Instant.now())
                .status(com.nexusai.analytics.core.model.AlertStatus.ACTIVE)
                .labels(new HashMap<>())
                .build();
        
        // Ajouter au cache
        activeAlerts.put(alertName, alert);
        
        // Envoyer notification
        notificationService.sendAlert(alert);
    }
    
    /**
     * Résout une alerte.
     */
    private void resolveAlert(String alertName) {
        com.nexusai.analytics.core.model.Alert alert = activeAlerts.remove(alertName);
        
        if (alert != null) {
            log.info("Resolving alert: {}", alertName);
            
            alert.setResolvedAt(Instant.now());
            alert.setStatus(com.nexusai.analytics.core.model.AlertStatus.RESOLVED);
            
            notificationService.sendAlertResolved(alert);
        }
    }
    
    /**
     * Retourne les alertes actives.
     */
    public List<com.nexusai.analytics.core.model.Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. NOTIFICATION SERVICE - Envoi de notifications
// ═══════════════════════════════════════════════════════════════

/**
 * Service d'envoi de notifications pour les alertes.
 * 
 * Supporte plusieurs canaux :
 * - Email
 * - Slack
 * - PagerDuty
 * - Webhooks
 * 
 * @author Équipe Analytics - Sous-équipe Monitoring
 */
@Slf4j
@Service
public class NotificationService {
    
    /**
     * Envoie une notification d'alerte.
     */
    public void sendAlert(com.nexusai.analytics.core.model.Alert alert) {
        log.info("Sending alert notification: {}", alert.getAlertName());
        
        // TODO: Implémenter l'envoi vers Slack, Email, etc.
        
        // Exemple de message Slack
        String slackMessage = formatSlackMessage(alert);
        log.info("Slack message: {}", slackMessage);
        
        // TODO: Appeler l'API Slack
    }
    
    /**
     * Envoie une notification de résolution d'alerte.
     */
    public void sendAlertResolved(com.nexusai.analytics.core.model.Alert alert) {
        log.info("Sending alert resolved notification: {}", alert.getAlertName());
        
        // TODO: Implémenter l'envoi
    }
    
    /**
     * Formate un message pour Slack.
     */
    private String formatSlackMessage(com.nexusai.analytics.core.model.Alert alert) {
        return String.format(
            "🚨 *%s Alert*\n" +
            "*Service:* %s\n" +
            "*Message:* %s\n" +
            "*Metric:* %s\n" +
            "*Value:* %.2f (threshold: %.2f)\n" +
            "*Time:* %s",
            alert.getSeverity(),
            alert.getServiceName(),
            alert.getMessage(),
            alert.getTriggerMetric(),
            alert.getTriggerValue(),
            alert.getThreshold(),
            alert.getTriggeredAt()
        );
    }
}
