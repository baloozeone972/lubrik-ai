// ===========================================
// MODULE 6.2 : AUDIO CORE - LOGIQUE MÉTIER
// ===========================================

// pom.xml pour nexus-audio-core
// ---------------------------------------------
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.nexusai</groupId>
        <artifactId>nexus-audio</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>nexus-audio-core</artifactId>
    <packaging>jar</packaging>
    
    <dependencies>
        <dependency>
            <groupId>com.nexusai</groupId>
            <artifactId>nexus-audio-persistence</artifactId>
        </dependency>
        <dependency>
            <groupId>com.nexusai</groupId>
            <artifactId>nexus-audio-stt</artifactId>
        </dependency>
        <dependency>
            <groupId>com.nexusai</groupId>
            <artifactId>nexus-audio-tts</artifactId>
        </dependency>
        <dependency>
            <groupId>com.nexusai</groupId>
            <artifactId>nexus-audio-storage</artifactId>
        </dependency>
        <dependency>
            <groupId>com.nexusai</groupId>
            <artifactId>nexus-audio-emotion</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>

// ===========================================
// DOMAIN MODELS
// ===========================================

package com.nexusai.audio.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle de domaine représentant un message vocal.
 * 
 * <p>Un message vocal contient l'enregistrement audio d'un utilisateur
 * ou d'un compagnon IA, avec sa transcription et l'analyse émotionnelle.</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessage {
    
    /**
     * Identifiant unique du message vocal.
     */
    private UUID id;
    
    /**
     * ID de la conversation à laquelle appartient ce message.
     */
    private String conversationId;
    
    /**
     * ID de l'utilisateur émetteur.
     */
    private UUID userId;
    
    /**
     * ID du compagnon IA (si applicable).
     */
    private String companionId;
    
    /**
     * Type d'émetteur : USER ou COMPANION.
     */
    private SenderType sender;
    
    /**
     * URL de stockage du fichier audio.
     */
    private String audioUrl;
    
    /**
     * Durée de l'enregistrement en secondes.
     */
    private Integer durationSeconds;
    
    /**
     * Taille du fichier en octets.
     */
    private Long fileSizeBytes;
    
    /**
     * Transcription textuelle du message.
     */
    private String transcription;
    
    /**
     * Langue détectée de la transcription (code ISO 639-1).
     */
    private String transcriptionLanguage;
    
    /**
     * Émotion détectée dans le message vocal.
     */
    private String emotionDetected;
    
    /**
     * Score de confiance de la détection émotionnelle (0.0 à 1.0).
     */
    private Float emotionConfidence;
    
    /**
     * Date et heure de création du message.
     */
    private LocalDateTime createdAt;
    
    /**
     * Énumération des types d'émetteur.
     */
    public enum SenderType {
        USER,
        COMPANION
    }
}

package com.nexusai.audio.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Modèle de domaine représentant un appel vocal en temps réel.
 * 
 * <p>Gère les sessions d'appels vocaux WebRTC entre un utilisateur
 * et son compagnon IA.</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCall {
    
    /**
     * Identifiant unique de l'appel vocal.
     */
    private UUID id;
    
    /**
     * ID de la conversation associée.
     */
    private String conversationId;
    
    /**
     * ID de l'utilisateur.
     */
    private UUID userId;
    
    /**
     * ID du compagnon IA.
     */
    private String companionId;
    
    /**
     * Statut actuel de l'appel.
     */
    private CallStatus status;
    
    /**
     * ID de session WebRTC.
     */
    private String webrtcSessionId;
    
    /**
     * Date et heure de début de l'appel.
     */
    private LocalDateTime startedAt;
    
    /**
     * Date et heure de fin de l'appel.
     */
    private LocalDateTime endedAt;
    
    /**
     * Durée totale de l'appel en secondes.
     */
    private Integer durationSeconds;
    
    /**
     * Métriques de qualité de l'appel.
     */
    private Map<String, Object> qualityMetrics;
    
    /**
     * Date de création de l'enregistrement.
     */
    private LocalDateTime createdAt;
    
    /**
     * Énumération des statuts d'appel possibles.
     */
    public enum CallStatus {
        INITIATED,
        RINGING,
        CONNECTED,
        ENDED,
        FAILED
    }
}

