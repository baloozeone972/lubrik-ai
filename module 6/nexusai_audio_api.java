// ===========================================
// MODULE 6.1 : AUDIO API - CONTROLLERS REST
// ===========================================

// pom.xml pour nexus-audio-api
// ---------------------------------------------
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.nexusai</groupId>
        <artifactId>nexus-audio</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>nexus-audio-api</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>com.nexusai</groupId>
            <artifactId>nexus-audio-core</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>

// ===========================================
// APPLICATION PRINCIPALE
// ===========================================

package com.nexusai.audio.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Application Spring Boot principale pour le module Audio.
 * 
 * <p>Cette application expose les APIs REST pour :</p>
 * <ul>
 *   <li>Messages vocaux asynchrones</li>
 *   <li>Appels vocaux temps réel (WebRTC)</li>
 *   <li>Profils vocaux des compagnons</li>
 *   <li>Transcription et synthèse vocale</li>
 * </ul>
 * 
 * <p><strong>URLs importantes :</strong></p>
 * <ul>
 *   <li>API REST: http://localhost:8083/api/v1/audio/*</li>
 *   <li>WebSocket: ws://localhost:8083/ws/audio/*</li>
 *   <li>Swagger UI: http://localhost:8083/swagger-ui.html</li>
 *   <li>Actuator: http://localhost:8083/actuator</li>
 * </ul>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.nexusai.audio")
public class AudioApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AudioApplication.class, args);
    }
}

// ===========================================
// DTOs (DATA TRANSFER OBJECTS)
// ===========================================

package com.nexusai.audio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de requête pour l'upload d'un message vocal.
 * 
 * <p>Ce DTO est utilisé avec Multipart Form Data pour envoyer
 * un fichier audio accompagné de ses métadonnées.</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessageRequest {
    
    /**
     * ID de la conversation (obligatoire).
     */
    @NotBlank(message = "conversationId est obligatoire")
    private String conversationId;
    
    /**
     * ID de l'utilisateur (obligatoire).
     */
    @NotNull(message = "userId est obligatoire")
    private UUID userId;
    
    /**
     * ID du compagnon (optionnel).
     */
    private String companionId;
    
    /**
     * Type d'émetteur (USER ou COMPANION).
     */
    @NotBlank(message = "senderType est obligatoire")
    private String senderType;
    
    // Le fichier audio est transmis séparément via MultipartFile
}

package com.nexusai.audio.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un message vocal.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessageResponse {
    
    private UUID id;
    private String conversationId;
    private UUID userId;
    private String companionId;
    private String sender;
    private String audioUrl;
    private Integer durationSeconds;
    private Long fileSizeBytes;
    private String transcription;
    private String transcriptionLanguage;
    private String emotionDetected;
    private Float emotionConfidence;
    private LocalDateTime createdAt;
}

package com.nexusai.audio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO de requête pour initier un appel vocal.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCallRequest {
    
    @NotNull(message = "userId est obligatoire")
    private UUID userId;
    
    @NotBlank(message = "companionId est obligatoire")
    private String companionId;
    
    @NotBlank(message = "conversationId est obligatoire")
    private String conversationId;
}

package com.nexusai.audio.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de réponse pour un appel vocal.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCallResponse {
    
    private UUID id;
    private String conversationId;
    private UUID userId;
    private String companionId;
    private String status;
    private String webrtcSessionId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private Map<String, Object> qualityMetrics;
    private LocalDateTime createdAt;
}

package com.nexusai.audio.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour créer/modifier un profil vocal.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceProfileRequest {
    
    @NotBlank(message = "companionId est obligatoire")
    private String companionId;
    
    private String provider;
    
    private String voiceId;
    
    @Min(value = -1, message = "pitch doit être >= -1.0")
    @Max(value = 1, message = "pitch doit être <= 1.0")
    private Float pitch;
    
    @Min(value = 0, message = "speed doit être >= 0.5")
    @Max(value = 2, message = "speed doit être <= 2.0")
    private Float speed;
    
    private String style;
    
    private String customVoiceUrl;
}

