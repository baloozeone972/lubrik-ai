package com.nexusai.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Service de gestion du stockage S3/MinIO.
 * 
 * Responsabilités:
 * - Upload de fichiers vidéo et assets
 * - Génération d'URLs signées
 * - Suppression de fichiers
 * - Gestion des métadonnées
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket.videos}")
    private String videoBucket;

    @Value("${aws.s3.bucket.assets}")
    private String assetsBucket;

    /**
     * Upload un fichier vidéo vers S3.
     * 
     * @param videoId ID de la vidéo
     * @param filePath Chemin local du fichier
     * @return URL S3 du fichier uploadé
     */
    public String uploadVideo(UUID videoId, Path filePath) {
        String key = String.format("videos/%s/%s.mp4", 
            videoId.toString().substring(0, 2), 
            videoId);

        try {
            log.info("Upload de la vidéo {} vers S3", videoId);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(videoBucket)
                .key(key)
                .contentType("video/mp4")
                .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(filePath));

            String url = String.format("https://%s.s3.amazonaws.com/%s", videoBucket, key);
            log.info("Vidéo uploadée avec succès: {}", url);

            return url;

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de la vidéo {}", videoId, e);
            throw new RuntimeException("Échec de l'upload S3", e);
        }
    }

    /**
     * Upload une vignette vers S3.
     * 
     * @param videoId ID de la vidéo
     * @param thumbnailFile Fichier de la vignette
     * @param index Index de la vignette
     * @return URL S3 de la vignette
     */
    public String uploadThumbnail(UUID videoId, File thumbnailFile, int index) {
        String key = String.format("thumbnails/%s/thumb_%d.jpg", videoId, index);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(videoBucket)
                .key(key)
                .contentType("image/jpeg")
                .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(thumbnailFile));

            return String.format("https://%s.s3.amazonaws.com/%s", videoBucket, key);

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de la vignette", e);
            throw new RuntimeException("Échec de l'upload de la vignette", e);
        }
    }

    /**
     * Upload un asset (image, audio) vers S3.
     * 
     * @param videoId ID de la vidéo
     * @param assetFile Fichier de l'asset
     * @param assetType Type d'asset
     * @return URL S3 de l'asset
     */
    public String uploadAsset(UUID videoId, File assetFile, String assetType) {
        String extension = getFileExtension(assetFile.getName());
        String key = String.format("assets/%s/%s_%s.%s", 
            videoId, 
            assetType, 
            UUID.randomUUID(), 
            extension);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(assetsBucket)
                .key(key)
                .contentType(getContentType(extension))
                .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(assetFile));

            return String.format("https://%s.s3.amazonaws.com/%s", assetsBucket, key);

        } catch (Exception e) {
            log.error("Erreur lors de l'upload de l'asset", e);
            throw new RuntimeException("Échec de l'upload de l'asset", e);
        }
    }

    /**
     * Supprime un fichier depuis S3.
     * 
     * @param fileUrl URL du fichier à supprimer
     */
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            String bucket = extractBucketFromUrl(fileUrl);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Fichier supprimé: {}", fileUrl);

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du fichier {}", fileUrl, e);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "mp4" -> "video/mp4";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }

    private String extractKeyFromUrl(String url) {
        return url.substring(url.indexOf(".com/") + 5);
    }

    private String extractBucketFromUrl(String url) {
        String domain = url.substring(url.indexOf("//") + 2);
        return domain.substring(0, domain.indexOf('.'));
    }
}

// ============================================================================
// SERVICE DE GESTION DES JETONS
// ============================================================================

package com.nexusai.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

/**
 * Service d'interface avec le module Payment pour la gestion des jetons.
 * 
 * Communique avec l'API du module Payment pour:
 * - Vérifier le solde de jetons
 * - Réserver des jetons
 * - Consommer des jetons
 * - Libérer des jetons en cas d'échec
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final WebClient paymentServiceClient;

    /**
     * Vérifie si l'utilisateur a suffisamment de jetons.
     * 
     * @param userId ID de l'utilisateur
     * @param requiredTokens Nombre de jetons requis
     * @return true si le solde est suffisant
     */
    public boolean hasEnoughTokens(UUID userId, int requiredTokens) {
        try {
            log.debug("Vérification du solde de jetons pour l'utilisateur {}", userId);

            Integer balance = paymentServiceClient
                .get()
                .uri("/api/v1/tokens/balance?userId=" + userId)
                .retrieve()
                .bodyToMono(Integer.class)
                .block();

            return balance != null && balance >= requiredTokens;

        } catch (Exception e) {
            log.error("Erreur lors de la vérification du solde de jetons", e);
            return false;
        }
    }

    /**
     * Réserve des jetons pour une opération.
     * 
     * Les jetons sont bloqués mais pas encore consommés.
     * 
     * @param userId ID de l'utilisateur
     * @param amount Nombre de jetons à réserver
     * @param reference Référence de la transaction (videoId)
     */
    public void reserveTokens(UUID userId, int amount, String reference) {
        try {
            log.info("Réservation de {} jetons pour l'utilisateur {}", amount, userId);

            paymentServiceClient
                .post()
                .uri("/api/v1/tokens/reserve")
                .bodyValue(new TokenReservationRequest(userId, amount, reference))
                .retrieve()
                .toBodilessEntity()
                .block();

            log.info("Jetons réservés avec succès");

        } catch (Exception e) {
            log.error("Erreur lors de la réservation des jetons", e);
            throw new RuntimeException("Échec de la réservation des jetons", e);
        }
    }

    /**
     * Consomme les jetons réservés.
     * 
     * @param userId ID de l'utilisateur
     * @param reference Référence de la transaction
     */
    public void consumeTokens(UUID userId, String reference) {
        try {
            log.info("Consommation des jetons pour la référence {}", reference);

            paymentServiceClient
                .post()
                .uri("/api/v1/tokens/consume")
                .bodyValue(new TokenConsumptionRequest(userId, reference))
                .retrieve()
                .toBodilessEntity()
                .block();

            log.info("Jetons consommés avec succès");

        } catch (Exception e) {
            log.error("Erreur lors de la consommation des jetons", e);
        }
    }

    /**
     * Libère les jetons réservés en cas d'échec.
     * 
     * @param userId ID de l'utilisateur
     * @param reference Référence de la transaction
     */
    public void releaseTokens(UUID userId, String reference) {
        try {
            log.info("Libération des jetons pour la référence {}", reference);

            paymentServiceClient
                .post()
                .uri("/api/v1/tokens/release")
                .bodyValue(new TokenReleaseRequest(userId, reference))
                .retrieve()
                .toBodilessEntity()
                .block();

            log.info("Jetons libérés avec succès");

        } catch (Exception e) {
            log.error("Erreur lors de la libération des jetons", e);
        }
    }

    private record TokenReservationRequest(UUID userId, int amount, String reference) {}
    private record TokenConsumptionRequest(UUID userId, String reference) {}
    private record TokenReleaseRequest(UUID userId, String reference) {}
}

