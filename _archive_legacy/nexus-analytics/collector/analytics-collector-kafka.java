package com.nexusai.analytics.collector.listener;

import com.nexusai.analytics.core.model.*;
import com.nexusai.analytics.core.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ═══════════════════════════════════════════════════════════════
 * ANALYTICS COLLECTOR - KAFKA LISTENERS
 * 
 * Listeners Kafka pour collecter les événements et métriques
 * de tous les modules du système et les persister dans ClickHouse.
 * 
 * Architecture :
 * - Consommation asynchrone des messages Kafka
 * - Buffering pour insertion batch optimisée
 * - Gestion des erreurs et retry
 * - Monitoring des performances de collecte
 * ═══════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════
// 1. EVENT LISTENER - Collecte des événements utilisateur
// ═══════════════════════════════════════════════════════════════

/**
 * Listener Kafka pour les événements utilisateur.
 * 
 * Topics écoutés :
 * - user.events (événements généraux)
 * - user.registered, user.deleted (événements utilisateur)
 * - message.sent, image.generated (événements métier)
 * 
 * @author Équipe Analytics - Sous-équipe Collector
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCollectorListener {
    
    private final EventService eventService;
    private final EventBuffer eventBuffer;
    
    // Compteurs pour monitoring
    private final AtomicInteger processedEvents = new AtomicInteger(0);
    private final AtomicInteger failedEvents = new AtomicInteger(0);
    
    /**
     * Écoute le topic principal des événements utilisateur.
     * 
     * Configuration :
     * - Consommation en batch (max 100 messages)
     * - Commit manuel après traitement réussi
     * - Retry automatique en cas d'erreur
     */
    @KafkaListener(
        topics = "user.events",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "eventKafkaListenerContainerFactory"
    )
    public void consumeUserEvents(
            @Payload List<EventMessage> messages,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.debug("Received {} events from partition {} at offset {}", 
            messages.size(), partition, offset);
        
        try {
            // Convertir les messages en entités UserEvent
            List<UserEvent> events = messages.stream()
                .map(this::convertToUserEvent)
                .toList();
            
            // Ajouter au buffer pour insertion batch
            eventBuffer.addAll(events);
            
            // Mise à jour compteurs
            processedEvents.addAndGet(events.size());
            
            // Commit manuel
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
            log.debug("Successfully processed {} events", events.size());
            
        } catch (Exception e) {
            log.error("Error processing events batch", e);
            failedEvents.addAndGet(messages.size());
            
            // Ne pas commit en cas d'erreur pour retry
            throw new RuntimeException("Event processing failed", e);
        }
    }
    
    /**
     * Écoute les événements d'inscription utilisateur.
     */
    @KafkaListener(
        topics = "user.registered",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeUserRegistered(@Payload EventMessage message) {
        log.info("User registered event: userId={}", message.getUserId());
        
        UserEvent event = convertToUserEvent(message);
        eventService.recordEvent(event);
    }
    
    /**
     * Écoute les événements de suppression utilisateur.
     */
    @KafkaListener(
        topics = "user.deleted",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeUserDeleted(@Payload EventMessage message) {
        log.info("User deleted event: userId={}", message.getUserId());
        
        UserEvent event = convertToUserEvent(message);
        eventService.recordEvent(event);
    }
    
    /**
     * Écoute les événements de messages envoyés.
     */
    @KafkaListener(
        topics = "message.sent",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeMessageSent(@Payload EventMessage message) {
        log.debug("Message sent event: userId={}", message.getUserId());
        
        UserEvent event = convertToUserEvent(message);
        eventBuffer.add(event);
    }
    
    /**
     * Écoute les événements de génération d'images.
     */
    @KafkaListener(
        topics = "image.generated",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeImageGenerated(@Payload EventMessage message) {
        log.debug("Image generated event: userId={}", message.getUserId());
        
        UserEvent event = convertToUserEvent(message);
        eventBuffer.add(event);
    }
    
    /**
     * Écoute les événements de création d'abonnement.
     */
    @KafkaListener(
        topics = "subscription.created",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeSubscriptionCreated(@Payload EventMessage message) {
        log.info("Subscription created event: userId={}", message.getUserId());
        
        UserEvent event = convertToUserEvent(message);
        eventService.recordEvent(event);
    }
    
    /**
     * Écoute les événements de paiement.
     */
    @KafkaListener(
        topics = {"payment.completed", "payment.failed"},
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentEvents(@Payload EventMessage message) {
        log.info("Payment event: type={}, userId={}", 
            message.getEventType(), message.getUserId());
        
        UserEvent event = convertToUserEvent(message);
        eventService.recordEvent(event);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Convertit un EventMessage Kafka en UserEvent.
     */
    private UserEvent convertToUserEvent(EventMessage message) {
        return UserEvent.builder()
            .eventId(UUID.randomUUID())
            .userId(message.getUserId())
            .eventType(message.getEventType())
            .eventData(message.getPayload())
            .timestamp(message.getTimestamp() != null ? 
                message.getTimestamp() : Instant.now())
            .sessionId(message.getSessionId())
            .deviceType(message.getMetadata().get("device_type"))
            .platform(message.getMetadata().get("platform"))
            .appVersion(message.getMetadata().get("app_version"))
            .ipAddress(message.getMetadata().get("ip_address"))
            .country(message.getMetadata().get("country"))
            .city(message.getMetadata().get("city"))
            .build();
    }
    
    /**
     * Retourne les statistiques de collecte.
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("processed", processedEvents.get());
        stats.put("failed", failedEvents.get());
        stats.put("buffered", eventBuffer.size());
        return stats;
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. METRIC LISTENER - Collecte des métriques système
// ═══════════════════════════════════════════════════════════════

/**
 * Listener Kafka pour les métriques système.
 * 
 * Topics écoutés :
 * - system.metrics (métriques générales)
 * - service.health (statuts des services)
 * - performance.metrics (métriques de performance)
 * 
 * @author Équipe Analytics - Sous-équipe Collector
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricCollectorListener {
    
    private final MetricService metricService;
    private final MetricBuffer metricBuffer;
    
    private final AtomicInteger processedMetrics = new AtomicInteger(0);
    private final AtomicInteger failedMetrics = new AtomicInteger(0);
    
    /**
     * Écoute le topic principal des métriques système.
     */
    @KafkaListener(
        topics = "system.metrics",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "metricKafkaListenerContainerFactory"
    )
    public void consumeSystemMetrics(
            @Payload List<MetricMessage> messages,
            Acknowledgment acknowledgment) {
        
        log.debug("Received {} metrics", messages.size());
        
        try {
            // Convertir les messages en entités SystemMetric
            List<SystemMetric> metrics = messages.stream()
                .map(this::convertToSystemMetric)
                .toList();
            
            // Ajouter au buffer
            metricBuffer.addAll(metrics);
            
            processedMetrics.addAndGet(metrics.size());
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("Error processing metrics batch", e);
            failedMetrics.addAndGet(messages.size());
            throw new RuntimeException("Metric processing failed", e);
        }
    }
    
    /**
     * Écoute les métriques de performance.
     */
    @KafkaListener(
        topics = "performance.metrics",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePerformanceMetrics(@Payload MetricMessage message) {
        SystemMetric metric = convertToSystemMetric(message);
        metricBuffer.add(metric);
    }
    
    /**
     * Écoute les statuts de santé des services.
     */
    @KafkaListener(
        topics = "service.health",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeServiceHealth(@Payload MetricMessage message) {
        log.debug("Service health: service={}, value={}", 
            message.getServiceName(), message.getValue());
        
        SystemMetric metric = convertToSystemMetric(message);
        metricService.recordMetric(metric);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════
    
    private SystemMetric convertToSystemMetric(MetricMessage message) {
        return SystemMetric.builder()
            .metricName(message.getMetricName())
            .metricValue(message.getValue())
            .tags(message.getTags())
            .timestamp(message.getTimestamp() != null ? 
                message.getTimestamp() : Instant.now())
            .serviceName(message.getServiceName())
            .instanceId(message.getTags().get("instance"))
            .build();
    }
    
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("processed", processedMetrics.get());
        stats.put("failed", failedMetrics.get());
        stats.put("buffered", metricBuffer.size());
        return stats;
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. EVENT BUFFER - Buffer pour insertion batch
// ═══════════════════════════════════════════════════════════════

/**
 * Buffer pour accumuler les événements avant insertion batch.
 * 
 * Fonctionnement :
 * - Accumule les événements en mémoire
 * - Flush automatique quand le buffer atteint la taille max
 * - Flush périodique toutes les X secondes
 * 
 * @author Équipe Analytics - Sous-équipe Collector
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventBuffer {
    
    private final EventService eventService;
    
    // Configuration
    private static final int BATCH_SIZE = 1000;
    private static final long FLUSH_INTERVAL_MS = 5000; // 5 secondes
    
    // Buffer thread-safe
    private final List<UserEvent> buffer = 
        Collections.synchronizedList(new ArrayList<>());
    
    private volatile Instant lastFlushTime = Instant.now();
    
    /**
     * Ajoute un événement au buffer.
     * Flush automatique si le buffer est plein.
     */
    public void add(UserEvent event) {
        buffer.add(event);
        
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }
    
    /**
     * Ajoute plusieurs événements au buffer.
     */
    public void addAll(List<UserEvent> events) {
        buffer.addAll(events);
        
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }
    
    /**
     * Flush le buffer vers ClickHouse.
     */
    public synchronized void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        
        log.info("Flushing event buffer: {} events", buffer.size());
        
        try {
            // Copier et vider le buffer
            List<UserEvent> toFlush = new ArrayList<>(buffer);
            buffer.clear();
            
            // Insertion batch
            int inserted = eventService.recordEventsBatch(toFlush);
            
            lastFlushTime = Instant.now();
            
            log.info("Successfully flushed {} events", inserted);
            
        } catch (Exception e) {
            log.error("Error flushing event buffer", e);
            // En cas d'erreur, les événements sont perdus
            // TODO: Implémenter dead letter queue
        }
    }
    
    /**
     * Flush périodique (appelé par un scheduler).
     */
    @org.springframework.scheduling.annotation.Scheduled(
        fixedDelayString = "${nexusai.analytics.collection.batch-timeout:5000}")
    public void scheduledFlush() {
        long timeSinceLastFlush = Instant.now().toEpochMilli() - 
            lastFlushTime.toEpochMilli();
        
        if (timeSinceLastFlush >= FLUSH_INTERVAL_MS && !buffer.isEmpty()) {
            flush();
        }
    }
    
    public int size() {
        return buffer.size();
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. METRIC BUFFER - Buffer pour métriques
// ═══════════════════════════════════════════════════════════════

/**
 * Buffer pour accumuler les métriques avant insertion batch.
 * 
 * @author Équipe Analytics - Sous-équipe Collector
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricBuffer {
    
    private final MetricService metricService;
    
    private static final int BATCH_SIZE = 1000;
    private static final long FLUSH_INTERVAL_MS = 5000;
    
    private final List<SystemMetric> buffer = 
        Collections.synchronizedList(new ArrayList<>());
    
    private volatile Instant lastFlushTime = Instant.now();
    
    public void add(SystemMetric metric) {
        buffer.add(metric);
        
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }
    
    public void addAll(List<SystemMetric> metrics) {
        buffer.addAll(metrics);
        
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }
    
    public synchronized void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        
        log.info("Flushing metric buffer: {} metrics", buffer.size());
        
        try {
            List<SystemMetric> toFlush = new ArrayList<>(buffer);
            buffer.clear();
            
            int inserted = metricService.recordMetricsBatch(toFlush);
            
            lastFlushTime = Instant.now();
            
            log.info("Successfully flushed {} metrics", inserted);
            
        } catch (Exception e) {
            log.error("Error flushing metric buffer", e);
        }
    }
    
    @org.springframework.scheduling.annotation.Scheduled(
        fixedDelayString = "${nexusai.analytics.collection.batch-timeout:5000}")
    public void scheduledFlush() {
        long timeSinceLastFlush = Instant.now().toEpochMilli() - 
            lastFlushTime.toEpochMilli();
        
        if (timeSinceLastFlush >= FLUSH_INTERVAL_MS && !buffer.isEmpty()) {
            flush();
        }
    }
    
    public int size() {
        return buffer.size();
    }
}

// ═══════════════════════════════════════════════════════════════
// 5. COLLECTOR STATISTICS - Statistiques de collecte
// ═══════════════════════════════════════════════════════════════

/**
 * Service pour exposer les statistiques de collecte.
 * 
 * @author Équipe Analytics - Sous-équipe Collector
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorStatistics {
    
    private final EventCollectorListener eventCollector;
    private final MetricCollectorListener metricCollector;
    private final EventBuffer eventBuffer;
    private final MetricBuffer metricBuffer;
    
    /**
     * Retourne les statistiques globales de collecte.
     */
    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("events", eventCollector.getStatistics());
        stats.put("metrics", metricCollector.getStatistics());
        
        stats.put("buffers", Map.of(
            "events", eventBuffer.size(),
            "metrics", metricBuffer.size()
        ));
        
        stats.put("timestamp", Instant.now());
        
        return stats;
    }
}