package com.nexusai.audio.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle de domaine représentant le profil vocal d'un compagnon IA.
 * 
 * <p>Contient les paramètres de personnalisation de la voix du compagnon,
 * utilisés lors de la synthèse vocale (TTS).</p>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceProfile {
    
    /**
     * Identifiant unique du profil vocal.
     */
    private UUID id;
    
    /**
     * ID du compagnon associé.
     */
    private String companionId;
    
    /**
     * Fournisseur de TTS utilisé (ex: ELEVENLABS, COQUI).
     */
    private String provider;
    
    /**
     * ID de la voix chez le fournisseur.
     */
    private String voiceId;
    
    /**
     * Hauteur tonale de la voix (-1.0 à 1.0).
     */
    private Float pitch;
    
    /**
     * Vitesse d'élocution (0.5 à 2.0).
     */
    private Float speed;
    
    /**
     * Style de voix (ex: conversational, energetic, calm).
     */
    private String style;
    
    /**
     * URL d'une voix personnalisée (clonée).
     */
    private String customVoiceUrl;
    
    /**
     * Date de création du profil.
     */
    private LocalDateTime createdAt;
    
    /**
     * Date de dernière mise à jour.
     */
    private LocalDateTime updatedAt;
}

// ===========================================
// EXCEPTIONS MÉTIER
// ===========================================

package com.nexusai.audio.core.exception;

