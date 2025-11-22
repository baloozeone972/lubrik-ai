package com.nexusai.core.repository;

import com.nexusai.core.entity.Message;
import com.nexusai.core.enums.MessageRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "ORDER BY m.createdAt DESC LIMIT :limit")
    List<Message> findRecentMessages(@Param("conversationId") UUID conversationId,
                                     @Param("limit") int limit);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND m.role = :role ORDER BY m.createdAt DESC")
    List<Message> findByConversationIdAndRole(@Param("conversationId") UUID conversationId,
                                               @Param("role") MessageRole role,
                                               Pageable pageable);

    @Query("SELECT SUM(m.tokensUsed) FROM Message m WHERE m.conversationId = :conversationId")
    Long sumTokensByConversationId(@Param("conversationId") UUID conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId")
    long countByConversationId(@Param("conversationId") UUID conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> searchInConversation(@Param("conversationId") UUID conversationId,
                                       @Param("query") String query,
                                       Pageable pageable);
}
