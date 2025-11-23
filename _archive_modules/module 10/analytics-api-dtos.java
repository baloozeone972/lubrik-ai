package com.nexusai.analytics.api.dto;

import com.nexusai.analytics.core.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ═══════════════════════════════════════════════════════════════
 * ANALYTICS API - DTOs (Data Transfer Objects)
 * 
 * Classes pour les requêtes et réponses de l'API REST.
 * Séparation claire entre la couche API et la couche métier.
 * ═══════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════
// 1. EVENT DTOs - Événements
// ═══════════════════════════════════════════════════════════════

/**
 * DTO pour la création d'un événement.
 * 
 * @author Équipe Analytics - Sous-équipe API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Event type is required")
    private String eventType;
    
    private Map<String, Object> eventData;
    
    private UUID sessionId;
    
    private String deviceType;
    
    private String platform;
    
    private String appVersion;
    
    private String ipAddress;
    
    private String country;
    
    private String city;
}

/**
 * DTO pour la réponse d'un événement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    
    private UUID eventId;
    
    private UUID userId;
    
    private String eventType;
    
    private Map<String, Object> eventData;
    
    private Instant timestamp;
    
    private UUID sessionId;
    
    private String deviceType;
    
    private String platform;
    
    private String appVersion;
    
    /**
     * Convertit une entité UserEvent en DTO de réponse.
     */
    public static EventResponse fromEntity(UserEvent event) {
        return EventResponse.builder()
            .eventId(event.getEventId())
            .userId(event.getUserId())
            .eventType(event.getEventType())
            .eventData(event.getEventData())
            .timestamp(event.getTimestamp())
            .sessionId(event.getSessionId())
            .deviceType(event.getDeviceType())
            .platform(event.getPlatform())
            .appVersion(event.getAppVersion())
            .build();
    }
}

/**
 * DTO pour les statistiques d'événements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatsResponse {
    
    private Instant startTime;
    
    private Instant endTime;
    
    private Map<String, Long> topEvents;
    
    private Long totalEvents;
}

/**
 * DTO pour les statistiques de session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatsResponse {
    
    private UUID userId;
    
    private Instant periodStart;
    
    private Instant periodEnd;
    
    private Integer totalSessions;
    
    private Double avgSessionDuration;
    
    private Integer totalEvents;
    
    private Double avgEventsPerSession;
}

// ═══════════════════════════════════════════════════════════════
// 2. METRIC DTOs - Métriques
// ═══════════════════════════════════════════════════════════════

/**
 * DTO pour la création d'une métrique.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricRequest {
    
    @NotBlank(message = "Metric name is required")
    private String metricName;
    
    @NotNull(message = "Metric value is required")
    private Double metricValue;
    
    private Map<String, String> tags;
    
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    private String instanceId;
}

/**
 * DTO pour la réponse d'une métrique.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricResponse {
    
    private String metricName;
    
    private Double metricValue;
    
    private Map<String, String> tags;
    
    private Instant timestamp;
    
    private String serviceName;
    
    private String instanceId;
    
    /**
     * Convertit une entité SystemMetric en DTO de réponse.
     */
    public static MetricResponse fromEntity(SystemMetric metric) {
        return MetricResponse.builder()
            .metricName(metric.getMetricName())
            .metricValue(metric.getMetricValue())
            .tags(metric.getTags())
            .timestamp(metric.getTimestamp())
            .serviceName(metric.getServiceName())
            .instanceId(metric.getInstanceId())
            .build();
    }
}

/**
 * DTO pour les statistiques d'une métrique.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricStatsResponse {
    
    private String metricName;
    
    private Instant periodStart;
    
    private Instant periodEnd;
    
    private Long count;
    
    private Double minValue;
    
    private Double maxValue;
    
    private Double avgValue;
    
    private Double medianValue;
    
    private Double p95Value;
    
    private Double p99Value;
}

/**
 * DTO pour une métrique agrégée.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedMetricResponse {
    
    private String metricType;
    
    private AggregationPeriod period;
    
    private Instant periodStart;
    
    private Instant periodEnd;
    
    private Long count;
    
    private Double minValue;
    
    private Double maxValue;
    
    private Double avgValue;
    
    private Double p95Value;
    
    /**
     * Convertit une entité AggregatedMetric en DTO de réponse.
     */
    public static AggregatedMetricResponse fromEntity(AggregatedMetric metric) {
        return AggregatedMetricResponse.builder()
            .metricType(metric.getMetricType())
            .period(metric.getPeriod())
            .periodStart(metric.getPeriodStart())
            .periodEnd(metric.getPeriodEnd())
            .count(metric.getCount())
            .minValue(metric.getMinValue())
            .maxValue(metric.getMaxValue())
            .avgValue(metric.getAvgValue())
            .p95Value(metric.getP95Value())
            .build();
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. DASHBOARD DTOs - Dashboards
// ═══════════════════════════════════════════════════════════════

/**
 * DTO pour le dashboard global.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverview {
    
    private Instant timestamp;
    
    /**
     * Nombre d'utilisateurs actifs
     */
    private Integer activeUsers;
    
    /**
     * Nombre total d'événements
     */
    private Long totalEvents;
    
    /**
     * Temps de réponse moyen (ms)
     */
    private Double avgResponseTime;
    
    /**
     * Taux d'erreur (%)
     */
    private Double errorRate;
    
    /**
     * Utilisation CPU moyenne (%)
     */
    private Double avgCpuUsage;
    
    /**
     * Utilisation mémoire moyenne (%)
     */
    private Double avgMemoryUsage;
    
    /**
     * Nombre total de requêtes
     */
    private Long totalRequests;
    
    /**
     * Nombre de requêtes en erreur
     */
    private Long errorRequests;
}

