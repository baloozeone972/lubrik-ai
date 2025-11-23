package com.nexusai.conversation.persistence;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;

/**
 * MODULE: conversation-persistence
 * 
 * Gère la persistance des conversations dans MongoDB
 * 
 * DÉVELOPPEUR ASSIGNÉ: Développeur 5
 * 
 * TÂCHES:
 * - Créer les entités MongoDB
 * - Implémenter les repositories réactifs
 * - Créer les index pour optimiser les requêtes
 * - Gérer les migrations de données
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */

// ============================================================================
// ENTITIES
// ============================================================================

/**
 * Entité représentant une conversation stockée dans MongoDB
 * 
 * Collection: conversations
 */
@Document(collection = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "user_lastMessage_idx", 
                   def = "{'userId': 1, 'lastMessageAt': -1}"),
    @CompoundIndex(name = "companion_created_idx", 
                   def = "{'companionId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "ephemeral_expires_idx", 
                   def = "{'isEphemeral': 1, 'expiresAt': 1}")
})
public class ConversationEntity {
    
    /**
     * Identifiant unique MongoDB
     */
    @Id
    private String id;
    
    /**
     * ID de l'utilisateur propriétaire
     */
    @Indexed
    private String userId;
    
    /**
     * ID du compagnon participant
     */
    @Indexed
    private String companionId;
    
    /**
     * Titre de la conversation
     */
    private String title;
    
    /**
     * Liste des messages de la conversation
     * 
     * Note: Pour des conversations très longues (>1000 messages),
     * envisager de déplacer vers une collection séparée
     */
    private List<MessageEntity> messages;
    
    /**
     * Contexte actuel de la conversation
     */
    private ContextEntity context;
    
    /**
     * Indique si la conversation est éphémère
     */
    private Boolean isEphemeral;
    
    /**
     * Date d'expiration (pour conversations éphémères)
     */
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;
    
    /**
     * Tags de catégorisation
     */
    @Indexed
    private List<String> tags;
    
    /**
     * Date de création
     */
    @CreatedDate
    private Instant createdAt;
    
    /**
     * Date du dernier message
     */
    private Instant lastMessageAt;
    
    /**
     * Version pour gestion optimiste des conflits
     */
    @Version
    private Long version;
}

/**
 * Entité représentant un message dans une conversation
 * 
 * Embedded dans ConversationEntity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {
    
    /**
     * Identifiant unique du message
     */
    private String id;
    
    /**
     * Expéditeur (USER, COMPANION, SYSTEM)
     */
    private String sender;
    
    /**
     * Contenu du message
     */
    private String content;
    
    /**
     * Type de message (TEXT, IMAGE, AUDIO, etc.)
     */
    private String type;
    
    /**
     * Métadonnées additionnelles
     */
    private Map<String, Object> metadata;
    
    /**
     * Timestamp du message
     */
    private Instant timestamp;
    
    /**
     * Réactions au message
     */
    private List<ReactionEntity> reactions;
    
    /**
     * Émotion détectée
     */
    private String detectedEmotion;
    
    /**
     * Confiance de la détection d'émotion (0-1)
     */
    private Double emotionConfidence;
}

/**
 * Entité représentant le contexte d'une conversation
 * 
 * Embedded dans ConversationEntity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextEntity {
    
    /**
     * Sujets principaux
     */
    private List<String> topics;
    
    /**
     * Ton émotionnel global
     */
    private String emotionalTone;
    
    /**
     * Dernier résumé généré
     */
    private String lastSummary;
    
    /**
     * Nombre total de messages
     */
    private Integer messageCount;
    
    /**
     * Durée totale en minutes
     */
    private Long durationMinutes;
    
    /**
     * État émotionnel actuel du compagnon
     */
    private String companionEmotionalState;
    
    /**
     * Mémoires clés extraites
     */
    private List<String> keyMemories;
}

/**
 * Entité représentant une réaction à un message
 * 
 * Embedded dans MessageEntity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionEntity {
    
    /**
     * Emoji de la réaction
     */
    private String emoji;
    
    /**
     * ID de l'utilisateur ayant réagi
     */
    private String userId;
    
    /**
     * Timestamp de la réaction
     */
    private Instant timestamp;
}

// ============================================================================
// REPOSITORIES
// ============================================================================

/**
 * Repository réactif pour les conversations
 * 
 * Utilise Spring Data MongoDB Reactive pour des opérations non-bloquantes
 */
public interface ConversationRepository extends ReactiveMongoRepository<ConversationEntity, String> {
    
    /**
     * Trouve toutes les conversations d'un utilisateur, triées par date du dernier message
     * 
     * @param userId ID de l'utilisateur
     * @return Flux de conversations
     */
    Flux<ConversationEntity> findByUserIdOrderByLastMessageAtDesc(String userId);
    
    /**
     * Trouve les conversations d'un utilisateur avec un compagnon spécifique
     * 
     * @param userId ID de l'utilisateur
     * @param companionId ID du compagnon
     * @return Flux de conversations
     */
    Flux<ConversationEntity> findByUserIdAndCompanionId(String userId, String companionId);
    
    /**
     * Trouve les conversations contenant un tag spécifique
     * 
     * @param tag Tag à rechercher
     * @return Flux de conversations
     */
    Flux<ConversationEntity> findByTagsContaining(String tag);
    