/**
 * Exception levée lors d'erreurs de traitement audio.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
public class AudioProcessingException extends RuntimeException {
    
    public AudioProcessingException(String message) {
        super(message);
    }
    
    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.nexusai.audio.core.exception;

/**
 * Exception levée lors d'erreurs de transcription audio.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
public class TranscriptionException extends RuntimeException {
    
    public TranscriptionException(String message) {
        super(message);
    }
    
    public TranscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.nexusai.audio.core.exception;

/**
 * Exception levée lors d'erreurs de synthèse vocale.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
public class SynthesisException extends RuntimeException {
    
    public SynthesisException(String message) {
        super(message);
    }
    
    public SynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}

// ===========================================
// SERVICES MÉTIER
// ===========================================

package com.nexusai.audio.core.service;

import com.nexusai.audio.core.domain.VoiceMessage;
import com.nexusai.audio.core.exception.AudioProcessingException;
import com.nexusai.audio.persistence.entity.VoiceMessageEntity;
import com.nexusai.audio.persistence.repository.VoiceMessageRepository;
import com.nexusai.audio.persistence.mapper.VoiceMessageMapper;
import com.nexusai.audio.stt.service.WhisperSTTService;
import com.nexusai.audio.storage.service.AudioStorageService;
import com.nexusai.audio.emotion.service.EmotionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de gestion des messages vocaux.
 * 
 * <p>Ce service orchestre le traitement complet d'un message vocal :</p>
 * <ul>
 *   <li>Upload du fichier audio vers le stockage</li>
 *   <li>Transcription du contenu audio en texte</li>
 *   <li>Analyse émotionnelle du message</li>
 *   <li>Sauvegarde en base de données</li>
 *   <li>Publication d'événements Kafka</li>
 * </ul>
 * 
 * <p><strong>Usage :</strong></p>
 * <pre>{@code
 * VoiceMessage message = voiceMessageService.createVoiceMessage(
 *     audioFile,
 *     conversationId,
 *     userId,
 *     VoiceMessage.SenderType.USER
 * );
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceMessageService {
    
    private final VoiceMessageRepository voiceMessageRepository;
    private final VoiceMessageMapper voiceMessageMapper;
    private final AudioStorageService audioStorageService;
    private final WhisperSTTService whisperSTTService;
    private final EmotionAnalysisService emotionAnalysisService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_VOICE_MESSAGE_CREATED = "voice.message.created";
    
    /**
     * Crée un nouveau message vocal à partir d'un fichier audio.
     * 
     * <p>Le traitement est effectué de manière synchrone dans l'ordre suivant :</p>
     * <ol>
     *   <li>Validation du fichier audio</li>
     *   <li>Upload vers S3/MinIO</li>
     *   <li>Transcription via Whisper API</li>
     *   <li>Analyse émotionnelle</li>
     *   <li>Sauvegarde en base</li>
     *   <li>Publication événement Kafka</li>
     * </ol>
     * 
     * @param audioFile Fichier audio à traiter
     * @param conversationId ID de la conversation
     * @param userId ID de l'utilisateur
     * @param senderType Type d'émetteur (USER ou COMPANION)
     * @return Le message vocal créé
     * @throws AudioProcessingException Si une erreur survient lors du traitement
     */
    @Transactional
    public VoiceMessage createVoiceMessage(
            MultipartFile audioFile,
            String conversationId,
            UUID userId,
            VoiceMessage.SenderType senderType) {
        
        log.info("Création d'un message vocal pour conversationId={}, userId={}",
                conversationId, userId);
        
        try {
            // 1. Upload du fichier audio
            log.debug("Upload du fichier audio vers le stockage");
            String audioUrl = audioStorageService.uploadAudio(
                    audioFile,
                    "voice-messages"
            );
            
            // 2. Transcription audio → texte
            log.debug("Transcription du fichier audio");
            var transcriptionResult = whisperSTTService.transcribe(audioFile);
            
            // 3. Analyse émotionnelle
            log.debug("Analyse émotionnelle du message");
            var emotionResult = emotionAnalysisService.analyzeEmotion(
                    audioFile
            );
            
            // 4. Création du domaine object
            VoiceMessage voiceMessage = VoiceMessage.builder()
                    .id(UUID.randomUUID())
                    .conversationId(conversationId)
                    .userId(userId)
                    .sender(senderType)
                    .audioUrl(audioUrl)
                    .durationSeconds(calculateDuration(audioFile))
                    .fileSizeBytes(audioFile.getSize())
                    .transcription(transcriptionResult.getText())
                    .transcriptionLanguage(transcriptionResult.getLanguage())
                    .emotionDetected(emotionResult.getEmotion())
                    .emotionConfidence(emotionResult.getConfidence())
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // 5. Sauvegarde en base de données
            VoiceMessageEntity entity = voiceMessageMapper.toEntity(voiceMessage);
            entity = voiceMessageRepository.save(entity);
            
            VoiceMessage savedMessage = voiceMessageMapper.toDomain(entity);
            
            // 6. Publication événement Kafka
            publishVoiceMessageCreatedEvent(savedMessage);
            
            log.info("Message vocal créé avec succès : id={}", savedMessage.getId());
            return savedMessage;
            
        } catch (Exception e) {
            log.error("Erreur lors de la création du message vocal", e);
            throw new AudioProcessingException(
                    "Échec de la création du message vocal", e
            );
        }
    }
    
    /**
     * Récupère un message vocal par son ID.
     * 
     * @param id ID du message vocal
     * @return Le message vocal trouvé
     * @throws AudioProcessingException Si le message n'existe pas
     */
    @Transactional(readOnly = true)
    public VoiceMessage getVoiceMessageById(UUID id) {
        log.debug("Récupération du message vocal id={}", id);
        
        return voiceMessageRepository.findById(id)
                .map(voiceMessageMapper::toDomain)
                .orElseThrow(() -> new AudioProcessingException(
                        "Message vocal introuvable : " + id
                ));
    }
    
    /**
     * Récupère tous les messages vocaux d'une conversation.
     * 
     * @param conversationId ID de la conversation
     * @return Liste des messages vocaux
     */
    @Transactional(readOnly = true)
    public List<VoiceMessage> getVoiceMessagesByConversation(String conversationId) {
        log.debug("Récupération des messages vocaux pour conversationId={}", 
                conversationId);
        
        return voiceMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(voiceMessageMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    /**
     * Supprime un message vocal.
     * 
     * <p>Cette opération supprime également le fichier audio du stockage.</p>
     * 
     * @param id ID du message vocal à supprimer
     */
    @Transactional
    public void deleteVoiceMessage(UUID id) {
        log.info("Suppression du message vocal id={}", id);
        
        VoiceMessage voiceMessage = getVoiceMessageById(id);
        
        // Suppression du fichier audio du stockage
        audioStorageService.deleteAudio(voiceMessage.getAudioUrl());
        
        // Suppression de la base de données
        voiceMessageRepository.deleteById(id);
        
        log.info("Message vocal supprimé avec succès : id={}", id);
    }
    
    /**
     * Calcule la durée d'un fichier audio.
     * 
     * @param audioFile Fichier audio
     * @return Durée en secondes
     */
    private Integer calculateDuration(MultipartFile audioFile) {
        // TODO: Implémenter le calcul réel de la durée
        // Pour l'instant, estimation basée sur la taille du fichier
        // 1 Mo ≈ 60 secondes pour un MP3 128kbps
        long sizeInMB = audioFile.getSize() / (1024 * 1024);
        return (int) (sizeInMB * 60);
    }
    
    /**
     * Publie un événement Kafka lors de la création d'un message vocal.
     * 
     * @param voiceMessage Message vocal créé
     */
    private void publishVoiceMessageCreatedEvent(VoiceMessage voiceMessage) {
        try {
            kafkaTemplate.send(TOPIC_VOICE_MESSAGE_CREATED, 
                    voiceMessage.getId().toString(), 
                    voiceMessage);
            log.debug("Événement publié sur Kafka : {}", TOPIC_VOICE_MESSAGE_CREATED);
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement Kafka", e);
            // On ne lève pas d'exception pour ne pas bloquer le flux principal
        }
    }
}