// ============================================================================
// SERVICE DE CONFIGURATION
// ============================================================================

package com.nexusai.video.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service de configuration centralisée pour la génération vidéo.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Service
public class VideoConfigurationService {

    @Value("${video.cost.base-per-second:5}")
    private int baseCostPerSecond;

    @Value("${video.queue.max-size:100}")
    private int maxQueueSize;

    @Value("${video.generation.timeout-minutes:60}")
    private int generationTimeoutMinutes;

    public int getBaseCostPerSecond() {
        return baseCostPerSecond;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public int getGenerationTimeoutMinutes() {
        return generationTimeoutMinutes;
    }
}

// ============================================================================
// SERVICE DE MONITORING DES WORKERS
// ============================================================================

package com.nexusai.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Service de monitoring des workers de génération vidéo.
 * 
 * Utilise Redis pour tracker l'état des workers actifs.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerMonitoringService {

    private static final String REDIS_KEY_WORKERS = "video:workers:active";
    private static final Duration WORKER_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Enregistre un worker comme actif.
     * 
     * @param workerId ID du worker
     */
    public void registerWorker(String workerId) {
        redisTemplate.opsForSet().add(REDIS_KEY_WORKERS, workerId);
        redisTemplate.expire(REDIS_KEY_WORKERS, WORKER_TTL);
        log.debug("Worker {} enregistré comme actif", workerId);
    }

    /**
     * Retire un worker de la liste des actifs.
     * 
     * @param workerId ID du worker
     */
    public void unregisterWorker(String workerId) {
        redisTemplate.opsForSet().remove(REDIS_KEY_WORKERS, workerId);
        log.debug("Worker {} retiré de la liste des actifs", workerId);
    }

    /**
     * Récupère le nombre de workers actifs.
     * 
     * @return Nombre de workers
     */
    public int getActiveWorkersCount() {
        Set<String> workers = redisTemplate.opsForSet().members(REDIS_KEY_WORKERS);
        return workers != null ? workers.size() : 0;
    }
}

// ============================================================================
// EXCEPTIONS PERSONNALISÉES
// ============================================================================

package com.nexusai.video.exception;

/**
 * Exception levée quand une vidéo n'est pas trouvée.
 */
public class VideoNotFoundException extends RuntimeException {
    public VideoNotFoundException(String message) {
        super(message);
    }
}

/**
 * Exception levée quand l'utilisateur n'a pas accès à une ressource.
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}

/**
 * Exception levée quand le solde de jetons est insuffisant.
 */
public class InsufficientTokensException extends RuntimeException {
    public InsufficientTokensException(String message) {
        super(message);
    }
}

/**
 * Exception levée lors de la validation des paramètres.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

// ============================================================================
// GESTIONNAIRE D'EXCEPTIONS GLOBAL
// ============================================================================

package com.nexusai.video.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour l'API vidéo.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(VideoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVideoNotFound(VideoNotFoundException ex) {
        log.warn("Vidéo non trouvée: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
            ));
    }

    @ExceptionHandler(InsufficientTokensException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientTokens(InsufficientTokensException ex) {
        log.warn("Solde insuffisant: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.PAYMENT_REQUIRED)
            .body(new ErrorResponse(
                HttpStatus.PAYMENT_REQUIRED.value(),
                ex.getMessage(),
                LocalDateTime.now()
            ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Erreur de validation: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erreur de validation des paramètres",
                errors,
                LocalDateTime.now()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Erreur non gérée", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Une erreur interne est survenue",
                LocalDateTime.now()
            ));
    }

    private record ErrorResponse(int status, String message, LocalDateTime timestamp) {}
    
    private record ValidationErrorResponse(
        int status, 
        String message, 
        Map<String, String> errors, 
        LocalDateTime timestamp
    ) {}
}
