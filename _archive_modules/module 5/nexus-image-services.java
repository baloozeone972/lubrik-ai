/**
 * ═══════════════════════════════════════════════════════════════════════════
 * MODULE 5 : SERVICES & CONTRÔLEURS
 * ═══════════════════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════════════════
// 3. MODULE CORE - BUSINESS LOGIC
// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.core.service;

import com.nexusai.image.domain.dto.ImageGenerationRequest;
import com.nexusai.image.domain.dto.ImageGenerationResponse;
import com.nexusai.image.domain.entity.GeneratedImage;
import com.nexusai.image.domain.entity.GeneratedImage.ImageStatus;
import com.nexusai.image.domain.event.ImageGenerationRequestedEvent;
import com.nexusai.image.infrastructure.kafka.ImageGenerationProducer;
import com.nexusai.image.infrastructure.repository.GeneratedImageRepository;
import com.nexusai.image.core.mapper.ImageMapper;
import com.nexusai.image.core.exception.ImageNotFoundException;
import com.nexusai.image.core.exception.InsufficientTokensException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service principal pour la génération d'images
 * 
 * Ce service gère:
 * - La création de requêtes de génération
 * - La validation des tokens
 * - La publication des événements Kafka
 * - La gestion du cycle de vie des images
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ImageGenerationService {

    private final GeneratedImageRepository imageRepository;
    private final ImageGenerationProducer kafkaProducer;
    private final TokenService tokenService;
    private final ModerationService moderationService;
    private final ImageMapper imageMapper;

    /**
     * Initie la génération d'une image
     * 
     * @param request Requête de génération
     * @param userId ID de l'utilisateur
     * @return Réponse avec les détails de l'image
     * @throws InsufficientTokensException Si l'utilisateur n'a pas assez de tokens
     */
    public ImageGenerationResponse generateImage(ImageGenerationRequest request, UUID userId) {
        log.info("Début génération image pour utilisateur: {}", userId);

        // 1. Calculer le coût en tokens
        int tokensCost = calculateTokensCost(request);
        
        // 2. Vérifier le solde de tokens
        if (!tokenService.hasEnoughTokens(userId, tokensCost)) {
            throw new InsufficientTokensException(
                String.format("Solde insuffisant. Requis: %d tokens", tokensCost)
            );
        }

        // 3. Modération du prompt
        moderationService.moderatePrompt(request.getPrompt());

        // 4. Créer l'enregistrement en base
        GeneratedImage image = GeneratedImage.builder()
            .userId(userId)
            .companionId(request.getCompanionId())
            .prompt(request.getPrompt())
            .negativePrompt(request.getNegativePrompt())
            .style(request.getStyle())
            .resolution(request.getResolution())
            .seed(request.getSeed())
            .status(ImageStatus.QUEUED)
            .tokensCost(tokensCost)
            .isFavorite(false)
            .isPublic(request.getIsPublic())
            .build();

        image = imageRepository.save(image);
        log.info("Image créée en base: {}", image.getId());

        // 5. Consommer les tokens
        tokenService.consumeTokens(userId, tokensCost);

        // 6. Publier l'événement Kafka
        ImageGenerationRequestedEvent event = ImageGenerationRequestedEvent.builder()
            .imageId(image.getId())
            .userId(userId)
            .prompt(image.getPrompt())
            .negativePrompt(image.getNegativePrompt())
            .style(image.getStyle())
            .resolution(image.getResolution())
            .seed(image.getSeed())
            .companionId(image.getCompanionId())
            .build();

        kafkaProducer.publishGenerationRequest(event);

        log.info("Événement Kafka publié pour image: {}", image.getId());

        return imageMapper.toResponse(image);
    }

    /**
     * Récupère une image par son ID
     * 
     * @param imageId ID de l'image
     * @param userId ID de l'utilisateur (pour sécurité)
     * @return Réponse avec les détails de l'image
     * @throws ImageNotFoundException Si l'image n'existe pas
     */
    @Transactional(readOnly = true)
    public ImageGenerationResponse getImage(UUID imageId, UUID userId) {
        GeneratedImage image = imageRepository.findByIdAndUserId(imageId, userId)
            .orElseThrow(() -> new ImageNotFoundException(
                String.format("Image non trouvée: %s", imageId)
            ));

        return imageMapper.toResponse(image);
    }

    /**
     * Liste les images d'un utilisateur avec pagination
     * 
     * @param userId ID de l'utilisateur
     * @param pageable Paramètres de pagination
     * @return Page d'images
     */
    @Transactional(readOnly = true)
    public Page<ImageGenerationResponse> getUserImages(UUID userId, Pageable pageable) {
        return imageRepository.findByUserId(userId, pageable)
            .map(imageMapper::toResponse);
    }

    /**
     * Liste les images favorites d'un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @param pageable Paramètres de pagination
     * @return Page d'images favorites
     */
    @Transactional(readOnly = true)
    public Page<ImageGenerationResponse> getFavoriteImages(UUID userId, Pageable pageable) {
        return imageRepository.findByUserIdAndIsFavoriteTrue(userId, pageable)
            .map(imageMapper::toResponse);
    }

    /**
     * Marque/Démarque une image comme favorite
     * 
     * @param imageId ID de l'image
     * @param userId ID de l'utilisateur
     * @param isFavorite Nouveau statut favorite
     */
    public void toggleFavorite(UUID imageId, UUID userId, boolean isFavorite) {
        GeneratedImage image = imageRepository.findByIdAndUserId(imageId, userId)
            .orElseThrow(() -> new ImageNotFoundException(
                String.format("Image non trouvée: %s", imageId)
            ));

        image.setIsFavorite(isFavorite);
        imageRepository.save(image);

        log.info("Image {} marquée comme favorite: {}", imageId, isFavorite);
    }

    /**
     * Supprime une image
     * 
     * @param imageId ID de l'image
     * @param userId ID de l'utilisateur
     */
    public void deleteImage(UUID imageId, UUID userId) {
        GeneratedImage image = imageRepository.findByIdAndUserId(imageId, userId)
            .orElseThrow(() -> new ImageNotFoundException(
                String.format("Image non trouvée: %s", imageId)
            ));

        // Supprimer de S3 si l'image a été générée
        if (image.getStorageUrl() != null) {
            // TODO: Appeler S3StorageService pour supprimer les fichiers
        }

        imageRepository.delete(image);

        log.info("Image supprimée: {}", imageId);
    }

    /**
     * Met à jour le statut d'une image (appelé par le worker)
     * 
     * @param imageId ID de l'image
     * @param status Nouveau statut
     * @param storageUrl URL de l'image générée (si COMPLETED)
     * @param thumbnailUrl URL de la thumbnail (si COMPLETED)
     * @param generationTimeSeconds Temps de génération
     */
    public void updateImageStatus(UUID imageId, ImageStatus status, 
                                   String storageUrl, String thumbnailUrl,
                                   Integer generationTimeSeconds) {
        GeneratedImage image = imageRepository.findById(imageId)
            .orElseThrow(() -> new ImageNotFoundException(
                String.format("Image non trouvée: %s", imageId)
            ));

        image.setStatus(status);

        if (status == ImageStatus.COMPLETED) {
            image.setStorageUrl(storageUrl);
            image.setThumbnailUrl(thumbnailUrl);
            image.setGenerationTimeSeconds(generationTimeSeconds);
            image.setCompletedAt(LocalDateTime.now());
        }

        imageRepository.save(image);

        log.info("Statut de l'image {} mis à jour: {}", imageId, status);
    }

    /**
     * Calcule le coût en tokens selon la résolution
     * 
     * @param request Requête de génération
     * @return Coût en tokens
     */
    private int calculateTokensCost(ImageGenerationRequest request) {
        return switch (request.getResolution()) {
            case "512x512" -> 5;
            case "768x768" -> 10;
            case "1024x1024" -> 20;
            case "1024x1536" -> 30;
            default -> 20;
        };
    }

    /**
     * Job planifié pour gérer les images en timeout
     * (Plus de 10 minutes en PROCESSING)
     */
    @Transactional
    public void handleTimedOutImages() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(10);
        
        var timedOutImages = imageRepository.findTimedOutImages(
            ImageStatus.PROCESSING, 
            timeoutThreshold
        );

        for (GeneratedImage image : timedOutImages) {
            log.warn("Image en timeout détectée: {}", image.getId());
            
            image.setStatus(ImageStatus.FAILED);
            imageRepository.save(image);
            
            // Rembourser les tokens
            tokenService.refundTokens(image.getUserId(), image.getTokensCost());
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Service de gestion des tokens
 * 
 * Ce service communique avec le module Payment
 * pour vérifier et consommer les tokens
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final RestTemplate restTemplate;
    
    private static final String PAYMENT_SERVICE_URL = "http://payment-service:8081";

    /**
     * Vérifie si l'utilisateur a assez de tokens
     * 
     * @param userId ID de l'utilisateur
     * @param requiredTokens Tokens nécessaires
     * @return true si l'utilisateur a assez de tokens
     */
    public boolean hasEnoughTokens(UUID userId, int requiredTokens) {
        try {
            String url = String.format("%s/api/v1/tokens/balance?userId=%s", 
                PAYMENT_SERVICE_URL, userId);
            
            TokenBalanceResponse response = restTemplate.getForObject(
                url, 
                TokenBalanceResponse.class
            );

            return response != null && response.getBalance() >= requiredTokens;

        } catch (Exception e) {
            log.error("Erreur lors de la vérification du solde: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Consomme des tokens
     * 
     * @param userId ID de l'utilisateur
     * @param tokens Nombre de tokens à consommer
     */
    public void consumeTokens(UUID userId, int tokens) {
        try {
            String url = String.format("%s/api/v1/tokens/consume", PAYMENT_SERVICE_URL);
            
            TokenConsumeRequest request = TokenConsumeRequest.builder()
                .userId(userId)
                .amount(tokens)
                .description("Image generation")
                .build();

            restTemplate.postForObject(url, request, Void.class);

            log.info("Tokens consommés: {} pour utilisateur: {}", tokens, userId);

        } catch (Exception e) {
            log.error("Erreur lors de la consommation de tokens: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la consommation de tokens", e);
        }
    }

    /**
     * Rembourse des tokens (en cas d'échec)
     * 
     * @param userId ID de l'utilisateur
     * @param tokens Nombre de tokens à rembourser
     */
    public void refundTokens(UUID userId, int tokens) {
        try {
            String url = String.format("%s/api/v1/tokens/refund", PAYMENT_SERVICE_URL);
            
            TokenRefundRequest request = TokenRefundRequest.builder()
                .userId(userId)
                .amount(tokens)
                .reason("Image generation failed")
                .build();

            restTemplate.postForObject(url, request, Void.class);

            log.info("Tokens remboursés: {} pour utilisateur: {}", tokens, userId);

        } catch (Exception e) {
            log.error("Erreur lors du remboursement de tokens: {}", e.getMessage(), e);
        }
    }

    // DTOs internes
    private record TokenBalanceResponse(int balance) {}
    
    @lombok.Builder
    private record TokenConsumeRequest(UUID userId, int amount, String description) {}
    
    @lombok.Builder
    private record TokenRefundRequest(UUID userId, int amount, String reason) {}
}

// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.core.service;

import com.nexusai.image.core.exception.ModerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service de modération des prompts
 * 
 * Ce service communique avec le module Moderation
 * pour valider les prompts avant génération
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {

    private final RestTemplate restTemplate;
    
    private static final String MODERATION_SERVICE_URL = "http://moderation-service:8089";

    /**
     * Modère un prompt avant génération
     * 
     * @param prompt Prompt à modérer
     * @throws ModerationException Si le prompt est inapproprié
     */
    public void moderatePrompt(String prompt) {
        try {
            String url = String.format("%s/api/v1/moderation/text", MODERATION_SERVICE_URL);
            
            ModerationRequest request = new ModerationRequest(prompt);
            ModerationResponse response = restTemplate.postForObject(
                url, 
                request, 
                ModerationResponse.class
            );

            if (response != null && response.isBlocked()) {
                log.warn("Prompt bloqué par modération: {}", response.getReason());
                throw new ModerationException(
                    String.format("Prompt inapproprié: %s", response.getReason())
                );
            }

            log.info("Prompt validé par modération");

        } catch (ModerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la modération: {}", e.getMessage(), e);
            // En cas d'erreur du service de modération, on laisse passer
            log.warn("Modération non disponible, génération autorisée");
        }
    }

    // DTOs internes
    private record ModerationRequest(String content) {}
    private record ModerationResponse(boolean blocked, String reason) {
        public boolean isBlocked() {
            return blocked;
        }
        public String getReason() {
            return reason;
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.core.mapper;

import com.nexusai.image.domain.dto.ImageGenerationResponse;
import com.nexusai.image.domain.entity.GeneratedImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre entités et DTOs
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@Mapper(componentModel = "spring")
public interface ImageMapper {

    /**
     * Convertit une entité en DTO de réponse
     */
    @Mapping(target = "status", expression = "java(image.getStatus().name())")
    ImageGenerationResponse toResponse(GeneratedImage image);
}

// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.core.exception;

/**
 * Exception levée quand une image n'est pas trouvée
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
public class ImageNotFoundException extends RuntimeException {
    public ImageNotFoundException(String message) {
        super(message);
    }
}

// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.core.exception;

/**
 * Exception levée quand l'utilisateur n'a pas assez de tokens
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
public class InsufficientTokensException extends RuntimeException {
    public InsufficientTokensException(String message) {
        super(message);
    }
}

// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.core.exception;

/**
 * Exception levée quand le prompt est bloqué par la modération
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
public class ModerationException extends RuntimeException {
    public ModerationException(String message) {
        super(message);
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 4. MODULE API - CONTRÔLEURS REST
// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.api.controller;

import com.nexusai.image.core.service.ImageGenerationService;
import com.nexusai.image.domain.dto.ImageGenerationRequest;
import com.nexusai.image.domain.dto.ImageGenerationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Contrôleur REST pour la génération d'images
 * 
 * Endpoints disponibles:
 * - POST /api/v1/images/generate : Générer une image
 * - GET /api/v1/images/{id} : Récupérer une image
 * - DELETE /api/v1/images/{id} : Supprimer une image
 * - GET /api/v1/images/user/{userId} : Lister les images d'un utilisateur
 * - POST /api/v1/images/{id}/favorite : Marquer comme favorite
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Generation", description = "APIs de génération d'images")
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;

    /**
     * Génère une nouvelle image
     * 
     * @param request Requête de génération
     * @param userId ID de l'utilisateur authentifié
     * @return Détails de l'image créée
     */
    @PostMapping("/generate")
    @Operation(
        summary = "Générer une image",
        description = "Initie la génération d'une image via IA selon le prompt fourni"
    )
    @ApiResponse(responseCode = "201", description = "Image créée avec succès")
    @ApiResponse(responseCode = "400", description = "Requête invalide")
    @ApiResponse(responseCode = "402", description = "Tokens insuffisants")
    public ResponseEntity<ImageGenerationResponse> generateImage(
            @Valid @RequestBody ImageGenerationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        
        log.info("Requête de génération reçue de l'utilisateur: {}", userId);
        
        ImageGenerationResponse response = imageGenerationService.generateImage(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère les détails d'une image
     * 
     * @param imageId ID de l'image
     * @param userId ID de l'utilisateur authentifié
     * @return Détails de l'image
     */
    @GetMapping("/{imageId}")
    @Operation(
        summary = "Récupérer une image",
        description = "Récupère les détails d'une image générée"
    )
    @ApiResponse(responseCode = "200", description = "Image trouvée")
    @ApiResponse(responseCode = "404", description = "Image non trouvée")
    public ResponseEntity<ImageGenerationResponse> getImage(
            @PathVariable UUID imageId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        
        ImageGenerationResponse response = imageGenerationService.getImage(imageId, userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Liste les images d'un utilisateur
     * 
     * @param userId ID de l'utilisateur authentifié
     * @param pageable Paramètres de pagination
     * @return Page d'images
     */
    @GetMapping("/user/me")
    @Operation(
        summary = "Lister mes images",
        description = "Liste toutes les images générées par l'utilisateur connecté"
    )
    public ResponseEntity<Page<ImageGenerationResponse>> getMyImages(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ImageGenerationResponse> images = imageGenerationService.getUserImages(userId, pageable);
        
        return ResponseEntity.ok(images);
    }

    /**
     * Liste les images favorites
     * 
     * @param userId ID de l'utilisateur authentifié
     * @param pageable Paramètres de pagination
     * @return Page d'images favorites
     */
    @GetMapping("/favorites")
    @Operation(
        summary = "Lister mes images favorites",
        description = "Liste les images marquées comme favorites"
    )
    public ResponseEntity<Page<ImageGenerationResponse>> getFavorites(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<ImageGenerationResponse> favorites = 
            imageGenerationService.getFavoriteImages(userId, pageable);
        
        return ResponseEntity.ok(favorites);
    }

    /**
     * Marque/Démarque une image comme favorite
     * 
     * @param imageId ID de l'image
     * @param userId ID de l'utilisateur authentifié
     * @param isFavorite Nouveau statut
     * @return 204 No Content
     */
    @PostMapping("/{imageId}/favorite")
    @Operation(
        summary = "Marquer comme favorite",
        description = "Marque ou démarque une image comme favorite"
    )
    @ApiResponse(responseCode = "204", description = "Statut mis à jour")
    public ResponseEntity<Void> toggleFavorite(
            @PathVariable UUID imageId,
            @RequestParam boolean isFavorite,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        
        imageGenerationService.toggleFavorite(imageId, userId, isFavorite);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Supprime une image
     * 
     * @param imageId ID de l'image
     * @param userId ID de l'utilisateur authentifié
     * @return 204 No Content
     */
    @DeleteMapping("/{imageId}")
    @Operation(
        summary = "Supprimer une image",
        description = "Supprime définitivement une image générée"
    )
    @ApiResponse(responseCode = "204", description = "Image supprimée")
    @ApiResponse(responseCode = "404", description = "Image non trouvée")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID imageId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        
        imageGenerationService.deleteImage(imageId, userId);
        
        return ResponseEntity.noContent().build();
    }
}
