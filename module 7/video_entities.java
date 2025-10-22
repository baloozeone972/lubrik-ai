package com.nexusai.video.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant une vidéo générée.
 * 
 * Cette entité stocke toutes les informations relatives à une génération vidéo,
 * incluant les métadonnées, le statut, et les références vers les assets.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Entity
@Table(name = "generated_videos", indexes = {
    @Index(name = "idx_videos_user", columnList = "user_id"),
    @Index(name = "idx_videos_status", columnList = "status"),
    @Index(name = "idx_videos_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedVideo {

    /**
     * Identifiant unique de la vidéo
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Identifiant de l'utilisateur propriétaire
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Identifiant du compagnon associé
     */
    @Column(name = "companion_id")
    private String companionId;

    /**
     * Prompt utilisateur pour la génération
     */
    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    /**
     * Scénario généré au format JSON
     */
    @Column(name = "scenario_json", nullable = false, columnDefinition = "JSONB")
    private String scenarioJson;

    /**
     * Durée de la vidéo en secondes
     */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    /**
     * Résolution vidéo (ex: "1080p", "4K")
     */
    @Column(name = "resolution", length = 20)
    private String resolution;

    /**
     * Images par seconde (30, 60, etc.)
     */
    @Column(name = "frame_rate")
    private Integer frameRate;

    /**
     * Qualité de la vidéo (STANDARD, HD, ULTRA)
     */
    @Column(name = "quality", length = 20)
    @Enumerated(EnumType.STRING)
    private VideoQuality quality;

    /**
     * Statut actuel de la génération
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VideoStatus status = VideoStatus.QUEUED;

    /**
     * Phase actuelle du pipeline
     */
    @Column(name = "current_phase", length = 50)
    @Enumerated(EnumType.STRING)
    private VideoPhase currentPhase;

    /**
     * Pourcentage de progression (0-100)
     */
    @Column(name = "progress_percentage")
    @Builder.Default
    private Integer progressPercentage = 0;

    /**
     * URL de stockage de la vidéo finale
     */
    @Column(name = "storage_url", columnDefinition = "TEXT")
    private String storageUrl;

    /**
     * URLs des vignettes
     */
    @Column(name = "thumbnail_urls", columnDefinition = "TEXT[]")
    private String[] thumbnailUrls;

    /**
     * Taille du fichier en Mo
     */
    @Column(name = "file_size_mb", precision = 10, scale = 2)
    private BigDecimal fileSizeMb;

    /**
     * Temps de génération en minutes
     */
    @Column(name = "generation_time_minutes")
    private Integer generationTimeMinutes;

    /**
     * Marquer comme favori
     */
    @Column(name = "is_favorite")
    @Builder.Default
    private Boolean isFavorite = false;

    /**
     * Coût en jetons pour cette génération
     */
    @Column(name = "tokens_cost", nullable = false)
    private Integer tokensCost;

    /**
     * Message d'erreur en cas d'échec
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Assets associés à cette vidéo
     */
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VideoAsset> assets = new ArrayList<>();

    /**
     * Date de création
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Date de complétion
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Date de dernière mise à jour
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Énumération des statuts possibles
     */
    public enum VideoStatus {
        QUEUED,          // En file d'attente
        PROCESSING,      // En cours de traitement
        COMPLETED,       // Terminé avec succès
        FAILED,          // Échec
        CANCELLED        // Annulé par l'utilisateur
    }

    /**
     * Énumération des phases du pipeline
     */
    public enum VideoPhase {
        SCRIPT_GENERATION,    // Génération du scénario
        ASSET_GENERATION,     // Génération des assets (images, audio)
        COMPOSITING,          // Composition des scènes
        RENDERING,            // Rendu vidéo
        ENCODING,             // Encodage final
        FINALIZATION          // Upload et finalisation
    }

    /**
     * Énumération des qualités vidéo
     */
    public enum VideoQuality {
        STANDARD,   // 1080p 30fps
        HD,         // 1080p 60fps
        ULTRA       // 4K 60fps HDR
    }

    /**
     * Méthode helper pour ajouter un asset
     */
    public void addAsset(VideoAsset asset) {
        assets.add(asset);
        asset.setVideo(this);
    }

    /**
     * Méthode helper pour mettre à jour la progression
     */
    public void updateProgress(VideoPhase phase, int percentage) {
        this.currentPhase = phase;
        this.progressPercentage = percentage;
    }

    /**
     * Méthode helper pour marquer comme terminé
     */
    public void markAsCompleted(String storageUrl, String[] thumbnails, BigDecimal fileSize) {
        this.status = VideoStatus.COMPLETED;
        this.storageUrl = storageUrl;
        this.thumbnailUrls = thumbnails;
        this.fileSizeMb = fileSize;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100;
    }

    /**
     * Méthode helper pour marquer comme échoué
     */
    public void markAsFailed(String errorMessage) {
        this.status = VideoStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}

/**
 * Entité représentant un asset vidéo (image, audio, etc.)
 * 
 * Les assets sont les composants individuels utilisés pour construire la vidéo finale.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Entity
@Table(name = "video_assets", indexes = {
    @Index(name = "idx_video_assets_video", columnList = "video_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Vidéo parente
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private GeneratedVideo video;

    /**
     * Type d'asset
     */
    @Column(name = "asset_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    /**
     * Numéro de scène
     */
    @Column(name = "scene_number")
    private Integer sceneNumber;

    /**
     * URL de stockage de l'asset
     */
    @Column(name = "storage_url", nullable = false, columnDefinition = "TEXT")
    private String storageUrl;

    /**
     * Métadonnées additionnelles au format JSON
     */
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    /**
     * Date de création
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Types d'assets possibles
     */
    public enum AssetType {
        BACKGROUND,     // Image de fond
        CHARACTER,      // Render du personnage
        AUDIO_VOICE,    // Piste audio voix
        AUDIO_MUSIC,    // Piste audio musique
        AUDIO_SFX,      // Effets sonores
        SUBTITLE        // Sous-titres
    }
}
