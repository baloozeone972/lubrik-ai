package com.nexusai.core.entity;

import com.nexusai.core.enums.MessageRole;
import com.nexusai.core.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Message entity representing a single message in a conversation.
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_conversation", columnList = "conversation_id"),
    @Index(name = "idx_messages_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "media_type", length = 50)
    private String mediaType;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "is_edited")
    @Builder.Default
    private Boolean isEdited = false;

    @Column(name = "parent_message_id")
    private UUID parentMessageId;

    /**
     * Checks if this is a user message.
     */
    public boolean isUserMessage() {
        return role == MessageRole.USER;
    }

    /**
     * Checks if this is an AI message.
     */
    public boolean isAiMessage() {
        return role == MessageRole.ASSISTANT;
    }

    /**
     * Checks if this message has media.
     */
    public boolean hasMedia() {
        return mediaUrl != null && !mediaUrl.isBlank();
    }
}
