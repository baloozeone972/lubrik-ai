package com.nexusai.moderation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_flags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "flag_reason", nullable = false, length = 100)
    private String flagReason;

    @Column(name = "flag_category", nullable = false, length = 50)
    private String flagCategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    @Builder.Default
    private String severity = "medium";

    @Column(length = 30)
    @Builder.Default
    private String status = "pending";

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(length = 50)
    private String resolution;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
