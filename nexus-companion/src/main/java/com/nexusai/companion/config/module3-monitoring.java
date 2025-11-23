// ============================================================================
// PACKAGE: com.nexusai.companion.config
// Description: Configuration du monitoring et métriques personnalisées
// ============================================================================

package com.nexusai.companion.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Configuration des métriques personnalisées pour Prometheus.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Configuration
public class MetricsConfig {
    
    /**
     * Bean pour les métriques métier.
     */
    @Bean
    public CompanionMetrics companionMetrics(MeterRegistry registry) {
        return new CompanionMetrics(registry);
    }
}

/**
 * Classe contenant toutes les métriques métier du service.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class CompanionMetrics {
    
    private final MeterRegistry registry;
    
    // Compteurs
    private Counter companionCreatedCounter;
    private Counter companionDeletedCounter;
    private Counter companionEvolvedCounter;
    private Counter companionMergedCounter;
    private Counter companionLikedCounter;
    
    // Timers
    private Timer companionCreationTimer;
    private Timer evolutionTimer;
    private Timer mergeTimer;
    
    @javax.annotation.PostConstruct
    public void init() {
        // Initialiser les compteurs
        companionCreatedCounter = Counter.builder("companion.created.total")
            .description("Nombre total de compagnons créés")
            .register(registry);
        
        companionDeletedCounter = Counter.builder("companion.deleted.total")
            .description("Nombre total de compagnons supprimés")
            .register(registry);
        
        companionEvolvedCounter = Counter.builder("companion.evolved.total")
            .description("Nombre total d'évolutions génétiques")
            .register(registry);
        
        companionMergedCounter = Counter.builder("companion.merged.total")
            .description("Nombre total de fusions de compagnons")
            .register(registry);
        
        companionLikedCounter = Counter.builder("companion.liked.total")
            .description("Nombre total de likes")
            .register(registry);
        
        // Initialiser les timers
        companionCreationTimer = Timer.builder("companion.creation.time")
            .description("Temps de création d'un compagnon")
            .register(registry);
        
        evolutionTimer = Timer.builder("companion.evolution.time")
            .description("Temps d'évolution génétique")
            .register(registry);
        
        mergeTimer = Timer.builder("companion.merge.time")
            .description("Temps de fusion de compagnons")
            .register(registry);
    }
    
    public void incrementCompanionCreated() {
        companionCreatedCounter.increment();
    }
    
    public void incrementCompanionDeleted() {
        companionDeletedCounter.increment();
    }
    
    public void incrementCompanionEvolved() {
        companionEvolvedCounter.increment();
    }
    
    public void incrementCompanionMerged() {
        companionMergedCounter.increment();
    }
    
    public void incrementCompanionLiked() {
        companionLikedCounter.increment();
    }
    
    public Timer.Sample startCreationTimer() {
        return Timer.start(registry);
    }
    
    public void recordCreation(Timer.Sample sample) {
        sample.stop(companionCreationTimer);
    }
    
    public Timer.Sample startEvolutionTimer() {
        return Timer.start(registry);
    }
    
    public void recordEvolution(Timer.Sample sample) {
        sample.stop(evolutionTimer);
    }
    
    public Timer.Sample startMergeTimer() {
        return Timer.start(registry);
    }
    
    public void recordMerge(Timer.Sample sample) {
        sample.stop(mergeTimer);
    }
}

// ============================================================================
// FICHIER: prometheus.yml
// Description: Configuration Prometheus
// ============================================================================

/**
 * prometheus.yml
 */

global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'nexusai-production'
    service: 'companion-service'

# Alerting configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

# Load rules
rule_files:
  - 'alerts.yml'

# Scrape configurations
scrape_configs:
  # Companion Service
  - job_name: 'companion-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['companion-service:8083']
        labels:
          environment: 'production'
          region: 'eu-west-1'
    
    # Relabeling
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: '([^:]+)(:[0-9]+)?'
        replacement: '${1}'

  # MongoDB Exporter
  - job_name: 'mongodb'
    static_configs:
      - targets: ['mongodb-exporter:9216']

  # Redis Exporter
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  # Kafka Exporter
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']

// ============================================================================
// FICHIER: alerts.yml
// Description: Règles d'alertes Prometheus
// ============================================================================

/**
 * alerts.yml
 */

