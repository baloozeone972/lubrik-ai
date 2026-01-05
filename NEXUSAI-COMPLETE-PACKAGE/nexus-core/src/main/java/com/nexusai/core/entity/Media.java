package com.nexusai.core.entity;

import com.nexusai.core.enums.MediaCategory;
import com.nexusai.core.enums.MediaStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un fichier média (image, audio, vidéo).
 */
@Entity
@Table(name = "media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String storageKey;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MediaStatus status = MediaStatus.ACTIVE;

    @Column(name = "public_url")
    private String publicUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration")
    private Integer duration; // En secondes pour audio/vidéo

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Méthodes utilitaires
    public boolean isImage() {
        return category == MediaCategory.IMAGE || category == MediaCategory.AVATAR;
    }

    public boolean isAudio() {
        return category == MediaCategory.AUDIO;
    }

    public boolean isVideo() {
        return category == MediaCategory.VIDEO;
    }

    public void markAsDeleted() {
        this.status = MediaStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }
}
