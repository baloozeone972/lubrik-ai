// ============================================================================
// PACKAGE: com.nexusai.companion.service
// Description: Services utilitaires
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.repository.CompanionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service de vérification des quotas utilisateur.
 * Communique avec le module Payment pour vérifier les limites.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuotaService {
    
    private final CompanionRepository companionRepository;
    private final RestTemplate restTemplate;
    
    @Value("${companion.limits.free}")
    private int freeLimit;
    
    @Value("${companion.limits.standard}")
    private int standardLimit;
    
    @Value("${companion.limits.premium}")
    private int premiumLimit;
    
    @Value("${companion.limits.vip-plus}")
    private int vipPlusLimit;
    
    /**
     * Vérifie si l'utilisateur peut créer un nouveau compagnon.
     * Appelle le service Payment pour récupérer le plan d'abonnement.
     * 
     * @param userId ID de l'utilisateur
     * @return true si l'utilisateur peut créer un compagnon
     */
    public boolean canCreateCompanion(String userId) {
        long currentCount = companionRepository.countByUserId(userId);
        
        // Récupérer le plan d'abonnement de l'utilisateur
        String subscriptionPlan = getUserSubscriptionPlan(userId);
        int limit = getLimitForPlan(subscriptionPlan);
        
        log.debug("User {} has {}/{} companions (plan: {})", 
                  userId, currentCount, limit, subscriptionPlan);
        
        return currentCount < limit;
    }
    
    /**
     * Récupère le plan d'abonnement de l'utilisateur.
     * Fait un appel au module Payment.
     * 
     * @param userId ID de l'utilisateur
     * @return Plan d'abonnement (FREE, STANDARD, PREMIUM, VIP_PLUS)
     */
    private String getUserSubscriptionPlan(String userId) {
        try {
            String url = "http://payment-service/api/v1/subscriptions/current?userId=" + userId;
            
            // TODO: Implémenter avec Feign Client ou WebClient
            // Pour l'instant, retourner FREE par défaut
            return "FREE";
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du plan: {}", e.getMessage());
            return "FREE"; // Default en cas d'erreur
        }
    }
    
    /**
     * Retourne la limite pour un plan donné.
     * 
     * @param plan Plan d'abonnement
     * @return Nombre maximum de compagnons
     */
    private int getLimitForPlan(String plan) {
        return switch (plan.toUpperCase()) {
            case "STANDARD" -> standardLimit;
            case "PREMIUM" -> premiumLimit;
            case "VIP_PLUS" -> vipPlusLimit;
            default -> freeLimit;
        };
    }
}

// ============================================================================
// FICHIER: EventPublisherService.java
// Description: Service de publication d'événements Kafka
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.domain.Companion;
import com.nexusai.companion.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service de publication d'événements vers Kafka.
 * Permet la communication asynchrone avec les autres modules.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisherService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_COMPANION_EVENTS = "companion.events";
    
    /**
     * Publie un événement de création de compagnon.
     */
    public void publishCompanionCreated(Companion companion) {
        CompanionEvent event = CompanionEvent.builder()
            .type("COMPANION_CREATED")
            .companionId(companion.getId())
            .userId(companion.getUserId())
            .timestamp(companion.getCreatedAt())
            .build();
        
        publishEvent(event);
    }
    
    /**
     * Publie un événement de mise à jour de compagnon.
     */
    public void publishCompanionUpdated(Companion companion) {
        CompanionEvent event = CompanionEvent.builder()
            .type("COMPANION_UPDATED")
            .companionId(companion.getId())
            .userId(companion.getUserId())
            .timestamp(companion.getUpdatedAt())
            .build();
        
        publishEvent(event);
    }
    
    /**
     * Publie un événement de suppression de compagnon.
     */
    public void publishCompanionDeleted(String companionId, String userId) {
        CompanionEvent event = CompanionEvent.builder()
            .type("COMPANION_DELETED")
            .companionId(companionId)
            .userId(userId)
            .timestamp(java.time.Instant.now())
            .build();
        
        publishEvent(event);
    }
    
    /**
     * Publie un événement d'évolution de compagnon.
     */
    public void publishCompanionEvolved(Companion companion) {
        CompanionEvent event = CompanionEvent.builder()
            .type("COMPANION_EVOLVED")
            .companionId(companion.getId())
            .userId(companion.getUserId())
            .timestamp(companion.getLastEvolutionDate())
            .build();
        
        publishEvent(event);
    }
    
    private void publishEvent(CompanionEvent event) {
        try {
            kafkaTemplate.send(TOPIC_COMPANION_EVENTS, event.getCompanionId(), event);
            log.info("Événement publié: {} - companionId: {}", 
                     event.getType(), event.getCompanionId());
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement: {}", e.getMessage(), e);
        }
    }
}

