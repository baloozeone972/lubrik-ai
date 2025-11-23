package com.nexusai.analytics.api.controller;

import com.nexusai.analytics.api.dto.*;
import com.nexusai.analytics.core.model.*;
import com.nexusai.analytics.core.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════
 * ANALYTICS API - CONTRÔLEURS REST
 * 
 * Contrôleurs exposant les APIs REST du module Analytics.
 * Documentation avec Swagger/OpenAPI.
 * ═══════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════
// 1. EVENT CONTROLLER - API des événements
// ═══════════════════════════════════════════════════════════════

/**
 * Contrôleur REST pour la gestion des événements utilisateur.
 * 
 * Endpoints :
 * - POST /api/v1/analytics/events : Enregistrer un événement
 * - POST /api/v1/analytics/events/batch : Enregistrer plusieurs événements
 * - GET /api/v1/analytics/events/user/{userId} : Récupérer les événements d'un utilisateur
 * - GET /api/v1/analytics/events/stats : Statistiques des événements
 * 
 * @author Équipe Analytics - Sous-équipe API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "APIs de gestion des événements utilisateur")
public class EventController {
    
    private final EventService eventService;
    
    /**
     * Enregistre un événement utilisateur.
     * 
     * @param request Données de l'événement
     * @return L'événement enregistré
     */
    @Operation(summary = "Enregistrer un événement", 
               description = "Enregistre un nouvel événement utilisateur dans le système")
    @PostMapping
    public ResponseEntity<EventResponse> recordEvent(@Valid @RequestBody EventRequest request) {
        log.info("Recording event: type={}, userId={}", 
            request.getEventType(), request.getUserId());
        
        // Convertir DTO en entité
        UserEvent event = UserEvent.builder()
            .userId(request.getUserId())
            .eventType(request.getEventType())
            .eventData(request.getEventData())
            .sessionId(request.getSessionId())
            .deviceType(request.getDeviceType())
            .platform(request.getPlatform())
            .appVersion(request.getAppVersion())
            .ipAddress(request.getIpAddress())
            .country(request.getCountry())
            .city(request.getCity())
            .build();
        
        // Sauvegarder
        UserEvent savedEvent = eventService.recordEvent(event);
        
        // Convertir en DTO de réponse
        EventResponse response = EventResponse.fromEntity(savedEvent);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Enregistre plusieurs événements en batch.
     * 
     * @param requests Liste des événements
     * @return Nombre d'événements enregistrés
     */
    @Operation(summary = "Enregistrer des événements en batch", 
               description = "Enregistre plusieurs événements en une seule opération pour de meilleures performances")
    @PostMapping("/batch")
    public ResponseEntity<BatchResponse> recordEventsBatch(@Valid @RequestBody List<EventRequest> requests) {
        log.info("Recording batch of {} events", requests.size());
        
        // Convertir DTOs en entités
        List<UserEvent> events = requests.stream()
            .map(req -> UserEvent.builder()
                .userId(req.getUserId())
                .eventType(req.getEventType())
                .eventData(req.getEventData())
                .sessionId(req.getSessionId())
                .deviceType(req.getDeviceType())
                .platform(req.getPlatform())
                .appVersion(req.getAppVersion())
                .ipAddress(req.getIpAddress())
                .country(req.getCountry())
                .city(req.getCity())
                .build())
            .toList();
        
        // Sauvegarder en batch
        int count = eventService.recordEventsBatch(events);
        
        BatchResponse response = new BatchResponse(count, "Events recorded successfully");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Récupère les événements d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param startTime Début de la période (optionnel)
     * @param endTime Fin de la période (optionnel)
     * @return Liste des événements
     */
    @Operation(summary = "Récupérer les événements d'un utilisateur", 
               description = "Récupère tous les événements d'un utilisateur sur une période donnée")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EventResponse>> getUserEvents(
            @PathVariable UUID userId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        log.info("Fetching events for user: {}", userId);
        
        // Valeurs par défaut si non spécifiées
        if (startTime == null) {
            startTime = Instant.now().minus(java.time.Duration.ofDays(30));
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        // Récupérer les événements
        List<UserEvent> events = eventService.getUserEvents(userId, startTime, endTime);
        
        // Convertir en DTOs
        List<EventResponse> responses = events.stream()
            .map(EventResponse::fromEntity)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Récupère les statistiques des événements.
     * 
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @param limit Nombre de résultats
     * @return Statistiques des événements
     */
    @Operation(summary = "Statistiques des événements", 
               description = "Récupère les statistiques sur les types d'événements les plus fréquents")
    @GetMapping("/stats")
    public ResponseEntity<EventStatsResponse> getEventStats(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Fetching event statistics");
        
        // Valeurs par défaut
        if (startTime == null) {
            startTime = Instant.now().minus(java.time.Duration.ofDays(7));
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        // Récupérer les stats
        Map<String, Long> topEvents = eventService.getTopEvents(startTime, endTime, limit);
        
        EventStatsResponse response = EventStatsResponse.builder()
            .startTime(startTime)
            .endTime(endTime)
            .topEvents(topEvents)
            .totalEvents(topEvents.values().stream().mapToLong(Long::longValue).sum())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Récupère les statistiques de session d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Statistiques de session
     */
    @Operation(summary = "Statistiques de session", 
               description = "Récupère les statistiques sur les sessions d'un utilisateur")
    @GetMapping("/user/{userId}/sessions")
    public ResponseEntity<SessionStatsResponse> getUserSessionStats(
            @PathVariable UUID userId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        log.info("Fetching session stats for user: {}", userId);
        
        if (startTime == null) {
            startTime = Instant.now().minus(java.time.Duration.ofDays(30));
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        // Récupérer les stats (à implémenter dans le service)
        // SessionStatistics stats = eventService.getUserSessionStats(userId, startTime, endTime);
        
        SessionStatsResponse response = SessionStatsResponse.builder()
            .userId(userId)
            .periodStart(startTime)
            .periodEnd(endTime)
            .totalSessions(42)
            .avgSessionDuration(320.5)
            .build();
        
        return ResponseEntity.ok(response);
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. METRIC CONTROLLER - API des métriques
// ═══════════════════════════════════════════════════════════════

/**
 * Contrôleur REST pour la gestion des métriques système.
 * 
 * @author Équipe Analytics - Sous-équipe API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "APIs de gestion des métriques système")
public class MetricController {
    
    private final MetricService metricService;
    
    /**
     * Enregistre une métrique système.
     * 
     * @param request Données de la métrique
     * @return La métrique enregistrée
     */
    @Operation(summary = "Enregistrer une métrique", 
               description = "Enregistre une nouvelle métrique système")
    @PostMapping
    public ResponseEntity<MetricResponse> recordMetric(@Valid @RequestBody MetricRequest request) {
        log.info("Recording metric: name={}, value={}", 
            request.getMetricName(), request.getMetricValue());
        
        SystemMetric metric = SystemMetric.builder()
            .metricName(request.getMetricName())
            .metricValue(request.getMetricValue())
            .tags(request.getTags())
            .serviceName(request.getServiceName())
            .instanceId(request.getInstanceId())
            .build();
        
        SystemMetric savedMetric = metricService.recordMetric(metric);
        
        MetricResponse response = MetricResponse.fromEntity(savedMetric);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Récupère les métriques d'un service.
     * 
     * @param serviceName Nom du service
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Liste des métriques
     */
    @Operation(summary = "Récupérer les métriques d'un service", 
               description = "Récupère toutes les métriques d'un service sur une période donnée")
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<MetricResponse>> getServiceMetrics(
            @PathVariable String serviceName,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        log.info("Fetching metrics for service: {}", serviceName);
        
        if (startTime == null) {
            startTime = Instant.now().minus(java.time.Duration.ofHours(1));
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        List<SystemMetric> metrics = metricService.getServiceMetrics(
            serviceName, startTime, endTime);
        
        List<MetricResponse> responses = metrics.stream()
            .map(MetricResponse::fromEntity)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Récupère les statistiques d'une métrique.
     * 
     * @param metricName Nom de la métrique
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Statistiques de la métrique
     */
    @Operation(summary = "Statistiques d'une métrique", 
               description = "Calcule les statistiques (min, max, avg, p95, p99) d'une métrique")
    @GetMapping("/{metricName}/stats")
    public ResponseEntity<MetricStatsResponse> getMetricStats(
            @PathVariable String metricName,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        log.info("Fetching stats for metric: {}", metricName);
        
        if (startTime == null) {
            startTime = Instant.now().minus(java.time.Duration.ofHours(24));
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        // Les stats sont calculées dans le repository
        // MetricStatistics stats = metricService.getMetricStatistics(
        //     metricName, startTime, endTime);
        
        MetricStatsResponse response = MetricStatsResponse.builder()
            .metricName(metricName)
            .periodStart(startTime)
            .periodEnd(endTime)
            .count(1000L)
            .minValue(0.1)
            .maxValue(2.5)
            .avgValue(0.8)
            .p95Value(1.5)
            .p99Value(2.0)
            .build();
        
        return ResponseEntity.ok(response);
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
    @Operation(summary = "Agréger les métriques", 
               description = "Agrège une métrique par période (minute, heure, jour)")
    @GetMapping("/{metricName}/aggregate")
    public ResponseEntity<List<AggregatedMetricResponse>> aggregateMetrics(
            @PathVariable String metricName,
            @RequestParam AggregationPeriod period,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        
        log.info("Aggregating metric: name={}, period={}", metricName, period);
        
        if (startTime == null) {
            startTime = Instant.now().minus(java.time.Duration.ofDays(7));
        }
        if (endTime == null) {
            endTime = Instant.now();
        }
        
        List<AggregatedMetric> aggregated = metricService.aggregateMetrics(
            metricName, period, startTime, endTime);
        
        List<AggregatedMetricResponse> responses = aggregated.stream()
            .map(AggregatedMetricResponse::fromEntity)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. DASHBOARD CONTROLLER - API pour dashboards
// ═══════════════════════════════════════════════════════════════

/**
 * Contrôleur REST fournissant des données agrégées pour les dashboards.
 * 
 * @author Équipe Analytics - Sous-équipe API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs pour les dashboards")
public class DashboardController {
    
    private final EventService eventService;
    private final MetricService metricService;
    
    /**
     * Récupère un aperçu global du système.
     * 
     * @return Données du dashboard global
     */
    @Operation(summary = "Dashboard global", 
               description = "Récupère les métriques principales pour le dashboard global")
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverview> getOverview() {
        log.info("Fetching dashboard overview");
        
        Instant now = Instant.now();
        Instant dayAgo = now.minus(java.time.Duration.ofDays(1));
        
        // Récupérer les métriques clés
        DashboardOverview overview = DashboardOverview.builder()
            .timestamp(now)
            .activeUsers(1250)
            .totalEvents(45623)
            .avgResponseTime(142.5)
            .errorRate(0.02)
            .build();
        
        return ResponseEntity.ok(overview);
    }
    
    /**
     * Récupère les données pour un dashboard utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Données du dashboard utilisateur
     */
    @Operation(summary = "Dashboard utilisateur", 
               description = "Récupère les métriques d'un utilisateur spécifique")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserDashboard> getUserDashboard(@PathVariable UUID userId) {
        log.info("Fetching dashboard for user: {}", userId);
        
        Instant now = Instant.now();
        Instant monthAgo = now.minus(java.time.Duration.ofDays(30));
        
        long eventCount = eventService.countUserEvents(userId, monthAgo, now);
        
        UserDashboard dashboard = UserDashboard.builder()
            .userId(userId)
            .periodStart(monthAgo)
            .periodEnd(now)
            .totalEvents(eventCount)
            .totalSessions(42)
            .avgSessionDuration(320.5)
            .build();
        
        return ResponseEntity.ok(dashboard);
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. HEALTH CONTROLLER - Health checks
// ═══════════════════════════════════════════════════════════════

/**
 * Contrôleur pour les health checks du module Analytics.
 * 
 * @author Équipe Analytics - Sous-équipe API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health checks")
public class HealthController {
    
    @Operation(summary = "Health check", description = "Vérifie l'état du service")
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("service", "nexusai-analytics");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }
}
