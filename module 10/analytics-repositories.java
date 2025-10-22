package com.nexusai.analytics.core.repository;

import com.nexusai.analytics.core.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════
 * ANALYTICS REPOSITORIES - Accès aux données ClickHouse
 * 
 * Repositories pour l'accès aux données stockées dans ClickHouse.
 * Utilise JdbcTemplate pour des requêtes SQL optimisées.
 * ═══════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════
// 1. EVENT REPOSITORY - Gestion des événements
// ═══════════════════════════════════════════════════════════════

/**
 * Repository pour la gestion des événements utilisateur dans ClickHouse.
 * 
 * ClickHouse est optimisé pour l'insertion et l'analyse de grandes quantités
 * de données time-series. Toutes les requêtes sont optimisées pour la performance.
 * 
 * @author Équipe Analytics - Sous-équipe Data Access
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EventRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Sauvegarde un événement dans ClickHouse.
     * 
     * @param event L'événement à sauvegarder
     * @return L'événement sauvegardé
     */
    public UserEvent save(UserEvent event) {
        String sql = """
            INSERT INTO user_events (
                event_id, user_id, event_type, event_data,
                timestamp, session_id, device_type, platform,
                app_version, ip_address, country, city
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.update(sql,
            event.getEventId().toString(),
            event.getUserId().toString(),
            event.getEventType(),
            convertMapToJson(event.getEventData()),
            event.getTimestamp(),
            event.getSessionId() != null ? event.getSessionId().toString() : null,
            event.getDeviceType(),
            event.getPlatform(),
            event.getAppVersion(),
            event.getIpAddress(),
            event.getCountry(),
            event.getCity()
        );
        
        return event;
    }
    
    /**
     * Sauvegarde plusieurs événements en batch.
     * Plus performant pour l'insertion en masse.
     * 
     * @param events Liste des événements
     * @return Nombre d'événements insérés
     */
    public int saveBatch(List<UserEvent> events) {
        String sql = """
            INSERT INTO user_events (
                event_id, user_id, event_type, event_data,
                timestamp, session_id, device_type, platform,
                app_version, ip_address, country, city
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        List<Object[]> batchArgs = events.stream()
            .map(event -> new Object[]{
                event.getEventId().toString(),
                event.getUserId().toString(),
                event.getEventType(),
                convertMapToJson(event.getEventData()),
                event.getTimestamp(),
                event.getSessionId() != null ? event.getSessionId().toString() : null,
                event.getDeviceType(),
                event.getPlatform(),
                event.getAppVersion(),
                event.getIpAddress(),
                event.getCountry(),
                event.getCity()
            })
            .toList();
        
        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        return Arrays.stream(results).sum();
    }
    
    /**
     * Trouve les événements d'un utilisateur sur une période.
     * 
     * @param userId ID de l'utilisateur
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Liste des événements
     */
    public List<UserEvent> findByUserIdAndTimestampBetween(UUID userId, 
                                                            Instant startTime, 
                                                            Instant endTime) {
        String sql = """
            SELECT * FROM user_events
            WHERE user_id = ?
            AND timestamp BETWEEN ? AND ?
            ORDER BY timestamp DESC
            """;
        
        return jdbcTemplate.query(sql, new UserEventRowMapper(),
            userId.toString(), startTime, endTime);
    }
    
    /**
     * Trouve les événements par type sur une période.
     * 
     * @param eventType Type d'événement
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Liste des événements
     */
    public List<UserEvent> findByEventTypeAndTimestampBetween(String eventType,
                                                                Instant startTime,
                                                                Instant endTime) {
        String sql = """
            SELECT * FROM user_events
            WHERE event_type = ?
            AND timestamp BETWEEN ? AND ?
            ORDER BY timestamp DESC
            LIMIT 1000
            """;
        
        return jdbcTemplate.query(sql, new UserEventRowMapper(),
            eventType, startTime, endTime);
    }
    
    /**
     * Compte les événements d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Nombre d'événements
     */
    public long countByUserIdAndTimestampBetween(UUID userId, 
                                                  Instant startTime, 
                                                  Instant endTime) {
        String sql = """
            SELECT count(*) FROM user_events
            WHERE user_id = ?
            AND timestamp BETWEEN ? AND ?
            """;
        
        Long count = jdbcTemplate.queryForObject(sql, Long.class,
            userId.toString(), startTime, endTime);
        
        return count != null ? count : 0L;
    }
    
    /**
     * Trouve les types d'événements les plus fréquents.
     * 
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @param limit Nombre de résultats
     * @return Map des types d'événements et leur nombre d'occurrences
     */
    public Map<String, Long> findTopEventTypes(Instant startTime, 
                                                Instant endTime, 
                                                int limit) {
        String sql = """
            SELECT event_type, count(*) as count
            FROM user_events
            WHERE timestamp BETWEEN ? AND ?
            GROUP BY event_type
            ORDER BY count DESC
            LIMIT ?
            """;
        
        Map<String, Long> results = new LinkedHashMap<>();
        
        jdbcTemplate.query(sql, rs -> {
            results.put(rs.getString("event_type"), rs.getLong("count"));
        }, startTime, endTime, limit);
        
        return results;
    }
    
    /**
     * Trouve les utilisateurs les plus actifs.
     * 
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @param limit Nombre de résultats
     * @return Map des user IDs et leur nombre d'événements
     */
    public Map<UUID, Long> findTopActiveUsers(Instant startTime, 
                                               Instant endTime, 
                                               int limit) {
        String sql = """
            SELECT user_id, count(*) as count
            FROM user_events
            WHERE timestamp BETWEEN ? AND ?
            GROUP BY user_id
            ORDER BY count DESC
            LIMIT ?
            """;
        
        Map<UUID, Long> results = new LinkedHashMap<>();
        
        jdbcTemplate.query(sql, rs -> {
            UUID userId = UUID.fromString(rs.getString("user_id"));
            results.put(userId, rs.getLong("count"));
        }, startTime, endTime, limit);
        
        return results;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ROW MAPPER
    // ═══════════════════════════════════════════════════════════════
    
    private static class UserEventRowMapper implements RowMapper<UserEvent> {
        @Override
        public UserEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return UserEvent.builder()
                .eventId(UUID.fromString(rs.getString("event_id")))
                .userId(UUID.fromString(rs.getString("user_id")))
                .eventType(rs.getString("event_type"))
                .eventData(parseJsonToMap(rs.getString("event_data")))
                .timestamp(rs.getTimestamp("timestamp").toInstant())
                .sessionId(rs.getString("session_id") != null ? 
                    UUID.fromString(rs.getString("session_id")) : null)
                .deviceType(rs.getString("device_type"))
                .platform(rs.getString("platform"))
                .appVersion(rs.getString("app_version"))
                .ipAddress(rs.getString("ip_address"))
                .country(rs.getString("country"))
                .city(rs.getString("city"))
                .build();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════
    
    private String convertMapToJson(Map<String, Object> map) {
        // Utiliser une bibliothèque JSON comme Jackson
        // Simplifié ici pour l'exemple
        if (map == null || map.isEmpty()) return "{}";
        
        StringBuilder json = new StringBuilder("{");
        map.forEach((key, value) -> {
            json.append("\"").append(key).append("\":")
                .append("\"").append(value).append("\",");
        });
        json.deleteCharAt(json.length() - 1); // Supprimer dernière virgule
        json.append("}");
        
        return json.toString();
    }
    
    private static Map<String, Object> parseJsonToMap(String json) {
        // Utiliser une bibliothèque JSON comme Jackson
        // Simplifié ici pour l'exemple
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return new HashMap<>();
        }
        
        // Parsing simple (à remplacer par Jackson)
        return new HashMap<>();
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. METRIC REPOSITORY - Gestion des métriques
// ═══════════════════════════════════════════════════════════════

/**
 * Repository pour la gestion des métriques système dans ClickHouse.
 * 
 * @author Équipe Analytics - Sous-équipe Data Access
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MetricRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Sauvegarde une métrique.
     * 
     * @param metric La métrique à sauvegarder
     * @return La métrique sauvegardée
     */
    public SystemMetric save(SystemMetric metric) {
        String sql = """
            INSERT INTO system_metrics (
                metric_name, metric_value, tags, timestamp,
                service_name, instance_id
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.update(sql,
            metric.getMetricName(),
            metric.getMetricValue(),
            convertMapToJson(metric.getTags()),
            metric.getTimestamp(),
            metric.getServiceName(),
            metric.getInstanceId()
        );
        
        return metric;
    }
    
    /**
     * Sauvegarde plusieurs métriques en batch.
     * 
     * @param metrics Liste des métriques
     * @return Nombre de métriques insérées
     */
    public int saveBatch(List<SystemMetric> metrics) {
        String sql = """
            INSERT INTO system_metrics (
                metric_name, metric_value, tags, timestamp,
                service_name, instance_id
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        List<Object[]> batchArgs = metrics.stream()
            .map(metric -> new Object[]{
                metric.getMetricName(),
                metric.getMetricValue(),
                convertMapToJson(metric.getTags()),
                metric.getTimestamp(),
                metric.getServiceName(),
                metric.getInstanceId()
            })
            .toList();
        
        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        return Arrays.stream(results).sum();
    }
    
    /**
     * Trouve les métriques d'un service sur une période.
     * 
     * @param serviceName Nom du service
     * @param startTime Début de la période
     * @param endTime Fin de la période
     * @return Liste des métriques
     */
    public List<SystemMetric> findByServiceNameAndTimestampBetween(String serviceName,
                                                                     Instant startTime,
                                                                     Instant endTime) {
        String sql = """
            SELECT * FROM system_metrics
            WHERE service_name = ?
            AND timestamp BETWEEN ? AND ?
            ORDER BY timestamp DESC
            LIMIT 10000
            """;
        
        return jdbcTemplate.query(sql, new SystemMetricRowMapper(),
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
    public MetricStatistics calculateStatistics(String metricName,
                                                 Instant startTime,
                                                 Instant endTime) {
        String sql = """
            SELECT
                count(*) as count,
                min(metric_value) as min_value,
                max(metric_value) as max_value,
                avg(metric_value) as avg_value,
                quantile(0.50)(metric_value) as median_value,
                quantile(0.95)(metric_value) as p95_value,
                quantile(0.99)(metric_value) as p99_value
            FROM system_metrics
            WHERE metric_name = ?
            AND timestamp BETWEEN ? AND ?
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> 
            MetricStatistics.builder()
                .count(rs.getLong("count"))
                .minValue(rs.getDouble("min_value"))
                .maxValue(rs.getDouble("max_value"))
                .avgValue(rs.getDouble("avg_value"))
                .medianValue(rs.getDouble("median_value"))
                .p95Value(rs.getDouble("p95_value"))
                .p99Value(rs.getDouble("p99_value"))
                .build(),
            metricName, startTime, endTime
        );
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
    public List<AggregatedMetric> aggregateByPeriod(String metricName,
                                                     AggregationPeriod period,
                                                     Instant startTime,
                                                     Instant endTime) {
        // Convertir la période en intervalle ClickHouse
        String interval = switch (period) {
            case MINUTE -> "toStartOfMinute(timestamp)";
            case HOUR -> "toStartOfHour(timestamp)";
            case DAY -> "toStartOfDay(timestamp)";
            case WEEK -> "toStartOfWeek(timestamp)";
            case MONTH -> "toStartOfMonth(timestamp)";
            case YEAR -> "toStartOfYear(timestamp)";
        };
        
        String sql = String.format("""
            SELECT
                %s as period_start,
                count(*) as count,
                min(metric_value) as min_value,
                max(metric_value) as max_value,
                avg(metric_value) as avg_value,
                quantile(0.95)(metric_value) as p95_value
            FROM system_metrics
            WHERE metric_name = ?
            AND timestamp BETWEEN ? AND ?
            GROUP BY period_start
            ORDER BY period_start
            """, interval);
        
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            AggregatedMetric.builder()
                .metricType(metricName)
                .period(period)
                .periodStart(rs.getTimestamp("period_start").toInstant())
                .count(rs.getLong("count"))
                .minValue(rs.getDouble("min_value"))
                .maxValue(rs.getDouble("max_value"))
                .avgValue(rs.getDouble("avg_value"))
                .p95Value(rs.getDouble("p95_value"))
                .build(),
            metricName, startTime, endTime
        );
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ROW MAPPER
    // ═══════════════════════════════════════════════════════════════
    
    private static class SystemMetricRowMapper implements RowMapper<SystemMetric> {
        @Override
        public SystemMetric mapRow(ResultSet rs, int rowNum) throws SQLException {
            return SystemMetric.builder()
                .metricName(rs.getString("metric_name"))
                .metricValue(rs.getDouble("metric_value"))
                .tags(parseJsonToMap(rs.getString("tags")))
                .timestamp(rs.getTimestamp("timestamp").toInstant())
                .serviceName(rs.getString("service_name"))
                .instanceId(rs.getString("instance_id"))
                .build();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════
    
    private String convertMapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        
        StringBuilder json = new StringBuilder("{");
        map.forEach((key, value) -> {
            json.append("\"").append(key).append("\":")
                .append("\"").append(value).append("\",");
        });
        json.deleteCharAt(json.length() - 1);
        json.append("}");
        
        return json.toString();
    }
    
    private static Map<String, String> parseJsonToMap(String json) {
        if (json == null || json.isEmpty() || json.equals("{}")) {
            return new HashMap<>();
        }
        return new HashMap<>();
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. AGGREGATED METRIC REPOSITORY
// ═══════════════════════════════════════════════════════════════

/**
 * Repository pour les métriques agrégées.
 * 
 * @author Équipe Analytics - Sous-équipe Data Access
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AggregatedMetricRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public AggregatedMetric save(AggregatedMetric metric) {
        String sql = """
            INSERT INTO aggregated_metrics (
                metric_type, period, period_start, period_end,
                count, min_value, max_value, avg_value, p95_value
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.update(sql,
            metric.getMetricType(),
            metric.getPeriod().name(),
            metric.getPeriodStart(),
            metric.getPeriodEnd(),
            metric.getCount(),
            metric.getMinValue(),
            metric.getMaxValue(),
            metric.getAvgValue(),
            metric.getP95Value()
        );
        
        return metric;
    }
}

// ═══════════════════════════════════════════════════════════════
// 4. CLASSES UTILITAIRES (à déplacer dans service)
// ═══════════════════════════════════════════════════════════════

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
