-- ═══════════════════════════════════════════════════════════════
-- MODULE 10 : ANALYTICS & MONITORING - SCHÉMAS CLICKHOUSE
-- 
-- Scripts SQL pour créer les tables ClickHouse optimisées
-- pour le stockage et l'analyse de grandes quantités de données.
-- 
-- ClickHouse est une base de données columnaire optimisée pour
-- les requêtes analytiques OLAP (Online Analytical Processing).
-- ═══════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════
-- 1. CRÉATION DE LA BASE DE DONNÉES
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS nexusai;

USE nexusai;

-- ═══════════════════════════════════════════════════════════════
-- 2. TABLE: user_events - Événements utilisateur
-- ═══════════════════════════════════════════════════════════════

/**
 * Table principale pour les événements utilisateur.
 * 
 * Moteur : MergeTree
 * - Optimisé pour les INSERT rapides et les requêtes analytiques
 * - Support du partitionnement par mois
 * - Index primaire sur (user_id, timestamp)
 * 
 * Partition : Par mois (toYYYYMM(timestamp))
 * - Permet une suppression rapide des anciennes données
 * - Optimise les requêtes sur des périodes spécifiques
 * 
 * TTL : 90 jours
 * - Suppression automatique des données anciennes
 */
CREATE TABLE IF NOT EXISTS user_events (
    -- Identifiants
    event_id String,
    user_id String,
    session_id Nullable(String),
    
    -- Type et données
    event_type String,
    event_data String, -- JSON
    
    -- Horodatage
    timestamp DateTime DEFAULT now(),
    
    -- Contexte
    device_type Nullable(String),
    platform Nullable(String),
    app_version Nullable(String),
    
    -- Localisation
    ip_address Nullable(String),
    country Nullable(String),
    city Nullable(String),
    
    -- Métadonnées pour partitionnement
    event_date Date DEFAULT toDate(timestamp)
    
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (user_id, timestamp)
TTL timestamp + INTERVAL 90 DAY
SETTINGS index_granularity = 8192;

-- Index secondaires pour optimiser les requêtes
ALTER TABLE user_events 
    ADD INDEX idx_event_type event_type TYPE set(100) GRANULARITY 4;

ALTER TABLE user_events 
    ADD INDEX idx_session session_id TYPE bloom_filter() GRANULARITY 4;

-- ═══════════════════════════════════════════════════════════════
-- 3. TABLE: system_metrics - Métriques système
-- ═══════════════════════════════════════════════════════════════

/**
 * Table pour les métriques système (CPU, mémoire, latence, etc.)
 * 
 * Moteur : MergeTree
 * Partition : Par mois
 * TTL : 365 jours (1 an)
 */
CREATE TABLE IF NOT EXISTS system_metrics (
    -- Nom et valeur de la métrique
    metric_name String,
    metric_value Float64,
    
    -- Tags (JSON)
    tags String,
    
    -- Horodatage
    timestamp DateTime DEFAULT now(),
    
    -- Service source
    service_name String,
    instance_id Nullable(String),
    
    -- Métadonnées
    metric_date Date DEFAULT toDate(timestamp)
    
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)
ORDER BY (metric_name, service_name, timestamp)
TTL timestamp + INTERVAL 365 DAY
SETTINGS index_granularity = 8192;

-- Index pour optimiser les requêtes par nom de métrique
ALTER TABLE system_metrics 
    ADD INDEX idx_metric_name metric_name TYPE set(100) GRANULARITY 4;

-- ═══════════════════════════════════════════════════════════════
-- 4. TABLE: aggregated_metrics - Métriques agrégées
-- ═══════════════════════════════════════════════════════════════

/**
 * Table pour les métriques pré-agrégées.
 * 
 * Utilisée pour accélérer les requêtes sur les dashboards.
 * Alimentée par un job d'agrégation périodique.
 */
CREATE TABLE IF NOT EXISTS aggregated_metrics (
    -- Type de métrique
    metric_type String,
    
    -- Période d'agrégation
    period Enum8(
        'MINUTE' = 1,
        'HOUR' = 2,
        'DAY' = 3,
        'WEEK' = 4,
        'MONTH' = 5,
        'YEAR' = 6
    ),
    
    -- Période couverte
    period_start DateTime,
    period_end DateTime,
    
    -- Statistiques
    count UInt64,
    min_value Nullable(Float64),
    max_value Nullable(Float64),
    avg_value Nullable(Float64),
    median_value Nullable(Float64),
    p95_value Nullable(Float64),
    p99_value Nullable(Float64),
    
    -- Tags
    tags String, -- JSON
    
    -- Métadonnées
    created_at DateTime DEFAULT now()
    
) ENGINE = MergeTree()
PARTITION BY (period, toYYYYMM(period_start))
ORDER BY (metric_type, period, period_start)
TTL period_start + INTERVAL 730 DAY
SETTINGS index_granularity = 8192;

-- ═══════════════════════════════════════════════════════════════
-- 5. TABLE: reports - Rapports générés
-- ═══════════════════════════════════════════════════════════════

/**
 * Table pour les rapports générés.
 */
CREATE TABLE IF NOT EXISTS reports (
    -- Identifiant
    report_id String,
    
    -- Type et format
    report_type Enum8(
        'DAILY' = 1,
        'WEEKLY' = 2,
        'MONTHLY' = 3,
        'QUARTERLY' = 4,
        'YEARLY' = 5,
        'CUSTOM' = 6
    ),
    report_format Enum8(
        'JSON' = 1,
        'PDF' = 2,
        'EXCEL' = 3,
        'CSV' = 4
    ),
    
    -- Métadonnées
    title String,
    description Nullable(String),
    
    -- Période couverte
    period_start DateTime,
    period_end DateTime,
    
    -- Données du rapport (JSON)
    report_data String,
    
    -- Stockage
    storage_url Nullable(String),
    
    -- Statut
    status Enum8(
        'PENDING' = 1,
        'GENERATING' = 2,
        'COMPLETED' = 3,
        'FAILED' = 4
    ),
    
    -- Dates
    generated_at Nullable(DateTime),
    generated_by String,
    created_at DateTime DEFAULT now()
    
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (report_type, created_at)
TTL created_at + INTERVAL 730 DAY
SETTINGS index_granularity = 8192;

-- ═══════════════════════════════════════════════════════════════
-- 6. TABLE: alerts - Alertes système
-- ═══════════════════════════════════════════════════════════════

/**
 * Table pour les alertes déclenchées.
 */
CREATE TABLE IF NOT EXISTS alerts (
    -- Identifiant
    alert_id String,
    
    -- Informations
    alert_name String,
    severity Enum8('INFO' = 1, 'WARNING' = 2, 'CRITICAL' = 3),
    message String,
    description Nullable(String),
    
    -- Service concerné
    service_name String,
    
    -- Métrique déclencheuse
    trigger_metric Nullable(String),
    trigger_value Nullable(Float64),
    threshold Nullable(Float64),
    
    -- Dates
    triggered_at DateTime,
    resolved_at Nullable(DateTime),
    
    -- Statut
    status Enum8(
        'ACTIVE' = 1,
        'ACKNOWLEDGED' = 2,
        'RESOLVED' = 3,
        'SILENCED' = 4
    ),
    
    -- Reconnaissance
    acknowledged_by Nullable(String),
    
    -- Labels (JSON)
    labels String
    
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(triggered_at)
ORDER BY (service_name, triggered_at)
TTL triggered_at + INTERVAL 365 DAY
SETTINGS index_granularity = 8192;

-- ═══════════════════════════════════════════════════════════════
-- 7. VUES MATÉRIALISÉES - Pour agrégations en temps réel
-- ═══════════════════════════════════════════════════════════════

/**
 * Vue matérialisée : Événements par heure
 * 
 * Calcule automatiquement le nombre d'événements par heure
 * à chaque insertion dans user_events.
 */
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_events_by_hour
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(event_hour)
ORDER BY (event_type, event_hour)
AS SELECT
    event_type,
    toStartOfHour(timestamp) AS event_hour,
    count() AS event_count
FROM user_events
GROUP BY event_type, event_hour;

/**
 * Vue matérialisée : Événements par utilisateur et jour
 */
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_events_by_user_day
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(event_date)
ORDER BY (user_id, event_date)
AS SELECT
    user_id,
    toDate(timestamp) AS event_date,
    count() AS event_count,
    uniq(session_id) AS session_count
FROM user_events
GROUP BY user_id, event_date;

/**
 * Vue matérialisée : Métriques système moyennes par heure
 */
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_metrics_hourly
ENGINE = AggregatingMergeTree()
PARTITION BY toYYYYMM(metric_hour)
ORDER BY (metric_name, service_name, metric_hour)
AS SELECT
    metric_name,
    service_name,
    toStartOfHour(timestamp) AS metric_hour,
    count() AS count,
    min(metric_value) AS min_value,
    max(metric_value) AS max_value,
    avg(metric_value) AS avg_value,
    quantile(0.95)(metric_value) AS p95_value
FROM system_metrics
GROUP BY metric_name, service_name, metric_hour;

-- ═══════════════════════════════════════════════════════════════
-- 8. REQUÊTES UTILES - Exemples de requêtes optimisées
-- ═══════════════════════════════════════════════════════════════

-- ───────────────────────────────────────────────────────────────
-- Top 10 des événements les plus fréquents (dernières 24h)
-- ───────────────────────────────────────────────────────────────
SELECT 
    event_type,
    count() AS event_count
FROM user_events
WHERE timestamp >= now() - INTERVAL 24 HOUR
GROUP BY event_type
ORDER BY event_count DESC
LIMIT 10;

-- ───────────────────────────────────────────────────────────────
-- Utilisateurs les plus actifs (derniers 7 jours)
-- ───────────────────────────────────────────────────────────────
SELECT 
    user_id,
    count() AS event_count,
    uniq(session_id) AS session_count
FROM user_events
WHERE timestamp >= now() - INTERVAL 7 DAY
GROUP BY user_id
ORDER BY event_count DESC
LIMIT 20;

-- ───────────────────────────────────────────────────────────────
-- Statistiques horaires des événements (dernières 24h)
-- ───────────────────────────────────────────────────────────────
SELECT 
    toStartOfHour(timestamp) AS hour,
    count() AS event_count
FROM user_events
WHERE timestamp >= now() - INTERVAL 24 HOUR
GROUP BY hour
ORDER BY hour;

-- ───────────────────────────────────────────────────────────────
-- Statistiques P95/P99 d'une métrique (dernières 24h)
-- ───────────────────────────────────────────────────────────────
SELECT 
    metric_name,
    count() AS count,
    min(metric_value) AS min_value,
    max(metric_value) AS max_value,
    avg(metric_value) AS avg_value,
    quantile(0.50)(metric_value) AS median,
    quantile(0.95)(metric_value) AS p95,
    quantile(0.99)(metric_value) AS p99
FROM system_metrics
WHERE 
    metric_name = 'http_request_duration_seconds'
    AND timestamp >= now() - INTERVAL 24 HOUR
GROUP BY metric_name;

-- ───────────────────────────────────────────────────────────────
-- Tendance d'une métrique sur 7 jours (par heure)
-- ───────────────────────────────────────────────────────────────
SELECT 
    toStartOfHour(timestamp) AS hour,
    avg(metric_value) AS avg_value
FROM system_metrics
WHERE 
    metric_name = 'cpu_usage'
    AND service_name = 'user-service'
    AND timestamp >= now() - INTERVAL 7 DAY
GROUP BY hour
ORDER BY hour;

-- ───────────────────────────────────────────────────────────────
-- Alertes actives par sévérité
-- ───────────────────────────────────────────────────────────────
SELECT 
    severity,
    count() AS alert_count
FROM alerts
WHERE 
    status = 'ACTIVE'
    AND triggered_at >= now() - INTERVAL 24 HOUR
GROUP BY severity
ORDER BY severity DESC;

-- ═══════════════════════════════════════════════════════════════
-- 9. OPTIMISATIONS & MAINTENANCE
-- ═══════════════════════════════════════════════════════════════

-- ───────────────────────────────────────────────────────────────
-- Optimiser les tables (fusionner les parts)
-- À exécuter périodiquement
-- ───────────────────────────────────────────────────────────────
OPTIMIZE TABLE user_events FINAL;
OPTIMIZE TABLE system_metrics FINAL;
OPTIMIZE TABLE aggregated_metrics FINAL;

-- ───────────────────────────────────────────────────────────────
-- Supprimer les anciennes partitions manuellement (si TTL non configuré)
-- ───────────────────────────────────────────────────────────────
-- Exemple: Supprimer les événements de janvier 2024
-- ALTER TABLE user_events DROP PARTITION '202401';

-- ───────────────────────────────────────────────────────────────
-- Vérifier l'utilisation de l'espace disque
-- ───────────────────────────────────────────────────────────────
SELECT 
    table,
    formatReadableSize(sum(bytes)) AS size,
    sum(rows) AS rows,
    max(modification_time) AS latest_modification
FROM system.parts
WHERE active AND database = 'nexusai'
GROUP BY table
ORDER BY sum(bytes) DESC;

-- ───────────────────────────────────────────────────────────────
-- Statistiques des requêtes (les plus lentes)
-- ───────────────────────────────────────────────────────────────
SELECT 
    query_duration_ms,
    query,
    event_time
FROM system.query_log
WHERE type = 'QueryFinish'
  AND query_duration_ms > 1000
ORDER BY query_duration_ms DESC
LIMIT 10;

-- ═══════════════════════════════════════════════════════════════
-- FIN DES SCHÉMAS CLICKHOUSE
-- ═══════════════════════════════════════════════════════════════
