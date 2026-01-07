package com.nexusai.analytics.repository;

import com.nexusai.analytics.entity.AnalyticsEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for AnalyticsEvent entities in nexus-analytics module.
 * Uses createdAt field (not timestamp).
 */
@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    // ========== PAGINATED QUERIES ==========

    Page<AnalyticsEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AnalyticsEvent> findByEventTypeAndCreatedAtBetween(
            String eventType,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    // ========== AGGREGATION QUERIES ==========

    @Query("SELECT e.eventType as type, COUNT(e) as count FROM AnalyticsEvent e " +
            "WHERE e.createdAt BETWEEN :from AND :to " +
            "GROUP BY e.eventType")
    List<Map<String, Object>> countEventsByType(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM AnalyticsEvent e " +
            "WHERE e.createdAt BETWEEN :from AND :to")
    long countUniqueUsers(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT e.eventCategory as category, COUNT(e) as count FROM AnalyticsEvent e " +
            "WHERE e.userId = :userId AND e.createdAt BETWEEN :from AND :to " +
            "GROUP BY e.eventCategory")
    List<Map<String, Object>> countUserEventsByCategory(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
