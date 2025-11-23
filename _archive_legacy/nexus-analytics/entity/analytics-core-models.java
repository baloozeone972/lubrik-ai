package com.nexusai.analytics.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ═══════════════════════════════════════════════════════════════
 * ANALYTICS CORE - MODÈLES DE DONNÉES
 * 
 * Ces classes représentent les entités principales du système
 * d'analytics et sont partagées entre tous les sous-modules.
 * ═══════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════
// 1. USER EVENT - Événement utilisateur
// ═══════════════════════════════════════════════════════════════

/**
 * Représente un événement utilisateur capturé dans le système.
 * Ces événements sont stockés dans ClickHouse pour l'analyse.
 * 
 * Exemples d'événements :
 * - session_start, session_end
 * - page_view, button_click
 * - message_sent, image_generated
 * - subscription_created, payment_completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    
    /**
     * Identifiant unique de l'événement (UUID)
     */
    private UUID eventId;
    
    /**
     * ID de l'utilisateur qui a déclenché l'événement
     */
    private UUID userId;
    
    /**
     * Type d'événement (ex: 'session_start', 'message_sent')
     */
    private String eventType;
    
    /**
     * Données additionnelles de l'événement (JSON)
     * Contient des informations spécifiques selon le type
     */
    private Map<String, Object> eventData;
    
    /**
     * Horodatage de l'événement
     */
    private Instant timestamp;
    
    /**
     * ID de la session utilisateur
     */
    private UUID sessionId;
    
    /**
     * Type d'appareil (desktop, mobile, tablet)
     */
    private String deviceType;
    
    /**
     * Plateforme (web, ios, android)
     */
    private String platform;
    
    /**
     * Version de l'application
     */
    private String appVersion;
    
    /**
     * Adresse IP de l'utilisateur
     */
    private String ipAddress;
    
    /**
     * Pays de l'utilisateur
     */
    private String country;
    
    /**
     * Ville de l'utilisateur
     */
    private String city;
}

// ═══════════════════════════════════════════════════════════════
// 2. SYSTEM METRIC - Métrique système
// ═══════════════════════════════════════════════════════════════

/**
 * Représente une métrique système (CPU, mémoire, latence, etc.)
 * Ces métriques sont collectées par Prometheus et stockées dans ClickHouse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetric {
    
    /**
     * Nom de la métrique (ex: 'cpu_usage', 'memory_usage')
     */
    private String metricName;
    
    /**
     * Valeur de la métrique
     */
    private Double metricValue;
    
    /**
     * Tags associés à la métrique (ex: {service: "user-service", instance: "1"})
     */
    private Map<String, String> tags;
    
    /**
     * Horodatage de la mesure
     */
    private Instant timestamp;
    
    /**
     * Nom du service qui a émis la métrique
     */
    private String serviceName;
    
    /**
     * ID de l'instance du service
     */
    private String instanceId;
}

// ═══════════════════════════════════════════════════════════════
// 3. AGGREGATED METRIC - Métrique agrégée
// ═══════════════════════════════════════════════════════════════

/**
 * Représente une métrique agrégée sur une période de temps.
 * Utilisé pour les dashboards et rapports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedMetric {
    
    /**
     * Type de métrique agrégée
     */
    private String metricType;
    
    /**
     * Période d'agrégation (MINUTE, HOUR, DAY, WEEK, MONTH)
     */
    private AggregationPeriod period;
    
    /**
     * Début de la période
     */
    private Instant periodStart;
    
    /**
     * Fin de la période
     */
    private Instant periodEnd;
    
    /**
     * Nombre total d'événements
     */
    private Long count;
    
    /**
     * Valeur minimum
     */
    private Double minValue;
    
    /**
     * Valeur maximum
     */
    private Double maxValue;
    
    /**
     * Valeur moyenne
     */
    private Double avgValue;
    
    /**
     * Valeur médiane
     */
    private Double medianValue;
    
    /**
     * Percentile 95
     */
    private Double p95Value;
    
    /**
     * Percentile 99
     */
    private Double p99Value;
    
    /**
     * Tags pour filtrage
     */
    private Map<String, String> tags;
}

