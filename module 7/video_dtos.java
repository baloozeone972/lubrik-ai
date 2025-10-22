package com.nexusai.video.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexusai.video.domain.entity.GeneratedVideo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO pour la requête de génération vidéo.
 * 
 * Contient tous les paramètres nécessaires pour initier une génération vidéo.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Requête de génération vidéo")
public class VideoGenerationRequestDto {

    /**
     * Prompt décrivant la vidéo souhaitée
     */
    @NotBlank(message = "Le prompt est obligatoire")
    @Size(min = 10, max = 2000, message = "Le prompt doit contenir entre 10 et 2000 caractères")
    @Schema(description = "Description de la vidéo à générer", 
            example = "Une vidéo de mon compagnon me souhaitant joyeux anniversaire dans un jardin fleuri")
    private String prompt;

    /**
     * Identifiant du compagnon (optionnel)
     */
    @Schema(description = "ID du compagnon à inclure dans la vidéo", 
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String companionId;

    /**
     * Durée souhaitée en secondes
     */
    @NotNull(message = "La durée est obligatoire")
    @Min(value = 10, message = "La durée minimale est de 10 secondes")
    @Max(value = 600, message = "La durée maximale est de 600 secondes (10 minutes)")
    @Schema(description = "Durée de la vidéo en secondes", 
            example = "120", 
            minimum = "10", 
            maximum = "600")
    private Integer durationSeconds;

    /**
     * Qualité de la vidéo
     */
    @NotNull(message = "La qualité est obligatoire")
    @Schema(description = "Qualité de la vidéo", 
            allowableValues = {"STANDARD", "HD", "ULTRA"})
    private GeneratedVideo.VideoQuality quality;

    /**
     * Style visuel (optionnel)
     */
    @Schema(description = "Style visuel de la vidéo", 
            example = "REALISTIC", 
            allowableValues = {"REALISTIC", "ANIME", "ARTISTIC", "CINEMATIC"})
    private String visualStyle;

    /**
     * Style musical (optionnel)
     */
    @Schema(description = "Style musical de fond", 
            example = "UPBEAT")
    private String musicStyle;

    /**
     * Éléments spécifiques à inclure (optionnel)
     */
    @Schema(description = "Liste d'éléments à inclure dans la vidéo", 
            example = "[\"ballons\", \"gâteau d'anniversaire\"]")
    private List<String> includeElements;
}

/**
 * DTO pour la réponse initiale de génération vidéo.
 * 
 * Retourné immédiatement après l'acceptation de la requête.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Réponse initiale de génération vidéo")
public class VideoGenerationResponseDto {

    @Schema(description = "ID unique de la vidéo", 
            example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID videoId;

    @Schema(description = "Statut de la génération", 
            example = "QUEUED")
    private GeneratedVideo.VideoStatus status;

    @Schema(description = "Position dans la file d'attente", 
            example = "3")
    private Integer queuePosition;

    @Schema(description = "Temps d'attente estimé en minutes", 
            example = "5")
    private Integer estimatedWaitMinutes;

    @Schema(description = "Coût en jetons", 
            example = "150")
    private Integer tokensCost;

    @Schema(description = "Message d'information", 
            example = "Votre vidéo a été ajoutée à la file d'attente")
    private String message;

    @Schema(description = "Date de création")
    private LocalDateTime createdAt;
}

/**
 * DTO pour les détails complets d'une vidéo.
 * 
 * Contient toutes les informations sur une vidéo générée.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Détails complets d'une vidéo générée")
public class VideoDetailsDto {

    @Schema(description = "ID unique de la vidéo")
    private UUID id;

    @Schema(description = "ID de l'utilisateur propriétaire")
    private UUID userId;

    @Schema(description = "ID du compagnon associé")
    private String companionId;

    @Schema(description = "Prompt original")
    private String prompt;

    @Schema(description = "Durée en secondes")
    private Integer durationSeconds;

    @Schema(description = "Résolution", example = "1080p")
    private String resolution;

    @Schema(description = "Images par seconde", example = "60")
    private Integer frameRate;

    @Schema(description = "Qualité")
    private GeneratedVideo.VideoQuality quality;

    @Schema(description = "Statut actuel")
    private GeneratedVideo.VideoStatus status;

    @Schema(description = "Phase actuelle du pipeline")
    private GeneratedVideo.VideoPhase currentPhase;

    @Schema(description = "Progression en pourcentage (0-100)")
    private Integer progressPercentage;

    @Schema(description = "URL de la vidéo (si disponible)")
    private String storageUrl;

    @Schema(description = "URLs des vignettes")
    private String[] thumbnailUrls;

    @Schema(description = "Taille du fichier en Mo")
    private BigDecimal fileSizeMb;

    @Schema(description = "Temps de génération en minutes")
    private Integer generationTimeMinutes;

    @Schema(description = "Favori")
    private Boolean isFavorite;

    @Schema(description = "Coût en jetons")
    private Integer tokensCost;

    @Schema(description = "Message d'erreur (si échec)")
    private String errorMessage;

    @Schema(description = "Date de création")
    private LocalDateTime createdAt;

    @Schema(description = "Date de complétion")
    private LocalDateTime completedAt;

    /**
     * Factory method pour créer un DTO depuis une entité
     */
    public static VideoDetailsDto fromEntity(GeneratedVideo video) {
        return VideoDetailsDto.builder()
            .id(video.getId())
            .userId(video.getUserId())
            .companionId(video.getCompanionId())
            .prompt(video.getPrompt())
            .durationSeconds(video.getDurationSeconds())
            .resolution(video.getResolution())
            .frameRate(video.getFrameRate())
            .quality(video.getQuality())
            .status(video.getStatus())
            .currentPhase(video.getCurrentPhase())
            .progressPercentage(video.getProgressPercentage())
            .storageUrl(video.getStorageUrl())
            .thumbnailUrls(video.getThumbnailUrls())
            .fileSizeMb(video.getFileSizeMb())
            .generationTimeMinutes(video.getGenerationTimeMinutes())
            .isFavorite(video.getIsFavorite())
            .tokensCost(video.getTokensCost())
            .errorMessage(video.getErrorMessage())
            .createdAt(video.getCreatedAt())
            .completedAt(video.getCompletedAt())
            .build();
    }
}