/**
 * DTO pour le dashboard utilisateur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboard {
    
    private UUID userId;
    
    private Instant periodStart;
    
    private Instant periodEnd;
    
    /**
     * Nombre total d'événements
     */
    private Long totalEvents;
    
    /**
     * Nombre de sessions
     */
    private Integer totalSessions;
    
    /**
     * Durée moyenne de session (secondes)
     */
    private Double avgSessionDuration;
    
    /**
     * Plateforme la plus utilisée
     */
    private String mostUsedPlatform;
    
    /**
     * Type d'appareil le plus utilisé
     */
    private String mostUsedDevice;
    
    /**
     * Actions les plus fréquentes
     */
    private Map<String, Long> topActions;
}

// ═══════════════════════════════════════════════════════════════
// 4. REPORT DTOs - Rapports
// ═══════════════════════════════════════════════════════════════

/**
 * DTO pour la création d'un rapport.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    
    @NotNull(message = "Report type is required")
    private ReportType reportType;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Period start is required")
    private Instant periodStart;
    
    @NotNull(message = "Period end is required")
    private Instant periodEnd;
    
    private ReportFormat format;
    
    /**
     * Filtres additionnels pour le rapport
     */
    private Map<String, String> filters;
}

/**
 * DTO pour la réponse d'un rapport.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    
    private UUID reportId;
    
    private ReportType reportType;
    
    private String title;
    
    private String description;
    
    private Instant periodStart;
    
    private Instant periodEnd;
    
    private ReportFormat format;
    
    private String storageUrl;
    
    private ReportStatus status;
    
    private Instant generatedAt;
    
    private String generatedBy;
    
    /**
     * Convertit une entité Report en DTO de réponse.
     */
    public static ReportResponse fromEntity(Report report) {
        return ReportResponse.builder()
            .reportId(report.getReportId())
            .reportType(report.getReportType())
            .title(report.getTitle())
            .description(report.getDescription())
            .periodStart(report.getPeriodStart())
            .periodEnd(report.getPeriodEnd())
            .format(report.getFormat())
            .storageUrl(report.getStorageUrl())
            .status(report.getStatus())
            .generatedAt(report.getGeneratedAt())
            .generatedBy(report.getGeneratedBy())
            .build();
    }
}

// ═══════════════════════════════════════════════════════════════
// 5. ALERT DTOs - Alertes
// ═══════════════════════════════════════════════════════════════

/**
 * DTO pour la création d'une alerte.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {
    
    @NotBlank(message = "Alert name is required")
    private String alertName;
    
    @NotNull(message = "Severity is required")
    private AlertSeverity severity;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String description;
    
    @NotBlank(message = "Service name is required")
    private String serviceName;
    
    private String triggerMetric;
    
    private Double triggerValue;
    
    private Double threshold;
    
    private Map<String, String> labels;
}

/**
 * DTO pour la réponse d'une alerte.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    
    private UUID alertId;
    
    private String alertName;
    
    private AlertSeverity severity;
    
    private String message;
    
    private String description;
    
    private String serviceName;
    
    private String triggerMetric;
    
    private Double triggerValue;
    
    private Double threshold;
    
    private Instant triggeredAt;
    
    private Instant resolvedAt;
    
    private AlertStatus status;
    
    private UUID acknowledgedBy;
    
    private Map<String, String> labels;
    
    /**
     * Convertit une entité Alert en DTO de réponse.
     */
    public static AlertResponse fromEntity(Alert alert) {
        return AlertResponse.builder()
            .alertId(alert.getAlertId())
            .alertName(alert.getAlertName())
            .severity(alert.getSeverity())
            .message(alert.getMessage())
            .description(alert.getDescription())
            .serviceName(alert.getServiceName())
            .triggerMetric(alert.getTriggerMetric())
            .triggerValue(alert.getTriggerValue())
            .threshold(alert.getThreshold())
            .triggeredAt(alert.getTriggeredAt())
            .resolvedAt(alert.getResolvedAt())
            .status(alert.getStatus())
            .acknowledgedBy(alert.getAcknowledgedBy())
            .labels(alert.getLabels())
            .build();
    }
}

// ═══════════════════════════════════════════════════════════════
// 6. GENERIC DTOs - Réponses génériques
// ═══════════════════════════════════════════════════════════════

/**
 * DTO pour les réponses batch.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchResponse {
    
    private Integer count;
    
    private String message;
}

/**
 * DTO pour les réponses d'erreur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private Instant timestamp;
    
    private Integer status;
    
    private String error;
    
    private String message;
    
    private String path;
    
    private Map<String, String> validationErrors;
}

/**
 * DTO pour les réponses de succès génériques.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse {
    
    private Instant timestamp;
    
    private String message;
    
    private Object data;
}

/**
 * DTO pour les réponses paginées.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    
    private java.util.List<T> content;
    
    private Integer pageNumber;
    
    private Integer pageSize;
    
    private Long totalElements;
    
    private Integer totalPages;
    
    private Boolean first;
    
    private Boolean last;
}
