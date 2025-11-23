package com.nexusai.moderation.repository;

import com.nexusai.moderation.entity.ContentFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContentFlagRepository extends JpaRepository<ContentFlag, UUID> {

    Page<ContentFlag> findByStatus(String status, Pageable pageable);

    Page<ContentFlag> findByContentTypeAndContentId(String contentType, UUID contentId, Pageable pageable);

    Page<ContentFlag> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);

    @Query("SELECT cf FROM ContentFlag cf WHERE cf.status = 'pending' ORDER BY " +
           "CASE cf.severity WHEN 'critical' THEN 1 WHEN 'high' THEN 2 WHEN 'medium' THEN 3 ELSE 4 END, " +
           "cf.createdAt ASC")
    Page<ContentFlag> findPendingOrderedByPriority(Pageable pageable);

    @Query("SELECT COUNT(cf) FROM ContentFlag cf WHERE cf.status = 'pending' AND cf.severity = :severity")
    long countPendingBySeverity(@Param("severity") String severity);

    @Query("SELECT cf FROM ContentFlag cf WHERE cf.contentType = :contentType AND cf.contentId = :contentId " +
           "AND cf.status = 'pending'")
    List<ContentFlag> findPendingForContent(@Param("contentType") String contentType,
                                             @Param("contentId") UUID contentId);

    @Query("SELECT COUNT(cf) FROM ContentFlag cf WHERE cf.reporterId = :reporterId " +
           "AND cf.createdAt >= :since")
    long countRecentFlagsByReporter(@Param("reporterId") UUID reporterId,
                                    @Param("since") LocalDateTime since);
}
