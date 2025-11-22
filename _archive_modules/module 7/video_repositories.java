package com.nexusai.video.repository;

import com.nexusai.video.domain.entity.GeneratedVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'accès aux données des vidéos générées.
 * 
 * Fournit les méthodes CRUD de base ainsi que des requêtes personnalisées
 * pour interroger les vidéos selon différents critères.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Repository
public interface GeneratedVideoRepository extends JpaRepository<GeneratedVideo, UUID> {

    /**
     * Trouve toutes les vidéos d'un utilisateur avec pagination.
     * 
     * @param userId ID de l'utilisateur
     * @param pageable Configuration de pagination
     * @return Page de vidéos
     */
    Page<GeneratedVideo> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Trouve toutes les vidéos d'un utilisateur avec un statut spécifique.
     * 
     * @param userId ID de l'utilisateur
     * @param status Statut recherché
     * @param pageable Configuration de pagination
     * @return Page de vidéos
     */
    Page<GeneratedVideo> findByUserIdAndStatus(
        UUID userId, 
        GeneratedVideo.VideoStatus status, 
        Pageable pageable
    );

    /**
     * Trouve les vidéos favorites d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param pageable Configuration de pagination
     * @return Page de vidéos favorites
     */
    Page<GeneratedVideo> findByUserIdAndIsFavoriteTrue(UUID userId, Pageable pageable);

    /**
     * Trouve toutes les vidéos en attente (QUEUED).
     * 
     * @return Liste des vidéos en attente
     */
    List<GeneratedVideo> findByStatusOrderByCreatedAtAsc(GeneratedVideo.VideoStatus status);

    /**
     * Compte le nombre de vidéos en cours de traitement.
     * 
     * @return Nombre de vidéos en traitement
     */
    @Query("SELECT COUNT(v) FROM GeneratedVideo v WHERE v.status = 'PROCESSING'")
    long countProcessingVideos();

    /**
     * Compte le nombre de vidéos en attente.
     * 
     * @return Nombre de vidéos en attente
     */
    @Query("SELECT COUNT(v) FROM GeneratedVideo v WHERE v.status = 'QUEUED'")
    long countQueuedVideos();

    /**
     * Trouve la position dans la file d'attente pour une vidéo donnée.
     * 
     * @param videoId ID de la vidéo
     * @return Position (1-based) ou 0 si pas en attente
     */
    @Query("""
        SELECT COUNT(v) + 1 
        FROM GeneratedVideo v 
        WHERE v.status = 'QUEUED' 
        AND v.createdAt < (
            SELECT v2.createdAt 
            FROM GeneratedVideo v2 
            WHERE v2.id = :videoId
        )
    """)
    long findQueuePosition(@Param("videoId") UUID videoId);

    /**
     * Trouve une vidéo par son ID et l'ID de l'utilisateur (sécurité).
     * 
     * @param id ID de la vidéo
     * @param userId ID de l'utilisateur
     * @return Optional contenant la vidéo si trouvée
     */
    Optional<GeneratedVideo> findByIdAndUserId(UUID id, UUID userId);
}

// ============================================================================
// SERVICE PRINCIPAL DE GESTION DES VIDÉOS
// ============================================================================

package com.nexusai.video.service;

