package com.nexusai.core.entity;

import com.nexusai.core.enums.ContentType;
import com.nexusai.core.enums.ModerationStatus;
import com.nexusai.core.enums.ModerationAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une action de modération de contenu.
 */
@Entity
@Table(name = "content_moderation", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_content_id", columnList = "content_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentModeration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ModerationStatus status = ModerationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken")
    private ModerationAction actionTaken;

    @Column(name = "flagged_categories", columnDefinition = "TEXT")
    private String flaggedCategories; // JSON array

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "is_automatic")
    @Builder.Default
    private Boolean isAutomatic = true;

    @Column(name = "moderator_id")
    private UUID moderatorId;

    @Column(name = "moderator_notes", columnDefinition = "TEXT")
    private String moderatorNotes;

    @Column(name = "report_count")
    @Builder.Default
    private Integer reportCount = 0;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isAutoApproved() {
        return status == ModerationStatus.APPROVED && isAutomatic;
    }

    public boolean needsHumanReview() {
        return status == ModerationStatus.FLAGGED || 
               (status == ModerationStatus.PENDING && confidenceScore != null && confidenceScore < 0.7);
    }

    public void approve(UUID moderatorId, String notes) {
        this.status = ModerationStatus.APPROVED;
        this.moderatorId = moderatorId;
        this.moderatorNotes = notes;
        this.reviewedAt = LocalDateTime.now();
        this.isAutomatic = false;
    }

    public void reject(UUID moderatorId, String notes, ModerationAction action) {
        this.status = ModerationStatus.REJECTED;
        this.moderatorId = moderatorId;
        this.moderatorNotes = notes;
        this.actionTaken = action;
        this.reviewedAt = LocalDateTime.now();
        this.isAutomatic = false;
    }

    public void incrementReportCount() {
        this.reportCount++;
        if (this.reportCount >= 3 && this.status == ModerationStatus.APPROVED) {
            this.status = ModerationStatus.FLAGGED;
        }
    }
}
