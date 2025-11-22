package com.nexusai.conversation.common.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

/**
 * MODULE: conversation-common
 * 
 * Contient les DTOs, enums et classes partagées entre tous les modules
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */

// ============================================================================
// REQUEST DTOs
// ============================================================================

/**
 * DTO pour créer une nouvelle conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {
    
    /**
     * ID de l'utilisateur créant la conversation
     */
    @NotBlank(message = "userId est obligatoire")
    private String userId;
    
    /**
     * ID du compagnon participant à la conversation
     */
    @NotBlank(message = "companionId est obligatoire")
    private String companionId;
    
    /**
     * Titre optionnel de la conversation
     */
    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères")
    private String title;
    
    /**
     * Indique si la conversation est éphémère (auto-suppression)
     */
    @Builder.Default
    private Boolean isEphemeral = false;
    
    /**
     * Date d'expiration pour les conversations éphémères
     */
    private Instant expiresAt;
    
    /**
     * Tags pour catégoriser la conversation
     */
    private List<String> tags;
}

/**
 * DTO pour envoyer un message dans une conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    /**
     * ID de la conversation
     */
    @NotBlank(message = "conversationId est obligatoire")
    private String conversationId;
    
    /**
     * Contenu textuel du message
     */
    @NotBlank(message = "Le contenu ne peut pas être vide")
    @Size(max = 4000, message = "Le message ne peut pas dépasser 4000 caractères")
    private String content;
    
    /**
     * Type de message (TEXT, IMAGE, AUDIO)
     */
    @NotNull(message = "Le type de message est obligatoire")
    private MessageType type;
    
    /**
     * Métadonnées additionnelles (format JSON)
     */
    private Map<String, Object> metadata;
    
    /**
     * Indique si ce message doit être sauvegardé dans l'historique
     */
    @Builder.Default
    private Boolean saveToHistory = true;
}

/**
 * DTO pour rechercher dans l'historique des conversations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchConversationRequest {
    
    /**
     * ID de la conversation dans laquelle rechercher
     */
    @NotBlank(message = "conversationId est obligatoire")
    private String conversationId;
    
    /**
     * Termes de recherche
     */
    @NotBlank(message = "La requête de recherche ne peut pas être vide")
    private String query;
    
    /**
     * Nombre maximum de résultats
     */
    @Min(value = 1, message = "Le nombre de résultats doit être au moins 1")
    @Max(value = 100, message = "Le nombre de résultats ne peut pas dépasser 100")
    @Builder.Default
    private Integer limit = 20;
    
    /**
     * Offset pour la pagination
     */
    @Min(value = 0, message = "L'offset ne peut pas être négatif")
    @Builder.Default
    private Integer offset = 0;
}

// ============================================================================
// RESPONSE DTOs
// ============================================================================

/**
 * DTO représentant une conversation complète
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    
    /**
     * Identifiant unique de la conversation
     */
    private String id;
    
    /**
     * ID de l'utilisateur
     */
    private String userId;
    
    /**
     * ID du compagnon
     */
    private String companionId;
    
    /**
     * Titre de la conversation
     */
    private String title;
    
    /**
     * Liste des messages
     */
    private List<MessageDTO> messages;
    
    /**
     * Contexte actuel de la conversation
     */
    private ConversationContext context;
    
    /**
     * Indique si la conversation est éphémère
     */
    private Boolean isEphemeral;
    
    /**
     * Date d'expiration (si éphémère)
     */
    private Instant expiresAt;
    
    /**
     * Tags de catégorisation
     */
    private List<String> tags;
    
    /**
     * Date de création
     */
    private Instant createdAt;
    
    /**
     * Date du dernier message
     */
    private Instant lastMessageAt;
}

/**
 * DTO représentant un message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    
    /**
     * Identifiant unique du message
     */
    private String id;
    
    /**
     * Expéditeur (USER ou COMPANION)
     */
    private MessageSender sender;
    
    /**
     * Contenu du message
     */
    private String content;
    
    /**
     * Type de message
     */
    private MessageType type;
    
    /**
     * Métadonnées additionnelles
     */
    private Map<String, Object> metadata;
    
    /**
     * Date d'envoi
     */
    private Instant timestamp;
    
    /**
     * Réactions au message
     */
    private List<ReactionDTO> reactions;
    
    /**
     * Émotion détectée dans le message
     */
    private EmotionType detectedEmotion;
    
    /**
     * Score de confiance de l'émotion détectée (0-1)
     */
    private Double emotionConfidence;
}

