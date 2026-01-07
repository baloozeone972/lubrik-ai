package com.nexusai.core.repository;

import com.nexusai.core.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for AnalyticsEvent entities.
 * Provides methods for querying analytics data.
 */
@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    // ========== BASIC QUERIES ==========

    List<AnalyticsEvent> findByUserIdOrderByTimestampDesc(UUID userId);

    List<AnalyticsEvent> findByUserIdAndTimestampBetween(
            UUID userId,
            LocalDateTime start,
            LocalDateTime end);

    List<AnalyticsEvent> findByEventTypeAndTimestampBetween(
            String eventType,
            LocalDateTime start,
            LocalDateTime end);

    // ========== COUNT QUERIES ==========

    long countByTimestampBetween(
            LocalDateTime start,
            LocalDateTime end);

    long countByEventTypeAndTimestampBetween(
            String eventType,
            LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM AnalyticsEvent e " +
            "WHERE e.timestamp BETWEEN :start AND :end")
    long countDistinctUsersByTimestampBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ========== AGGREGATION QUERIES ==========

    @Query("SELECT e.eventType as type, COUNT(e) as count FROM AnalyticsEvent e " +
            "WHERE e.timestamp BETWEEN :start AND :end " +
            "GROUP BY e.eventType")
    List<Map<String, Object>> countEventsByType(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT e.eventCategory as category, COUNT(e) as count FROM AnalyticsEvent e " +
            "WHERE e.userId = :userId AND e.timestamp BETWEEN :start AND :end " +
            "GROUP BY e.eventCategory")
    List<Map<String, Object>> countUserEventsByCategory(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // ========== TOP QUERIES ==========

    @Query(value = "SELECT event_type as type, COUNT(*) as count " +
            "FROM analytics_events " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "GROUP BY event_type " +
            "ORDER BY count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Map<String, Object>> findTopEventTypes(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit);

    @Query(value = "SELECT user_id, COUNT(*) as count " +
            "FROM analytics_events " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "GROUP BY user_id " +
            "ORDER BY count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Map<String, Object>> findTopUsers(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit);

    // ========== TIME-BASED QUERIES ==========

    @Query(value = "SELECT DATE_TRUNC('hour', timestamp) as hour, COUNT(*) as count " +
            "FROM analytics_events " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "GROUP BY hour " +
            "ORDER BY hour", nativeQuery = true)
    List<Map<String, Object>> countEventsByHour(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = "SELECT DATE_TRUNC('day', timestamp) as day, COUNT(*) as count " +
            "FROM analytics_events " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "GROUP BY day " +
            "ORDER BY day", nativeQuery = true)
    List<Map<String, Object>> countEventsByDay(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
