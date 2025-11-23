package com.nexusai.video.controller;

import com.nexusai.video.dto.*;
import com.nexusai.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Contrôleur REST pour la génération et gestion des vidéos.
 * 
 * Expose les endpoints suivants:
 * - POST /api/v1/videos/generate : Créer une nouvelle génération
 * - GET /api/v1/videos/{id} : Récupérer une vidéo
 * - GET /api/v1/videos/user/{userId} : Lister les vidéos d'un utilisateur
 * - DELETE /api/v1/videos/{id} : Supprimer une vidéo
 * - POST /api/v1/videos/{id}/favorite : Basculer le statut favori
 * - GET /api/v1/videos/queue-status : Statut de la file d'attente
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Video Generation", description = "APIs de génération et gestion des vidéos")
public class VideoController {

    private final VideoService videoService;

    /**
     * Crée une nouvelle demande de génération vidéo.
     * 
     * @param request Paramètres de génération
     * @param userId ID de l'utilisateur (injecté depuis le token JWT)
     * @return Réponse initiale avec ID de la vidéo
     */
    @PostMapping("/generate")
    @Operation(
        summary = "Générer une nouvelle vidéo",
        description = "Crée une demande de génération vidéo et l'ajoute à la file d'attente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Vidéo créée et ajoutée à la file d'attente",
            content = @Content(schema = @Schema(implementation = VideoGenerationResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Paramètres invalides"),
        @ApiResponse(responseCode = "402", description = "Solde de jetons insuffisant"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<VideoGenerationResponseDto> generateVideo(
            @Valid @RequestBody VideoGenerationRequestDto request,
            @AuthenticationPrincipal UUID userId) {
        
        log.info("Nouvelle demande de génération vidéo par l'utilisateur {}", userId);
        
        VideoGenerationResponseDto response = videoService.createVideoGeneration(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère les détails d'une vidéo.
     * 
     * @param videoId ID de la vidéo
     * @param userId ID de l'utilisateur (injecté depuis le token JWT)
     * @return Détails complets de la vidéo
     */
    @GetMapping("/{videoId}")
    @Operation(
        summary = "Récupérer les détails d'une vidéo",
        description = "Retourne toutes les informations d'une vidéo, incluant son statut et sa progression"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vidéo trouvée",
            content = @Content(schema = @Schema(implementation = VideoDetailsDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "Vidéo non trouvée"),
        @ApiResponse(responseCode = "403", description = "Accès non autorisé")
    })
    public ResponseEntity<VideoDetailsDto> getVideo(
            @Parameter(description = "ID de la vidéo") @PathVariable UUID videoId,
            @AuthenticationPrincipal UUID userId) {
        
        log.debug("Récupération de la vidéo {} par l'utilisateur {}", videoId, userId);
        
        VideoDetailsDto video = videoService.getVideoDetails(videoId, userId);
        
        return ResponseEntity.ok(video);
    }

    /**
     * Liste les vidéos d'un utilisateur avec pagination.
     * 
     * @param userId ID de l'utilisateur (injecté depuis le token JWT)
     * @param page Numéro de page (0-based)
     * @param size Taille de la page
     * @return Liste paginée de vidéos
     */
    @GetMapping("/user")
    @Operation(
        summary = "Lister les vidéos de l'utilisateur",
        description = "Retourne la liste paginée des vidéos de l'utilisateur connecté"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Liste récupérée avec succès",
            content = @Content(schema = @Schema(implementation = VideoListDto.class))
        )
    })
    public ResponseEntity<VideoListDto> listUserVideos(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "Numéro de page (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") 
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Liste des vidéos pour l'utilisateur {} - page {}", userId, page);
        
        Pageable pageable = PageRequest.of(page, size);
        VideoListDto videos = videoService.listUserVideos(userId, pageable);
        
        return ResponseEntity.ok(videos);
    }

    /**
     * Bascule le statut favori d'une vidéo.
     * 
     * @param videoId ID de la vidéo
     * @param userId ID de l'utilisateur (injecté depuis le token JWT)
     * @return Nouveau statut favori
     */
    @PostMapping("/{videoId}/favorite")
    @Operation(
        summary = "Basculer le statut favori",
        description = "Marque ou retire une vidéo des favoris"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
        @ApiResponse(responseCode = "404", description = "Vidéo non trouvée")
    })
    public ResponseEntity<FavoriteStatusDto> toggleFavorite(
            @Parameter(description = "ID de la vidéo") @PathVariable UUID videoId,
            @AuthenticationPrincipal UUID userId) {
        
        log.info("Basculement du statut favori pour la vidéo {} par l'utilisateur {}", videoId, userId);
        
        boolean isFavorite = videoService.toggleFavorite(videoId, userId);
        
        return ResponseEntity.ok(new FavoriteStatusDto(isFavorite));
    }

    /**
     * Supprime une vidéo.
     * 
     * @param videoId ID de la vidéo
     * @param userId ID de l'utilisateur (injecté depuis le token JWT)
     * @return 204 No Content
     */
    @DeleteMapping("/{videoId}")
    @Operation(
        summary = "Supprimer une vidéo",
        description = "Supprime définitivement une vidéo et ses fichiers associés"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Vidéo supprimée"),
        @ApiResponse(responseCode = "404", description = "Vidéo non trouvée"),
        @ApiResponse(responseCode = "403", description = "Accès non autorisé")
    })
    public ResponseEntity<Void> deleteVideo(
            @Parameter(description = "ID de la vidéo") @PathVariable UUID videoId,
            @AuthenticationPrincipal UUID userId) {
        
        log.info("Suppression de la vidéo {} par l'utilisateur {}", videoId, userId);
        
        videoService.deleteVideo(videoId, userId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère le statut global de la file d'attente.
     * 
     * @return Statistiques de la file d'attente
     */
    @GetMapping("/queue-status")
    @Operation(
        summary = "Statut de la file d'attente",
        description = "Retourne les statistiques de la file d'attente de génération"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statut récupéré",
            content = @Content(schema = @Schema(implementation = QueueStatusDto.class))
        )
    })
    public ResponseEntity<QueueStatusDto> getQueueStatus() {
        log.debug("Récupération du statut de la file d'attente");
        
        QueueStatusDto status = videoService.getQueueStatus();
        
        return ResponseEntity.ok(status);
    }

    // DTO pour la réponse du toggle favorite
    private record FavoriteStatusDto(boolean isFavorite) {}
}

// ============================================================================
// SERVICE D'ORCHESTRATION KAFKA
// ============================================================================

package com.nexusai.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.video.domain.entity.GeneratedVideo;
import com.nexusai.video.dto.VideoGenerationRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service d'orchestration du pipeline de génération vidéo via Kafka.
 * 
 * Responsabilités:
 * - Envoi des demandes de génération dans les topics Kafka
 * - Coordination des différentes phases du pipeline
 * - Gestion de l'annulation des générations
 * - Nettoyage des ressources
 * 
 * Topics Kafka utilisés:
 * - video.generation.requests : Nouvelles demandes
 * - video.generation.cancel : Annulations
 * - video.generation.events : Événements de progression
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoOrchestrationService {

    private static final String TOPIC_GENERATION_REQUESTS = "video.generation.requests";
    private static final String TOPIC_GENERATION_CANCEL = "video.generation.cancel";
    private static final String TOPIC_GENERATION_EVENTS = "video.generation.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final S3StorageService s3Service;
    private final WorkerMonitoringService workerMonitoring;

    /**
     * Envoie une demande de génération vidéo dans la file Kafka.
     * 
     * Le message contient toutes les informations nécessaires pour
     * que les workers puissent traiter la génération.
     * 
     * @param video Entité vidéo créée
     * @param request Paramètres de génération
     */
    public void queueVideoGeneration(
            GeneratedVideo video, 
            VideoGenerationRequestDto request) {
        
        try {
            // Construction du message
            Map<String, Object> message = new HashMap<>();
            message.put("videoId", video.getId().toString());
            message.put("userId", video.getUserId().toString());
            message.put("companionId", video.getCompanionId());
            message.put("prompt", request.getPrompt());
            message.put("durationSeconds", request.getDurationSeconds());
            message.put("quality", video.getQuality().name());
            message.put("resolution", video.getResolution());
            message.put("frameRate", video.getFrameRate());
            message.put("visualStyle", request.getVisualStyle());
            message.put("musicStyle", request.getMusicStyle());
            message.put("includeElements", request.getIncludeElements());
            message.put("timestamp", System.currentTimeMillis());

            String messageJson = objectMapper.writeValueAsString(message);

            // Envoi dans Kafka avec la clé = userId pour partitionnement
            kafkaTemplate.send(
                TOPIC_GENERATION_REQUESTS,
                video.getUserId().toString(),
                messageJson
            );

            log.info("Demande de génération envoyée dans Kafka pour la vidéo {}", video.getId());

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la demande dans Kafka", e);
            throw new RuntimeException("Échec de l'envoi dans la file d'attente", e);
        }
    }

    /**
     * Annule une génération vidéo en cours.
     * 
     * Envoie un message d'annulation que les workers écouteront
     * pour arrêter le traitement.
     * 
     * @param videoId ID de la vidéo à annuler
     */
    public void cancelVideoGeneration(UUID videoId) {
        try {
            Map<String, Object> cancelMessage = new HashMap<>();
            cancelMessage.put("videoId", videoId.toString());
            cancelMessage.put("timestamp", System.currentTimeMillis());
            cancelMessage.put("reason", "USER_CANCELLATION");

            String messageJson = objectMapper.writeValueAsString(cancelMessage);

            kafkaTemplate.send(
                TOPIC_GENERATION_CANCEL,
                videoId.toString(),
                messageJson
            );

            log.info("Message d'annulation envoyé pour la vidéo {}", videoId);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message d'annulation", e);
        }
    }

    /**
     * Supprime les fichiers d'une vidéo depuis S3.
     * 
     * @param video Vidéo dont il faut supprimer les fichiers
     */
    public void deleteVideoFiles(GeneratedVideo video) {
        try {
            // Suppression de la vidéo principale
            if (video.getStorageUrl() != null) {
                s3Service.deleteFile(video.getStorageUrl());
                log.info("Vidéo principale supprimée: {}", video.getStorageUrl());
            }

            // Suppression des vignettes
            if (video.getThumbnailUrls() != null) {
                for (String thumbnailUrl : video.getThumbnailUrls()) {
                    s3Service.deleteFile(thumbnailUrl);
                }
                log.info("Vignettes supprimées pour la vidéo {}", video.getId());
            }

            // Suppression des assets
            video.getAssets().forEach(asset -> {
                s3Service.deleteFile(asset.getStorageUrl());
            });
            log.info("Assets supprimés pour la vidéo {}", video.getId());

        } catch (Exception e) {
            log.error("Erreur lors de la suppression des fichiers S3", e);
        }
    }

    /**
     * Récupère le nombre de workers actifs.
     * 
     * @return Nombre de workers en activité
     */
    public int getActiveWorkersCount() {
        return workerMonitoring.getActiveWorkersCount();
    }

    /**
     * Émet un événement de progression dans Kafka.
     * 
     * Utilisé par les workers pour notifier de l'avancement.
     * 
     * @param videoId ID de la vidéo
     * @param phase Phase actuelle
     * @param percentage Pourcentage de progression
     */
    public void emitProgressEvent(UUID videoId, String phase, int percentage) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("videoId", videoId.toString());
            event.put("phase", phase);
            event.put("percentage", percentage);
            event.put("timestamp", System.currentTimeMillis());

            String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                TOPIC_GENERATION_EVENTS,
                videoId.toString(),
                eventJson
            );

        } catch (Exception e) {
            log.error("Erreur lors de l'émission de l'événement de progression", e);
        }
    }
}