// ============================================================================
// FICHIER: StorageService.java
// Description: Service de stockage S3/MinIO pour les avatars
// ============================================================================

package com.nexusai.companion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

/**
 * Service de gestion du stockage des fichiers (avatars).
 * Utilise S3 ou MinIO.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService {
    
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    /**
     * Upload un fichier avatar vers S3.
     * 
     * @param file Fichier à uploader
     * @param companionId ID du compagnon
     * @return URL du fichier uploadé
     */
    public String uploadAvatar(MultipartFile file, String companionId) {
        try {
            String fileName = generateFileName(companionId, file.getOriginalFilename());
            String key = "avatars/" + fileName;
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
            
            s3Client.putObject(
                putRequest,
                RequestBody.fromBytes(file.getBytes())
            );
            
            log.info("Avatar uploadé: {}", key);
            return getFileUrl(key);
            
        } catch (IOException e) {
            log.error("Erreur lors de l'upload: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'upload du fichier", e);
        }
    }
    
    /**
     * Supprime un fichier du storage.
     * 
     * @param fileUrl URL du fichier à supprimer
     */
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
            
            s3Client.deleteObject(deleteRequest);
            
            log.info("Fichier supprimé: {}", key);
            
        } catch (Exception e) {
            log.error("Erreur lors de la suppression: {}", e.getMessage(), e);
        }
    }
    
    private String generateFileName(String companionId, String originalName) {
        String extension = originalName.substring(originalName.lastIndexOf("."));
        return companionId + "_" + UUID.randomUUID() + extension;
    }
    
    private String getFileUrl(String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }
    
    private String extractKeyFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}

// ============================================================================
// PACKAGE: com.nexusai.companion.exception
// Description: Exceptions personnalisées
// ============================================================================

package com.nexusai.companion.exception;

/**
 * Exception levée lorsque le quota de compagnons est dépassé.
 */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}

/**
 * Exception levée lorsqu'un compagnon n'est pas trouvé.
 */
public class CompanionNotFoundException extends RuntimeException {
    public CompanionNotFoundException(String message) {
        super(message);
    }
}

/**
 * Exception levée lors d'un nom de compagnon en double.
 */
public class DuplicateNameException extends RuntimeException {
    public DuplicateNameException(String message) {
        super(message);
    }
}

/**
 * Exception levée lors d'un accès non autorisé.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

// ============================================================================
// FICHIER: GlobalExceptionHandler.java
// Description: Gestionnaire global des exceptions
// ============================================================================

package com.nexusai.companion.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions.
 * Capture et formate les erreurs pour l'API.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CompanionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanionNotFound(
            CompanionNotFoundException ex) {
        
        log.error("Compagnon non trouvé: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(
            QuotaExceededException ex) {
        
        log.error("Quota dépassé: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Quota Exceeded")
            .message(ex.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex) {
        
        log.error("Accès non autorisé: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message(ex.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateName(
            DuplicateNameException ex) {
        
        log.error("Nom en double: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Conflict")
            .message(ex.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Erreurs de validation: {}", errors);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Erreurs de validation")
            .validationErrors(errors)
            .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex) {
        
        log.error("Erreur interne: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("Une erreur interne est survenue")
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

/**
 * DTO pour les réponses d'erreur.
 */
@lombok.Data
@lombok.Builder
class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;
}

// ============================================================================
// PACKAGE: com.nexusai.companion.mapper
// Description: Mapper DTO <-> Entité (MapStruct)
// ============================================================================

package com.nexusai.companion.mapper;

import com.nexusai.companion.domain.Companion;
import com.nexusai.companion.dto.*;
import org.mapstruct.*;

/**
 * Mapper pour convertir entre DTOs et entités.
 * Utilise MapStruct pour générer automatiquement le code.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface CompanionMapper {
    
    /**
     * Convertit un CreateCompanionRequest en Companion.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "geneticProfile", ignore = true)
    @Mapping(target = "emotionalState", ignore = true)
    @Mapping(target = "isPublic", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastEvolutionDate", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Companion toEntity(CreateCompanionRequest request);
    
    /**
     * Convertit un Companion en CompanionResponse.
     */
    CompanionResponse toResponse(Companion companion);
    
    /**
     * Convertit un AppearanceDto en Appearance.
     */
    Companion.Appearance toAppearanceEntity(AppearanceDto dto);
    
    /**
     * Convertit un PersonalityDto en Personality.
     */
    Companion.Personality toPersonalityEntity(PersonalityDto dto);
    
    /**
     * Convertit un VoiceDto en Voice.
     */
    Companion.Voice toVoiceEntity(VoiceDto dto);
}