groups:
  - name: companion_service_alerts
    interval: 30s
    rules:
      # Service Down
      - alert: CompanionServiceDown
        expr: up{job="companion-service"} == 0
        for: 2m
        labels:
          severity: critical
          service: companion-service
        annotations:
          summary: "Companion Service est down"
          description: "Le service Companion n'est pas accessible depuis 2 minutes"

      # High Error Rate
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[5m]) 
          / 
          rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
          service: companion-service
        annotations:
          summary: "Taux d'erreur élevé détecté"
          description: "Le taux d'erreur 5xx est au-dessus de 5% ({{ $value }}%)"

      # High Latency
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95, 
            rate(http_server_requests_seconds_bucket[5m])
          ) > 1
        for: 10m
        labels:
          severity: warning
          service: companion-service
        annotations:
          summary: "Latence élevée détectée"
          description: "P95 latency est > 1s ({{ $value }}s)"

      # Memory Usage
      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.85
        for: 5m
        labels:
          severity: warning
          service: companion-service
        annotations:
          summary: "Utilisation mémoire élevée"
          description: "L'utilisation de la heap est > 85% ({{ $value }}%)"

      # Database Connection Pool
      - alert: LowDatabaseConnections
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: warning
          service: companion-service
        annotations:
          summary: "Pool de connexions DB presque saturé"
          description: "Utilisation du pool de connexions > 80%"

      # Kafka Lag
      - alert: HighKafkaLag
        expr: kafka_consumergroup_lag > 1000
        for: 10m
        labels:
          severity: warning
          service: companion-service
        annotations:
          summary: "Lag Kafka élevé"
          description: "Le lag du consumer Kafka est > 1000 messages"

      # Evolution Job Failure
      - alert: EvolutionJobFailed
        expr: |
          increase(companion_evolved_total[1h]) == 0 
          AND 
          day_of_week() == 0
        for: 1h
        labels:
          severity: warning
          service: companion-service
        annotations:
          summary: "Job d'évolution hebdomadaire n'a pas tourné"
          description: "Aucune évolution détectée le dimanche"

      # Quota Exceeded Rate
      - alert: HighQuotaExceededRate
        expr: |
          rate(companion_quota_exceeded_total[5m]) > 10
        for: 10m
        labels:
          severity: info
          service: companion-service
        annotations:
          summary: "Taux élevé de dépassement de quota"
          description: "Plus de 10 tentatives/s de dépassement de quota"

// ============================================================================
// FICHIER: grafana/dashboard.json
// Description: Dashboard Grafana pour le service
// ============================================================================

/**
 * grafana/dashboard.json
 * Dashboard complet pour le monitoring du Companion Service
 */

{
  "dashboard": {
    "title": "NexusAI - Companion Service",
    "tags": ["nexusai", "companion", "microservice"],
    "timezone": "browser",
    "refresh": "30s",
    
    "panels": [
      {
        "id": 1,
        "title": "Request Rate (req/s)",
        "type": "graph",
        "gridPos": {"x": 0, "y": 0, "w": 12, "h": 8},
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{job='companion-service'}[5m])) by (uri)",
            "legendFormat": "{{uri}}"
          }
        ]
      },
      
      {
        "id": 2,
        "title": "Error Rate (%)",
        "type": "graph",
        "gridPos": {"x": 12, "y": 0, "w": 12, "h": 8},
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{job='companion-service',status=~'5..'}[5m])) / sum(rate(http_server_requests_seconds_count{job='companion-service'}[5m])) * 100",
            "legendFormat": "Error Rate"
          }
        ]
      },
      
      {
        "id": 3,
        "title": "P95 Latency (s)",
        "type": "graph",
        "gridPos": {"x": 0, "y": 8, "w": 12, "h": 8},
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{job='companion-service'}[5m])) by (le, uri))",
            "legendFormat": "{{uri}}"
          }
        ]
      },
      
      {
        "id": 4,
        "title": "Companions Created (Total)",
        "type": "stat",
        "gridPos": {"x": 12, "y": 8, "w": 6, "h": 4},
        "targets": [
          {
            "expr": "companion_created_total"
          }
        ]
      },
      
      {
        "id": 5,
        "title": "Active Companions",
        "type": "stat",
        "gridPos": {"x": 18, "y": 8, "w": 6, "h": 4},
        "targets": [
          {
            "expr": "mongodb_collection_count{collection='companions'}"
          }
        ]
      },
      
      {
        "id": 6,
        "title": "Evolutions (Last 24h)",
        "type": "stat",
        "gridPos": {"x": 12, "y": 12, "w": 6, "h": 4},
        "targets": [
          {
            "expr": "increase(companion_evolved_total[24h])"
          }
        ]
      },
      
      {
        "id": 7,
        "title": "Merges (Last 24h)",
        "type": "stat",
        "gridPos": {"x": 18, "y": 12, "w": 6, "h": 4},
        "targets": [
          {
            "expr": "increase(companion_merged_total[24h])"
          }
        ]
      },
      
      {
        "id": 8,
        "title": "JVM Memory (Heap)",
        "type": "graph",
        "gridPos": {"x": 0, "y": 16, "w": 12, "h": 8},
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area='heap',job='companion-service'}",
            "legendFormat": "Used"
          },
          {
            "expr": "jvm_memory_max_bytes{area='heap',job='companion-service'}",
            "legendFormat": "Max"
          }
        ]
      },
      
      {
        "id": 9,
        "title": "Database Connections",
        "type": "graph",
        "gridPos": {"x": 12, "y": 16, "w": 12, "h": 8},
        "targets": [
          {
            "expr": "hikaricp_connections_active{job='companion-service'}",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_connections_idle{job='companion-service'}",
            "legendFormat": "Idle"
          },
          {
            "expr": "hikaricp_connections_max{job='companion-service'}",
            "legendFormat": "Max"
          }
        ]
      },
      
      {
        "id": 10,
        "title": "Top Endpoints by Request Count",
        "type": "table",
        "gridPos": {"x": 0, "y": 24, "w": 12, "h": 8},
        "targets": [
          {
            "expr": "topk(10, sum(rate(http_server_requests_seconds_count{job='companion-service'}[5m])) by (uri, method))",
            "format": "table"
          }
        ]
      },
      
      {
        "id": 11,
        "title": "Kafka Consumer Lag",
        "type": "graph",
        "gridPos": {"x": 12, "y": 24, "w": 12, "h": 8},
        "targets": [
          {
            "expr": "kafka_consumergroup_lag{group='companion-service'}",
            "legendFormat": "{{topic}}"
          }
        ]
      }
    ]
  }
}