/**
 * DTO pour une liste paginée de vidéos.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Liste paginée de vidéos")
public class VideoListDto {

    @Schema(description = "Liste des vidéos")
    private List<VideoSummaryDto> videos;

    @Schema(description = "Numéro de la page actuelle", example = "0")
    private Integer currentPage;

    @Schema(description = "Nombre d'éléments par page", example = "20")
    private Integer pageSize;

    @Schema(description = "Nombre total d'éléments", example = "156")
    private Long totalElements;

    @Schema(description = "Nombre total de pages", example = "8")
    private Integer totalPages;

    @Schema(description = "Est la dernière page")
    private Boolean isLast;
}

/**
 * DTO pour un résumé de vidéo (pour les listes).
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Résumé d'une vidéo")
public class VideoSummaryDto {

    @Schema(description = "ID de la vidéo")
    private UUID id;

    @Schema(description = "Prompt (tronqué)")
    private String prompt;

    @Schema(description = "Durée en secondes")
    private Integer durationSeconds;

    @Schema(description = "Qualité")
    private GeneratedVideo.VideoQuality quality;

    @Schema(description = "Statut")
    private GeneratedVideo.VideoStatus status;

    @Schema(description = "Progression")
    private Integer progressPercentage;

    @Schema(description = "URL de la première vignette")
    private String thumbnailUrl;

    @Schema(description = "Favori")
    private Boolean isFavorite;

    @Schema(description = "Date de création")
    private LocalDateTime createdAt;

    /**
     * Factory method pour créer un résumé depuis une entité
     */
    public static VideoSummaryDto fromEntity(GeneratedVideo video) {
        return VideoSummaryDto.builder()
            .id(video.getId())
            .prompt(truncatePrompt(video.getPrompt(), 100))
            .durationSeconds(video.getDurationSeconds())
            .quality(video.getQuality())
            .status(video.getStatus())
            .progressPercentage(video.getProgressPercentage())
            .thumbnailUrl(video.getThumbnailUrls() != null && video.getThumbnailUrls().length > 0 
                ? video.getThumbnailUrls()[0] 
                : null)
            .isFavorite(video.getIsFavorite())
            .createdAt(video.getCreatedAt())
            .build();
    }

    private static String truncatePrompt(String prompt, int maxLength) {
        if (prompt == null || prompt.length() <= maxLength) {
            return prompt;
        }
        return prompt.substring(0, maxLength) + "...";
    }
}

/**
 * DTO pour le statut de la file d'attente.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Statut de la file d'attente de génération")
public class QueueStatusDto {

    @Schema(description = "Nombre de vidéos en attente", example = "12")
    private Integer queuedCount;

    @Schema(description = "Nombre de vidéos en traitement", example = "5")
    private Integer processingCount;

    @Schema(description = "Nombre de workers actifs", example = "3")
    private Integer activeWorkers;

    @Schema(description = "Temps d'attente moyen en minutes", example = "8")
    private Integer averageWaitMinutes;

    @Schema(description = "Dernière mise à jour")
    private LocalDateTime lastUpdate;
}