// ═══════════════════════════════════════════════════════════════
// 4. REPORT - Rapport généré
// ═══════════════════════════════════════════════════════════════

/**
 * Représente un rapport généré (quotidien, hebdomadaire, mensuel)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    
    /**
     * ID unique du rapport
     */
    private UUID reportId;
    
    /**
     * Type de rapport (DAILY, WEEKLY, MONTHLY, CUSTOM)
     */
    private ReportType reportType;
    
    /**
     * Titre du rapport
     */
    private String title;
    
    /**
     * Description du rapport
     */
    private String description;
    
    /**
     * Période couverte - début
     */
    private Instant periodStart;
    
    /**
     * Période couverte - fin
     */
    private Instant periodEnd;
    
    /**
     * Données du rapport (JSON)
     */
    private Map<String, Object> reportData;
    
    /**
     * Format du rapport (JSON, PDF, EXCEL)
     */
    private ReportFormat format;
    
    /**
     * URL de stockage du rapport
     */
    private String storageUrl;
    
    /**
     * Date de génération
     */
    private Instant generatedAt;
    
    /**
     * Généré par (user ID ou "SYSTEM")
     */
    private String generatedBy;
    
    /**
     * Statut (PENDING, COMPLETED, FAILED)
     */
    private ReportStatus status;
}

// ═══════════════════════════════════════════════════════════════
// 5. ALERT - Alerte système
// ═══════════════════════════════════════════════════════════════

/**
 * Représente une alerte déclenchée par le système de monitoring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    
    /**
     * ID unique de l'alerte
     */
    private UUID alertId;
    
    /**
     * Nom de l'alerte
     */
    private String alertName;
    
    /**
     * Sévérité (INFO, WARNING, CRITICAL)
     */
    private AlertSeverity severity;
    
    /**
     * Message de l'alerte
     */
    private String message;
    
    /**
     * Description détaillée
     */
    private String description;
    
    /**
     * Service concerné
     */
    private String serviceName;
    
    /**
     * Métrique qui a déclenché l'alerte
     */
    private String triggerMetric;
    
    /**
     * Valeur qui a déclenché l'alerte
     */
    private Double triggerValue;
    
    /**
     * Seuil configuré
     */
    private Double threshold;
    
    /**
     * Date de déclenchement
     */
    private Instant triggeredAt;
    
    /**
     * Date de résolution (null si non résolue)
     */
    private Instant resolvedAt;
    
    /**
     * Statut (ACTIVE, RESOLVED, ACKNOWLEDGED)
     */
    private AlertStatus status;
    
    /**
     * Reconnu par (user ID)
     */
    private UUID acknowledgedBy;
    
    /**
     * Labels additionnels
     */
    private Map<String, String> labels;
}

// ═══════════════════════════════════════════════════════════════
// 6. ENUMS
// ═══════════════════════════════════════════════════════════════

/**
 * Période d'agrégation
 */
public enum AggregationPeriod {
    MINUTE,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    YEAR
}

/**
 * Type de rapport
 */
public enum ReportType {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM
}

/**
 * Format de rapport
 */
public enum ReportFormat {
    JSON,
    PDF,
    EXCEL,
    CSV
}

/**
 * Statut de rapport
 */
public enum ReportStatus {
    PENDING,
    GENERATING,
    COMPLETED,
    FAILED
}

/**
 * Sévérité d'alerte
 */
public enum AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Statut d'alerte
 */
public enum AlertStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED,
    SILENCED
}

// ═══════════════════════════════════════════════════════════════
// 7. DTOs pour Kafka Events
// ═══════════════════════════════════════════════════════════════

/**
 * DTO pour les événements Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {
    private String eventType;
    private UUID userId;
    private UUID sessionId;
    private Map<String, Object> payload;
    private Instant timestamp;
    private Map<String, String> metadata;
}

/**
 * DTO pour les métriques Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricMessage {
    private String metricName;
    private Double value;
    private Map<String, String> tags;
    private Instant timestamp;
    private String serviceName;
}
