package com.nexusai.core.repository;

import com.nexusai.core.entity.Conversation;
import com.nexusai.core.enums.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findByUserIdAndStatusOrderByLastActivityAtDesc(
            UUID userId, ConversationStatus status, Pageable pageable);

    List<Conversation> findByUserIdAndCompanionIdAndStatus(
            UUID userId, UUID companionId, ConversationStatus status);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId " +
           "AND c.status = 'ACTIVE' ORDER BY c.lastActivityAt DESC")
    List<Conversation> findRecentConversations(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Conversation c SET c.lastActivityAt = :activityTime, " +
           "c.messageCount = c.messageCount + 1 WHERE c.id = :conversationId")
    void incrementMessageCount(@Param("conversationId") UUID conversationId,
                               @Param("activityTime") LocalDateTime activityTime);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    long countActiveByUserId(@Param("userId") UUID userId);
}