package com.nexusai.audio.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un profil vocal.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceProfileResponse {
    
    private UUID id;
    private String companionId;
    private String provider;
    private String voiceId;
    private Float pitch;
    private Float speed;
    private String style;
    private String customVoiceUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// ===========================================
// CONTROLLERS REST
// ===========================================

package com.nexusai.audio.api.controller;

import com.nexusai.audio.api.dto.VoiceMessageRequest;
import com.nexusai.audio.api.dto.VoiceMessageResponse;
import com.nexusai.audio.core.domain.VoiceMessage;
import com.nexusai.audio.core.service.VoiceMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des messages vocaux.
 * 
 * <p>Ce contrôleur expose les endpoints suivants :</p>
 * <ul>
 *   <li>POST /api/v1/audio/voice-messages - Upload message vocal</li>
 *   <li>GET /api/v1/audio/voice-messages/{id} - Récupérer un message</li>
 *   <li>GET /api/v1/audio/voice-messages/conversation/{id} - Messages d'une conversation</li>
 *   <li>DELETE /api/v1/audio/voice-messages/{id} - Supprimer un message</li>
 * </ul>
 * 
 * <p><strong>Exemple d'utilisation (cURL) :</strong></p>
 * <pre>{@code
 * curl -X POST "http://localhost:8083/api/v1/audio/voice-messages" \
 *   -H "Authorization: Bearer YOUR_JWT_TOKEN" \
 *   -F "audioFile=@message.mp3" \
 *   -F "conversationId=conv-123" \
 *   -F "userId=550e8400-e29b-41d4-a716-446655440000" \
 *   -F "senderType=USER"
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audio/voice-messages")
@RequiredArgsConstructor
@Tag(name = "Voice Messages", description = "APIs de gestion des messages vocaux")
public class VoiceMessageController {
    
    private final VoiceMessageService voiceMessageService;
    
    /**
     * Upload et traitement d'un message vocal.
     * 
     * <p>Le traitement inclut :</p>
     * <ul>
     *   <li>Stockage du fichier audio</li>
     *   <li>Transcription (Speech-to-Text)</li>
     *   <li>Analyse émotionnelle</li>
     * </ul>
     * 
     * @param audioFile Fichier audio (MP3, WAV, M4A)
     * @param request Métadonnées du message
     * @return Le message vocal créé
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload un message vocal",
        description = "Upload et traite un message vocal avec transcription automatique"
    )
    @ApiResponse(responseCode = "201", description = "Message vocal créé avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "500", description = "Erreur serveur")
    public ResponseEntity<VoiceMessageResponse> uploadVoiceMessage(
            @Parameter(description = "Fichier audio (MP3, WAV, M4A)")
            @RequestParam("audioFile") MultipartFile audioFile,
            
            @Valid @ModelAttribute VoiceMessageRequest request) {
        
        log.info("Réception d'un message vocal : conversationId={}, userId={}", 
                request.getConversationId(), request.getUserId());
        
        VoiceMessage voiceMessage = voiceMessageService.createVoiceMessage(
                audioFile,
                request.getConversationId(),
                request.getUserId(),
                VoiceMessage.SenderType.valueOf(request.getSenderType())
        );
        
        VoiceMessageResponse response = mapToResponse(voiceMessage);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    /**
     * Récupère un message vocal par son ID.
     * 
     * @param id ID du message vocal
     * @return Le message vocal
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un message vocal par ID")
    @ApiResponse(responseCode = "200", description = "Message trouvé")
    @ApiResponse(responseCode = "404", description = "Message introuvable")
    public ResponseEntity<VoiceMessageResponse> getVoiceMessage(
            @PathVariable UUID id) {
        
        log.debug("Récupération du message vocal : id={}", id);
        
        VoiceMessage voiceMessage = voiceMessageService.getVoiceMessageById(id);
        VoiceMessageResponse response = mapToResponse(voiceMessage);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Récupère tous les messages vocaux d'une conversation.
     * 
     * @param conversationId ID de la conversation
     * @return Liste des messages vocaux
     */
    @GetMapping("/conversation/{conversationId}")
    @Operation(
        summary = "Récupérer les messages vocaux d'une conversation",
        description = "Retourne tous les messages vocaux d'une conversation, triés par date"
    )
    @ApiResponse(responseCode = "200", description = "Liste des messages")
    public ResponseEntity<List<VoiceMessageResponse>> getConversationMessages(
            @PathVariable String conversationId) {
        
        log.debug("Récupération des messages vocaux : conversationId={}", conversationId);
        
        List<VoiceMessage> messages = voiceMessageService
                .getVoiceMessagesByConversation(conversationId);
        
        List<VoiceMessageResponse> responses = messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Supprime un message vocal.
     * 
     * @param id ID du message vocal à supprimer
     * @return Réponse HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un message vocal",
        description = "Supprime le message vocal et son fichier audio associé"
    )
    @ApiResponse(responseCode = "204", description = "Message supprimé")
    @ApiResponse(responseCode = "404", description = "Message introuvable")
    public ResponseEntity<Void> deleteVoiceMessage(@PathVariable UUID id) {
        
        log.info("Suppression du message vocal : id={}", id);
        
        voiceMessageService.deleteVoiceMessage(id);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Convertit un domaine VoiceMessage en DTO Response.
     */
    private VoiceMessageResponse mapToResponse(VoiceMessage voiceMessage) {
        return VoiceMessageResponse.builder()
                .id(voiceMessage.getId())
                .conversationId(voiceMessage.getConversationId())
                .userId(voiceMessage.getUserId())
                .companionId(voiceMessage.getCompanionId())
                .sender(voiceMessage.getSender().name())
                .audioUrl(voiceMessage.getAudioUrl())
                .durationSeconds(voiceMessage.getDurationSeconds())
                .fileSizeBytes(voiceMessage.getFileSizeBytes())
                .transcription(voiceMessage.getTranscription())
                .transcriptionLanguage(voiceMessage.getTranscriptionLanguage())
                .emotionDetected(voiceMessage.getEmotionDetected())
                .emotionConfidence(voiceMessage.getEmotionConfidence())
                .createdAt(voiceMessage.getCreatedAt())
                .build();
    }
}

package com.nexusai.audio.api.controller;

import com.nexusai.audio.api.dto.VoiceCallRequest;
import com.nexusai.audio.api.dto.VoiceCallResponse;
import com.nexusai.audio.core.domain.VoiceCall;
import com.nexusai.audio.core.service.VoiceCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des appels vocaux temps réel.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audio/calls")
@RequiredArgsConstructor
@Tag(name = "Voice Calls", description = "APIs de gestion des appels vocaux WebRTC")
public class VoiceCallController {
    
    private final VoiceCallService voiceCallService;
    
    /**
     * Initie un nouvel appel vocal.
     * 
     * @param request Requête d'appel
     * @return L'appel vocal créé
     */
    @PostMapping("/initiate")
    @Operation(
        summary = "Initier un appel vocal",
        description = "Crée une session d'appel WebRTC entre l'utilisateur et son compagnon"
    )
    public ResponseEntity<VoiceCallResponse> initiateCall(
            @Valid @RequestBody VoiceCallRequest request) {
        
        log.info("Initiation d'un appel : userId={}, companionId={}", 
                request.getUserId(), request.getCompanionId());
        
        VoiceCall voiceCall = voiceCallService.initiateCall(
                request.getUserId(),
                request.getCompanionId(),
                request.getConversationId()
        );
        
        VoiceCallResponse response = mapToResponse(voiceCall);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    /**
     * Récupère un appel vocal par son ID.
     * 
     * @param id ID de l'appel
     * @return L'appel vocal
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un appel vocal par ID")
    public ResponseEntity<VoiceCallResponse> getCall(@PathVariable UUID id) {
        
        VoiceCall voiceCall = voiceCallService.getCallById(id);
        VoiceCallResponse response = mapToResponse(voiceCall);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Termine un appel vocal.
     * 
     * @param id ID de l'appel
     * @param qualityMetrics Métriques de qualité
     * @return L'appel terminé
     */
    @PostMapping("/{id}/end")
    @Operation(summary = "Terminer un appel vocal")
    public ResponseEntity<VoiceCallResponse> endCall(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> qualityMetrics) {
        
        log.info("Terminaison de l'appel : callId={}", id);
        
        VoiceCall voiceCall = voiceCallService.endCall(id, qualityMetrics);
        VoiceCallResponse response = mapToResponse(voiceCall);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Récupère tous les appels d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des appels
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Récupérer les appels d'un utilisateur")
    public ResponseEntity<List<VoiceCallResponse>> getUserCalls(
            @PathVariable UUID userId) {
        
        List<VoiceCall> calls = voiceCallService.getCallsByUser(userId);
        
        List<VoiceCallResponse> responses = calls.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Récupère l'appel actif d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return L'appel actif ou 404
     */
    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Récupérer l'appel actif d'un utilisateur")
    public ResponseEntity<VoiceCallResponse> getActiveCall(@PathVariable UUID userId) {
        
        VoiceCall activeCall = voiceCallService.getActiveCall(userId);
        
        if (activeCall == null) {
            return ResponseEntity.notFound().build();
        }
        
        VoiceCallResponse response = mapToResponse(activeCall);
        return ResponseEntity.ok(response);
    }
    
    private VoiceCallResponse mapToResponse(VoiceCall voiceCall) {
        return VoiceCallResponse.builder()
                .id(voiceCall.getId())
                .conversationId(voiceCall.getConversationId())
                .userId(voiceCall.getUserId())
                .companionId(voiceCall.getCompanionId())
                .status(voiceCall.getStatus().name())
                .webrtcSessionId(voiceCall.getWebrtcSessionId())
                .startedAt(voiceCall.getStartedAt())
                .endedAt(voiceCall.getEndedAt())
                .durationSeconds(voiceCall.getDurationSeconds())
                .qualityMetrics(voiceCall.getQualityMetrics())
                .createdAt(voiceCall.getCreatedAt())
                .build();
    }
}