import com.nexusai.video.domain.entity.GeneratedVideo;
import com.nexusai.video.dto.*;
import com.nexusai.video.exception.*;
import com.nexusai.video.repository.GeneratedVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service principal de gestion des vidéos.
 * 
 * Coordonne les opérations CRUD et la logique métier liée aux vidéos générées.
 * 
 * Responsabilités:
 * - Création de nouvelles demandes de génération
 * - Récupération des vidéos existantes
 * - Mise à jour du statut et de la progression
 * - Gestion des favoris
 * - Suppression de vidéos
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final GeneratedVideoRepository videoRepository;
    private final VideoOrchestrationService orchestrationService;
    private final TokenService tokenService;
    private final VideoConfigurationService configService;

    /**
     * Crée une nouvelle demande de génération vidéo.
     * 
     * Cette méthode:
     * 1. Valide la demande
     * 2. Vérifie le solde de jetons
     * 3. Calcule le coût
     * 4. Crée l'enregistrement en base
     * 5. Envoie la demande dans la file Kafka
     * 
     * @param request Paramètres de génération
     * @param userId ID de l'utilisateur
     * @return Réponse initiale avec ID et statut
     * @throws InsufficientTokensException Si l'utilisateur n'a pas assez de jetons
     * @throws ValidationException Si les paramètres sont invalides
     */
    @Transactional
    public VideoGenerationResponseDto createVideoGeneration(
            VideoGenerationRequestDto request, 
            UUID userId) {
        
        log.info("Création d'une nouvelle génération vidéo pour l'utilisateur {}", userId);

        // Validation métier
        validateRequest(request);

        // Calcul du coût en jetons
        int tokenCost = calculateTokenCost(request);
        log.debug("Coût calculé: {} jetons", tokenCost);

        // Vérification du solde
        if (!tokenService.hasEnoughTokens(userId, tokenCost)) {
            log.warn("Solde de jetons insuffisant pour l'utilisateur {}", userId);
            throw new InsufficientTokensException(
                "Solde insuffisant. Requis: " + tokenCost + " jetons"
            );
        }

        // Création de l'entité
        GeneratedVideo video = GeneratedVideo.builder()
            .userId(userId)
            .companionId(request.getCompanionId())
            .prompt(request.getPrompt())
            .durationSeconds(request.getDurationSeconds())
            .quality(request.getQuality())
            .resolution(getResolutionForQuality(request.getQuality()))
            .frameRate(getFrameRateForQuality(request.getQuality()))
            .status(GeneratedVideo.VideoStatus.QUEUED)
            .progressPercentage(0)
            .tokensCost(tokenCost)
            .build();

        // Sauvegarde en base
        video = videoRepository.save(video);
        log.info("Vidéo créée avec l'ID: {}", video.getId());

        // Réservation des jetons
        tokenService.reserveTokens(userId, tokenCost, video.getId().toString());

        // Envoi dans la file d'attente Kafka
        orchestrationService.queueVideoGeneration(video, request);

        // Calcul de la position et du temps d'attente
        long queuePosition = videoRepository.findQueuePosition(video.getId());
        int estimatedWaitMinutes = estimateWaitTime(queuePosition);

        return VideoGenerationResponseDto.builder()
            .videoId(video.getId())
            .status(video.getStatus())
            .queuePosition((int) queuePosition)
            .estimatedWaitMinutes(estimatedWaitMinutes)
            .tokensCost(tokenCost)
            .message("Votre vidéo a été ajoutée à la file d'attente de génération")
            .createdAt(video.getCreatedAt())
            .build();
    }

    /**
     * Récupère les détails complets d'une vidéo.
     * 
     * @param videoId ID de la vidéo
     * @param userId ID de l'utilisateur (pour vérification de propriété)
     * @return Détails de la vidéo
     * @throws VideoNotFoundException Si la vidéo n'existe pas
     * @throws UnauthorizedAccessException Si l'utilisateur n'est pas propriétaire
     */
    @Transactional(readOnly = true)
    public VideoDetailsDto getVideoDetails(UUID videoId, UUID userId) {
        log.debug("Récupération des détails de la vidéo {} pour l'utilisateur {}", videoId, userId);

        GeneratedVideo video = videoRepository.findByIdAndUserId(videoId, userId)
            .orElseThrow(() -> new VideoNotFoundException(
                "Vidéo non trouvée ou accès non autorisé: " + videoId
            ));

        return VideoDetailsDto.fromEntity(video);
    }

    /**
     * Liste les vidéos d'un utilisateur avec pagination.
     * 
     * @param userId ID de l'utilisateur
     * @param pageable Configuration de pagination
     * @return Page de résumés de vidéos
     */
    @Transactional(readOnly = true)
    public VideoListDto listUserVideos(UUID userId, Pageable pageable) {
        log.debug("Liste des vidéos pour l'utilisateur {} - page {}", userId, pageable.getPageNumber());

        Page<GeneratedVideo> page = videoRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return VideoListDto.builder()
            .videos(page.getContent().stream()
                .map(VideoSummaryDto::fromEntity)
                .toList())
            .currentPage(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .isLast(page.isLast())
            .build();
    }

    /**
     * Bascule le statut favori d'une vidéo.
     * 
     * @param videoId ID de la vidéo
     * @param userId ID de l'utilisateur
     * @return true si désormais favori, false sinon
     */
    @Transactional
    public boolean toggleFavorite(UUID videoId, UUID userId) {
        log.info("Basculement du statut favori pour la vidéo {}", videoId);

        GeneratedVideo video = videoRepository.findByIdAndUserId(videoId, userId)
            .orElseThrow(() -> new VideoNotFoundException("Vidéo non trouvée: " + videoId));

        video.setIsFavorite(!video.getIsFavorite());
        videoRepository.save(video);

        return video.getIsFavorite();
    }

    /**
     * Supprime une vidéo.
     * 
     * @param videoId ID de la vidéo
     * @param userId ID de l'utilisateur
     */
    @Transactional
    public void deleteVideo(UUID videoId, UUID userId) {
        log.info("Suppression de la vidéo {} par l'utilisateur {}", videoId, userId);

        GeneratedVideo video = videoRepository.findByIdAndUserId(videoId, userId)
            .orElseThrow(() -> new VideoNotFoundException("Vidéo non trouvée: " + videoId));

        // Si la vidéo est en cours de traitement, l'annuler
        if (video.getStatus() == GeneratedVideo.VideoStatus.PROCESSING ||
            video.getStatus() == GeneratedVideo.VideoStatus.QUEUED) {
            
            orchestrationService.cancelVideoGeneration(videoId);
            video.setStatus(GeneratedVideo.VideoStatus.CANCELLED);
        }

        // Suppression des fichiers S3 si existants
        if (video.getStorageUrl() != null) {
            orchestrationService.deleteVideoFiles(video);
        }

        videoRepository.delete(video);
        log.info("Vidéo {} supprimée avec succès", videoId);
    }

    /**
     * Récupère le statut de la file d'attente.
     * 
     * @return Statut global de la file
     */
    @Transactional(readOnly = true)
    public QueueStatusDto getQueueStatus() {
        long queuedCount = videoRepository.countQueuedVideos();
        long processingCount = videoRepository.countProcessingVideos();

        return QueueStatusDto.builder()
            .queuedCount((int) queuedCount)
            .processingCount((int) processingCount)
            .activeWorkers(orchestrationService.getActiveWorkersCount())
            .averageWaitMinutes(estimateWaitTime(queuedCount))
            .lastUpdate(LocalDateTime.now())
            .build();
    }

    // ========================================================================
    // MÉTHODES PRIVÉES
    // ========================================================================

    private void validateRequest(VideoGenerationRequestDto request) {
        // Validation de la durée selon la qualité
        int maxDuration = switch (request.getQuality()) {
            case STANDARD -> 300; // 5 minutes
            case HD -> 180;       // 3 minutes
            case ULTRA -> 120;    // 2 minutes
        };

        if (request.getDurationSeconds() > maxDuration) {
            throw new ValidationException(
                String.format("Durée maximale pour %s: %d secondes", 
                    request.getQuality(), maxDuration)
            );
        }
    }

    private int calculateTokenCost(VideoGenerationRequestDto request) {
        int baseCost = configService.getBaseCostPerSecond();
        int qualityMultiplier = switch (request.getQuality()) {
            case STANDARD -> 1;
            case HD -> 2;
            case ULTRA -> 5;
        };

        return request.getDurationSeconds() * baseCost * qualityMultiplier;
    }

    private String getResolutionForQuality(GeneratedVideo.VideoQuality quality) {
        return switch (quality) {
            case STANDARD, HD -> "1080p";
            case ULTRA -> "4K";
        };
    }

    private int getFrameRateForQuality(GeneratedVideo.VideoQuality quality) {
        return switch (quality) {
            case STANDARD -> 30;
            case HD, ULTRA -> 60;
        };
    }

    private int estimateWaitTime(long queuePosition) {
        // Estimation: 5-10 minutes par vidéo en moyenne
        return (int) (queuePosition * 7);
    }
}
