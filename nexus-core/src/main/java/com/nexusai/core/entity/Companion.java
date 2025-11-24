package com.nexusai.core.entity;

import com.nexusai.core.enums.CompanionStyle;
import com.nexusai.core.enums.CompanionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

/**
 * AI Companion entity.
 */
@Entity
@Table(name = "companions", indexes = {
    @Index(name = "idx_companions_user", columnList = "user_id"),
    @Index(name = "idx_companions_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Companion extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "style", nullable = false)
    @Builder.Default
    private CompanionStyle style = CompanionStyle.REALISTIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CompanionStatus status = CompanionStatus.ACTIVE;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "personality_traits", columnDefinition = "jsonb")
    private String personalityTraits;

    @Column(name = "appearance_config", columnDefinition = "jsonb")
    private String appearanceConfig;

    @Column(name = "voice_config", columnDefinition = "jsonb")
    private String voiceConfig;

    @Column(name = "system_prompt", columnDefinition = "text")
    private String systemPrompt;

    @Column(name = "model_provider", length = 50)
    @Builder.Default
    private String modelProvider = "ollama";

    @Column(name = "model_name", length = 100)
    @Builder.Default
    private String modelName = "llama3";

    @Column(name = "total_messages")
    @Builder.Default
    private Long totalMessages = 0L;

    @Column(name = "total_tokens_used")
    @Builder.Default
    private Long totalTokensUsed = 0L;

    @Column(name = "likes_count")
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Increments the message count.
     */
    public void incrementMessages() {
        this.totalMessages++;
    }

    /**
     * Adds tokens used.
     */
    public void addTokensUsed(long tokens) {
        this.totalTokensUsed += tokens;
    }

    /**
     * Increments like count.
     */
    public void incrementLikes() {
        this.likesCount++;
    }

    /**
     * Decrements like count.
     */
    public void decrementLikes() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }
}