// ============================================================================
// PACKAGE: com.nexusai.companion.aspect
// Description: Aspects AOP pour le logging et métriques
// ============================================================================

package com.nexusai.companion.aspect;

import com.nexusai.companion.config.CompanionMetrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect pour capturer automatiquement les métriques des services.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class MetricsAspect {
    
    private final CompanionMetrics metrics;
    
    /**
     * Pointcut pour tous les services.
     */
    @Pointcut("within(com.nexusai.companion.service..*)")
    public void serviceLayer() {}
    
    /**
     * Capture les métriques de création de compagnons.
     */
    @Around("execution(* com.nexusai.companion.service.CompanionService.createCompanion(..))")
    public Object trackCompanionCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = metrics.startCreationTimer();
        
        try {
            Object result = joinPoint.proceed();
            metrics.incrementCompanionCreated();
            metrics.recordCreation(sample);
            return result;
        } catch (Exception e) {
            log.error("Erreur lors de la création: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Capture les métriques d'évolution.
     */
    @Around("execution(* com.nexusai.companion.service.EvolutionService.evolveCompanion(..))")
    public Object trackEvolution(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = metrics.startEvolutionTimer();
        
        try {
            Object result = joinPoint.proceed();
            metrics.incrementCompanionEvolved();
            metrics.recordEvolution(sample);
            return result;
        } catch (Exception e) {
            log.error("Erreur lors de l'évolution: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Capture les métriques de fusion.
     */
    @Around("execution(* com.nexusai.companion.service.EvolutionService.mergeCompanions(..))")
    public Object trackMerge(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = metrics.startMergeTimer();
        
        try {
            Object result = joinPoint.proceed();
            metrics.incrementCompanionMerged();
            metrics.recordMerge(sample);
            return result;
        } catch (Exception e) {
            log.error("Erreur lors de la fusion: {}", e.getMessage());
            throw e;
        }
    }
}

/**
 * Aspect pour le logging structuré.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {
    
    @Around("within(com.nexusai.companion.controller..*)")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.info(">>> Appel: {}.{}", className, methodName);
        
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            
            log.info("<<< Retour: {}.{} - {}ms", className, methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("!!! Erreur: {}.{} - {}ms - {}", 
                     className, methodName, duration, e.getMessage());
            throw e;
        }
    }
}

// ============================================================================
// FICHIER: logback-spring.xml
// Description: Configuration Logback pour ELK Stack
// ============================================================================

/**
 * logback-spring.xml
 * Configuration du logging avec format JSON pour Elasticsearch
 */

<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- Console Appender (développement) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- JSON Appender (production - pour ELK) -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"companion-service","environment":"${SPRING_PROFILES_ACTIVE}"}</customFields>
        </encoder>
    </appender>
    
    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/companion-service.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/companion-service.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy 
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Loggers -->
    <logger name="com.nexusai.companion" level="DEBUG"/>
    <logger name="org.springframework.data.mongodb" level="INFO"/>
    <logger name="org.springframework.kafka" level="INFO"/>
    <logger name="org.mongodb.driver" level="WARN"/>
    
    <!-- Root Logger -->
    <springProfile name="dev,local">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
    
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="JSON"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
    
</configuration>