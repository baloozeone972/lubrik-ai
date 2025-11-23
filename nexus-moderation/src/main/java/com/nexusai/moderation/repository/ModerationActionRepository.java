package com.nexusai.moderation.repository;

import com.nexusai.moderation.entity.ModerationAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    Page<ModerationAction> findByTargetTypeAndIsActiveTrue(String targetType, Pageable pageable);

    Page<ModerationAction> findByTargetIdOrderByCreatedAtDesc(UUID targetId, Pageable pageable);

    List<ModerationAction> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime now);

    @Query("SELECT ma FROM ModerationAction ma WHERE ma.targetType = :targetType " +
           "AND ma.targetId = :targetId AND ma.actionType = :actionType AND ma.isActive = true")
    Optional<ModerationAction> findActiveAction(@Param("targetType") String targetType,
                                                 @Param("targetId") UUID targetId,
                                                 @Param("actionType") String actionType);

    @Query("SELECT COUNT(ma) FROM ModerationAction ma WHERE ma.targetId = :targetId " +
           "AND ma.actionType = 'warn' AND ma.createdAt >= :since")
    long countRecentWarnings(@Param("targetId") UUID targetId, @Param("since") LocalDateTime since);

    @Query("SELECT ma FROM ModerationAction ma WHERE ma.moderatorId = :moderatorId " +
           "ORDER BY ma.createdAt DESC")
    Page<ModerationAction> findByModeratorId(@Param("moderatorId") UUID moderatorId, Pageable pageable);

    @Query("SELECT COUNT(ma) FROM ModerationAction ma WHERE ma.actionType = :actionType " +
           "AND ma.createdAt >= :since")
    long countActionsByType(@Param("actionType") String actionType, @Param("since") LocalDateTime since);
}