/**
 * DTO représentant le contexte d'une conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    
    /**
     * Sujets principaux abordés
     */
    private List<String> topics;
    
    /**
     * Ton émotionnel global de la conversation
     */
    private String emotionalTone;
    
    /**
     * Résumé de la conversation jusqu'à présent
     */
    private String lastSummary;
    
    /**
     * Nombre total de messages échangés
     */
    private Integer messageCount;
    
    /**
     * Durée de la conversation en minutes
     */
    private Long durationMinutes;
    
    /**
     * État émotionnel actuel du compagnon
     */
    private EmotionType companionEmotionalState;
    
    /**
     * Informations mémorisées importantes
     */
    private List<String> keyMemories;
}

/**
 * DTO représentant une réaction à un message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDTO {
    
    /**
     * Emoji de la réaction
     */
    private String emoji;
    
    /**
     * ID de l'utilisateur ayant réagi
     */
    private String userId;
    
    /**
     * Date de la réaction
     */
    private Instant timestamp;
}

// ============================================================================
// ENUMS
// ============================================================================

/**
 * Types de messages supportés
 */
public enum MessageType {
    /**
     * Message textuel standard
     */
    TEXT,
    
    /**
     * Message contenant une image
     */
    IMAGE,
    
    /**
     * Message vocal
     */
    AUDIO,
    
    /**
     * Message vidéo
     */
    VIDEO,
    
    /**
     * Message système (notifications, etc.)
     */
    SYSTEM
}

/**
 * Expéditeurs possibles d'un message
 */
public enum MessageSender {
    /**
     * Message envoyé par l'utilisateur
     */
    USER,
    
    /**
     * Message envoyé par le compagnon IA
     */
    COMPANION,
    
    /**
     * Message automatique du système
     */
    SYSTEM
}

/**
 * Types d'émotions détectables
 */
public enum EmotionType {
    /**
     * Joie, bonheur
     */
    JOY,
    
    /**
     * Tristesse
     */
    SADNESS,
    
    /**
     * Colère
     */
    ANGER,
    
    /**
     * Peur, anxiété
     */
    FEAR,
    
    /**
     * Surprise
     */
    SURPRISE,
    
    /**
     * Dégoût
     */
    DISGUST,
    
    /**
     * Amour, affection
     */
    LOVE,
    
    /**
     * État neutre
     */
    NEUTRAL,
    
    /**
     * Excitation
     */
    EXCITEMENT,
    
    /**
     * Mélancolie
     */
    MELANCHOLY
}

// ============================================================================
// EXCEPTIONS PERSONNALISÉES
// ============================================================================

/**
 * Exception levée lorsqu'une conversation n'est pas trouvée
 */
public class ConversationNotFoundException extends RuntimeException {
    
    private final String conversationId;
    
    public ConversationNotFoundException(String conversationId) {
        super("Conversation non trouvée: " + conversationId);
        this.conversationId = conversationId;
    }
    
    public String getConversationId() {
        return conversationId;
    }
}

/**
 * Exception levée lorsqu'un message est invalide
 */
public class InvalidMessageException extends RuntimeException {
    
    public InvalidMessageException(String message) {
        super(message);
    }
    
    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception levée lorsque le quota de messages est atteint
 */
public class MessageQuotaExceededException extends RuntimeException {
    
    private final String userId;
    private final int currentCount;
    private final int maxCount;
    
    public MessageQuotaExceededException(String userId, int currentCount, int maxCount) {
        super(String.format("Quota de messages dépassé pour l'utilisateur %s: %d/%d", 
                          userId, currentCount, maxCount));
        this.userId = userId;
        this.currentCount = currentCount;
        this.maxCount = maxCount;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public int getCurrentCount() {
        return currentCount;
    }
    
    public int getMaxCount() {
        return maxCount;
    }
}

// ============================================================================
// CONSTANTES
// ============================================================================

/**
 * Constantes utilisées dans le module conversation
 */
public final class ConversationConstants {
    
    private ConversationConstants() {
        // Empêcher l'instanciation
    }
    
    /**
     * Durée maximale d'une conversation en heures
     */
    public static final int MAX_CONVERSATION_DURATION_HOURS = 24;
    
    /**
     * Nombre maximum de messages par conversation
     */
    public static final int MAX_MESSAGES_PER_CONVERSATION = 10_000;
    
    /**
     * Taille maximale du contexte en caractères
     */
    public static final int MAX_CONTEXT_SIZE = 8_000;
    
    /**
     * Nombre de messages à inclure dans le contexte court terme
     */
    public static final int SHORT_TERM_MEMORY_SIZE = 20;
    
    /**
     * Durée de vie d'une conversation éphémère en heures
     */
    public static final int EPHEMERAL_CONVERSATION_TTL_HOURS = 1;
    
    /**
     * Topics Kafka
     */
    public static final String TOPIC_MESSAGE_SENT = "conversation.message.sent";
    public static final String TOPIC_CONVERSATION_CREATED = "conversation.created";
    public static final String TOPIC_CONVERSATION_DELETED = "conversation.deleted";
}