    /**
     * Compte le nombre de conversations d'un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @return Mono contenant le nombre de conversations
     */
    Mono<Long> countByUserId(String userId);
    
    /**
     * Trouve les conversations éphémères expirées
     * 
     * @param now Instant actuel
     * @return Flux de conversations expirées
     */
    Flux<ConversationEntity> findByIsEphemeralTrueAndExpiresAtBefore(Instant now);
    
    /**
     * Supprime toutes les conversations d'un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @return Mono<Void> indiquant la fin de l'opération
     */
    Mono<Void> deleteByUserId(String userId);
}

// ============================================================================
// CUSTOM REPOSITORY IMPLEMENTATION
// ============================================================================

/**
 * Interface pour les requêtes custom sur les conversations
 */
public interface ConversationCustomRepository {
    
    /**
     * Recherche textuelle dans les messages d'une conversation
     * 
     * @param conversationId ID de la conversation
     * @param searchQuery Termes de recherche
     * @param limit Nombre maximum de résultats
     * @return Flux de messages correspondants
     */
    Flux<MessageEntity> searchMessagesInConversation(
        String conversationId, 
        String searchQuery, 
        int limit
    );
    
    /**
     * Récupère les N derniers messages d'une conversation
     * 
     * @param conversationId ID de la conversation
     * @param count Nombre de messages à récupérer
     * @return Flux de messages
     */
    Flux<MessageEntity> getLastNMessages(String conversationId, int count);
    
    /**
     * Ajoute un message à une conversation de manière atomique
     * 
     * @param conversationId ID de la conversation
     * @param message Message à ajouter
     * @return Mono<ConversationEntity> la conversation mise à jour
     */
    Mono<ConversationEntity> addMessage(String conversationId, MessageEntity message);
    
    /**
     * Met à jour le contexte d'une conversation
     * 
     * @param conversationId ID de la conversation
     * @param context Nouveau contexte
     * @return Mono<Boolean> indiquant le succès
     */
    Mono<Boolean> updateContext(String conversationId, ContextEntity context);
}

/**
 * Implémentation des requêtes custom MongoDB
 */
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationCustomRepositoryImpl implements ConversationCustomRepository {
    
    private final ReactiveMongoTemplate mongoTemplate;
    
    @Override
    public Flux<MessageEntity> searchMessagesInConversation(
            String conversationId, 
            String searchQuery, 
            int limit) {
        
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(conversationId));
        query.fields().include("messages");
        
        return mongoTemplate.findOne(query, ConversationEntity.class)
            .flatMapMany(conv -> Flux.fromIterable(conv.getMessages()))
            .filter(msg -> msg.getContent().toLowerCase()
                              .contains(searchQuery.toLowerCase()))
            .take(limit);
    }
    
    @Override
    public Flux<MessageEntity> getLastNMessages(String conversationId, int count) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(conversationId));
        query.fields().include("messages");
        
        return mongoTemplate.findOne(query, ConversationEntity.class)
            .flatMapMany(conv -> {
                List<MessageEntity> messages = conv.getMessages();
                int start = Math.max(0, messages.size() - count);
                return Flux.fromIterable(messages.subList(start, messages.size()));
            });
    }
    
    @Override
    public Mono<ConversationEntity> addMessage(
            String conversationId, 
            MessageEntity message) {
        
        Query query = new Query(Criteria.where("id").is(conversationId));
        
        Update update = new Update()
            .push("messages", message)
            .set("lastMessageAt", message.getTimestamp())
            .inc("context.messageCount", 1);
        
        return mongoTemplate.findAndModify(
            query, 
            update, 
            ConversationEntity.class
        );
    }
    
    @Override
    public Mono<Boolean> updateContext(String conversationId, ContextEntity context) {
        Query query = new Query(Criteria.where("id").is(conversationId));
        
        Update update = new Update().set("context", context);
        
        return mongoTemplate.updateFirst(query, update, ConversationEntity.class)
            .map(result -> result.getModifiedCount() > 0);
    }
}

// ============================================================================
// CONFIGURATION MONGODB
// ============================================================================

/**
 * Configuration MongoDB pour le module conversation
 */
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.nexusai.conversation.persistence")
@EnableReactiveMongoAuditing
public class MongoDBConfig {
    
    /**
     * Bean pour la configuration des index MongoDB
     * 
     * Les index sont créés automatiquement au démarrage via les annotations
     * @Indexed et @CompoundIndexes sur les entités
     */
}

// ============================================================================
// MAPPERS
// ============================================================================

/**
 * Mapper entre entités de persistence et DTOs
 * 
 * Utilise MapStruct pour la génération automatique du code de mapping
 */
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
    
    ConversationMapper INSTANCE = Mappers.getMapper(ConversationMapper.class);
    
    /**
     * Convertit une entité en DTO
     */
    ConversationDTO entityToDto(ConversationEntity entity);
    
    /**
     * Convertit un DTO en entité
     */
    ConversationEntity dtoToEntity(ConversationDTO dto);
    
    /**
     * Convertit une liste d'entités en liste de DTOs
     */
    List<ConversationDTO> entitiesToDtos(List<ConversationEntity> entities);
    
    /**
     * Convertit un message entité en DTO
     */
    MessageDTO messageEntityToDto(MessageEntity entity);
    
    /**
     * Convertit un message DTO en entité
     */
    MessageEntity messageDtoToEntity(MessageDTO dto);
}
