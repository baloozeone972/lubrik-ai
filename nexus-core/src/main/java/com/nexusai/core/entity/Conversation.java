package com.nexusai.core.entity;

import com.nexusai.core.enums.ConversationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Conversation entity representing a chat session.
 */
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversations_user", columnList = "user_id"),
    @Index(name = "idx_conversations_companion", columnList = "companion_id"),
    @Index(name = "idx_conversations_last_activity", columnList = "last_activity_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "companion_id", nullable = false)
    private UUID companionId;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "message_count")
    @Builder.Default
    private Integer messageCount = 0;

    @Column(name = "total_tokens")
    @Builder.Default
    private Long totalTokens = 0L;

    @Column(name = "context_summary", columnDefinition = "text")
    private String contextSummary;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /**
     * Updates the last activity timestamp.
     */
    public void touch() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * Increments the message count.
     */
    public void incrementMessages() {
        this.messageCount++;
        touch();
    }

    /**
     * Adds tokens to the total.
     */
    public void addTokens(long tokens) {
        this.totalTokens += tokens;
    }
}
