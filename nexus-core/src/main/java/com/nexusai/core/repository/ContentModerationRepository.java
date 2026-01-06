package com.nexusai.core.repository;

import com.nexusai.core.entity.ContentModeration;
import com.nexusai.core.enums.ContentType;
import com.nexusai.core.enums.ModerationStatus;
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
public interface ContentModerationRepository extends JpaRepository<ContentModeration, UUID> {

    Optional<ContentModeration> findByContentIdAndContentType(UUID contentId, ContentType contentType);

    Page<ContentModeration> findByStatus(ModerationStatus status, Pageable pageable);

    Page<ContentModeration> findByUserIdAndStatus(UUID userId, ModerationStatus status, Pageable pageable);

    @Query("SELECT cm FROM ContentModeration cm WHERE cm.status = 'FLAGGED' ORDER BY cm.reportCount DESC, cm.createdAt ASC")
    Page<ContentModeration> findPendingReviews(Pageable pageable);

    @Query("SELECT COUNT(cm) FROM ContentModeration cm WHERE cm.status = 'FLAGGED'")
    long countPendingReviews();

    List<ContentModeration> findByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);

    @Query("SELECT cm FROM ContentModeration cm WHERE cm.userId = :userId AND cm.status = 'REJECTED' AND cm.createdAt > :since")
    List<ContentModeration> findRecentRejections(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}
