package com.nexusai.conversation.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.util.*;

/**
 * MODULE: conversation-core
 * 
 * Contient toute la logique métier pour la gestion des conversations
 * 
 * DÉVELOPPEUR ASSIGNÉ: Développeur 2
 * 
 * TÂCHES:
 * - Implémenter les services métier
 * - Orchestrer les appels entre modules
 * - Gérer les règles business
 * - Valider les données
 * - Émettre les événements Kafka
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */

// ============================================================================
// SERVICE PRINCIPAL DE CONVERSATION
// ============================================================================

/**
 * Service principal gérant l'ensemble du cycle de vie des conversations
 * 
 * Responsabilités:
 * - Créer et supprimer des conversations
 * - Orchestrer l'envoi et la réception de messages
 * - Gérer le contexte conversationnel
 * - Coordonner avec les autres modules (LLM, Memory, etc.)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final ConversationCustomRepository customRepository;
    private final LLMService llmService;
    private final MemoryService memoryService;
    private final ContextService contextService;
    private final ModerationService moderationService;
    private final EventPublisher eventPublisher;
    private final ConversationMapper mapper;
    
    /**
     * Crée une nouvelle conversation
     * 
     * @param request Données de création
     * @return Mono contenant la conversation créée
     */
    @Transactional
    public Mono<ConversationDTO> createConversation(CreateConversationRequest request) {
        log.info("Création d'une nouvelle conversation pour userId={}, companionId={}", 
                 request.getUserId(), request.getCompanionId());
        
        return Mono.just(request)
            // 1. Valider les quotas utilisateur
            .flatMap(req -> validateUserQuota(req.getUserId()))
            
            // 2. Créer l'entité conversation
            .map(req -> ConversationEntity.builder()
                .userId(request.getUserId())
                .companionId(request.getCompanionId())
                .title(request.getTitle())
                .messages(new ArrayList<>())
                .context(createInitialContext())
                .isEphemeral(request.getIsEphemeral())
                .expiresAt(request.getExpiresAt())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .build())
            
            // 3. Sauvegarder en base
            .flatMap(conversationRepository::save)
            
            // 4. Initialiser le contexte en mémoire
            .flatMap(entity -> contextService.initializeContext(entity.getId())
                .thenReturn(entity))
            
            // 5. Émettre événement
            .doOnSuccess(entity -> eventPublisher.publishConversationCreated(entity.getId()))
            
            // 6. Convertir en DTO
            .map(mapper::entityToDto)
            
            .doOnSuccess(dto -> log.info("Conversation créée avec succès: {}", dto.getId()))
            .doOnError(error -> log.error("Erreur lors de la création de la conversation", error));
    }
    
    /**
     * Envoie un message dans une conversation
     * 
     * Ce processus inclut:
     * - Modération du contenu
     * - Sauvegarde du message utilisateur
     * - Génération de la réponse IA
     * - Mise à jour du contexte
     * - Mise à jour de la mémoire
     * 
     * @param request Données du message
     * @return Mono contenant le message de réponse du compagnon
     */
    @Transactional
    public Mono<MessageDTO> sendMessage(SendMessageRequest request) {
        log.info("Envoi d'un message dans conversation={}", request.getConversationId());
        
        return Mono.just(request)
            // 1. Vérifier l'existence de la conversation
            .flatMap(req -> conversationRepository.findById(req.getConversationId())
                .switchIfEmpty(Mono.error(
                    new ConversationNotFoundException(req.getConversationId()))))
            
            // 2. Modération du contenu
            .flatMap(conv -> moderationService.moderateMessage(request.getContent())
                .thenReturn(conv))
            
            // 3. Créer le message utilisateur
            .map(conv -> MessageEntity.builder()
                .id(UUID.randomUUID().toString())
                .sender("USER")
                .content(request.getContent())
                .type(request.getType().toString())
                .metadata(request.getMetadata())
                .timestamp(Instant.now())
                .reactions(new ArrayList<>())
                .build())
            
            // 4. Sauvegarder le message utilisateur
            .flatMap(userMsg -> customRepository.addMessage(
                request.getConversationId(), userMsg)
                .thenReturn(userMsg))
            
            // 5. Récupérer le contexte
            .flatMap(userMsg -> contextService.getContext(request.getConversationId())
                .map(context -> Map.entry(userMsg, context)))
            
            // 6. Générer la réponse du compagnon
            .flatMap(entry -> generateCompanionResponse(
                request.getConversationId(),
                entry.getKey(),
                entry.getValue()))
            
            // 7. Sauvegarder le message compagnon
            .flatMap(companionMsg -> customRepository.addMessage(
                request.getConversationId(), companionMsg)
                .thenReturn(companionMsg))
            
            // 8. Mettre à jour le contexte
            .flatMap(companionMsg -> contextService.updateContext(
                request.getConversationId(), 
                companionMsg)
                .thenReturn(companionMsg))
            
            // 9. Émettre événement
            .doOnSuccess(msg -> eventPublisher.publishMessageSent(
                request.getConversationId(), msg.getId()))
            
            // 10. Convertir en DTO
            .map(mapper::messageEntityToDto)
            
            .doOnSuccess(dto -> log.info("Message envoyé avec succès: {}", dto.getId()))
            .doOnError(error -> log.error("Erreur lors de l'envoi du message", error));
    }
    
    /**
     * Récupère une conversation complète par son ID
     * 
     * @param conversationId ID de la conversation
     * @return Mono contenant la conversation
     */
    public Mono<ConversationDTO> getConversation(String conversationId) {
        log.debug("Récupération de la conversation: {}", conversationId);
        
        return conversationRepository.findById(conversationId)
            .switchIfEmpty(Mono.error(
                new ConversationNotFoundException(conversationId)))
            .map(mapper::entityToDto);
    }
    
    /**
     * Récupère toutes les conversations d'un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @return Flux de conversations
     */
    public Flux<ConversationDTO> getUserConversations(String userId) {
        log.debug("Récupération des conversations de l'utilisateur: {}", userId);
        
        return conversationRepository.findByUserIdOrderByLastMessageAtDesc(userId)
            .map(mapper::entityToDto);
    }
    
    /**
     * Supprime une conversation
     * 
     * @param conversationId ID de la conversation
     * @return Mono<Void>
     */
    @Transactional
    public Mono<Void> deleteConversation(String conversationId) {
        log.info("Suppression de la conversation: {}", conversationId);
        
        return conversationRepository.findById(conversationId)
            .switchIfEmpty(Mono.error(
                new ConversationNotFoundException(conversationId)))
            
            // Nettoyer la mémoire associée
            .flatMap(conv -> memoryService.clearMemory(conversationId)
                .thenReturn(conv))
            
            // Nettoyer le contexte en cache
            .flatMap(conv -> contextService.clearContext(conversationId)
                .thenReturn(conv))
            
            // Supprimer la conversation
            .flatMap(conv -> conversationRepository.deleteById(conversationId))
            
            // Émettre événement
            .doOnSuccess(v -> eventPublisher.publishConversationDeleted(conversationId))
            
            .doOnSuccess(v -> log.info("Conversation supprimée: {}", conversationId))
            .doOnError(error -> log.error("Erreur lors de la suppression", error));
    }
    
    /**
     * Recherche dans l'historique d'une conversation
     * 
     * @param request Paramètres de recherche
     * @return Flux de messages correspondants
     */
    public Flux<MessageDTO> searchConversation(SearchConversationRequest request) {
        log.debug("Recherche dans conversation={}, query={}", 
                  request.getConversationId(), request.getQuery());
        
        return customRepository.searchMessagesInConversation(
                request.getConversationId(),
                request.getQuery(),
                request.getLimit())
            .map(mapper::messageEntityToDto)
            .skip(request.getOffset());
    }
    
    /**
     * Exporte une conversation en JSON
     * 
     * @param conversationId ID de la conversation
     * @return Mono contenant le JSON
     */
    public Mono<String> exportConversation(String conversationId) {
        log.info("Export de la conversation: {}", conversationId);
        
        return getConversation(conversationId)
            .map(this::convertToJson);
    }
    
    // ========================================================================
    // MÉTHODES PRIVÉES
    // ========================================================================
    
    /**
     * Valide que l'utilisateur n'a pas dépassé son quota de conversations
     */
    private Mono<CreateConversationRequest> validateUserQuota(String userId) {
        return conversationRepository.countByUserId(userId)
            .flatMap(count -> {
                // Récupérer le quota de l'utilisateur (selon son abonnement)
                int maxConversations = getMaxConversationsForUser(userId);
                
                if (count >= maxConversations) {
                    return Mono.error(new QuotaExceededException(
                        "Nombre maximum de conversations atteint: " + maxConversations));
                }
                
                return Mono.just(createRequestFromUserId(userId));
            });
    }
    
    /**
     * Crée le contexte initial d'une conversation
     */
    private ContextEntity createInitialContext() {
        return ContextEntity.builder()
            .topics(new ArrayList<>())
            .emotionalTone("NEUTRAL")
            .lastSummary("")
            .messageCount(0)
            .durationMinutes(0L)
            .companionEmotionalState("NEUTRAL")
            .keyMemories(new ArrayList<>())
            .build();
    }
    
    /**
     * Génère la réponse du compagnon via le LLM
     */
    private Mono<MessageEntity> generateCompanionResponse(
            String conversationId,
            MessageEntity userMessage,
            ConversationContext context) {
        
        return llmService.generateResponse(conversationId, userMessage, context)
            .map(response -> MessageEntity.builder()
                .id(UUID.randomUUID().toString())
                .sender("COMPANION")
                .content(response.getContent())
                .type("TEXT")
                .metadata(response.getMetadata())
                .timestamp(Instant.now())
                .reactions(new ArrayList<>())
                .detectedEmotion(response.getDetectedEmotion())
                .emotionConfidence(response.getEmotionConfidence())
                .build());
    }
    
    /**
     * Convertit une conversation en JSON pour l'export
     */
    private String convertToJson(ConversationDTO conversation) {
        // Utiliser Jackson ObjectMapper ou similaire
        return "{}"; // Implémentation simplifiée
    }
    
    /**
     * Récupère le quota maximum selon l'abonnement de l'utilisateur
     */
    private int getMaxConversationsForUser(String userId) {
        // Appeler le module User/Payment pour récupérer le plan
        // FREE: 5, STANDARD: 20, PREMIUM: 50, VIP+: illimité
        return 20; // Valeur par défaut
    }
    
    private CreateConversationRequest createRequestFromUserId(String userId) {
        // Helper method
        return null;
    }
}

// ============================================================================
// SERVICE DE CONTEXTE
// ============================================================================

/**
 * Service gérant le contexte conversationnel
 * 
 * Responsabilités:
 * - Maintenir le contexte court terme en mémoire (Redis)
 * - Générer des résumés périodiques
 * - Extraire les sujets principaux
 * - Suivre l'état émotionnel
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContextService {
    
    private final RedisTemplate<String, ConversationContext> redisTemplate;
    private final LLMService llmService;
    
    /**
     * Initialise le contexte d'une nouvelle conversation
     */
    public Mono<Void> initializeContext(String conversationId) {
        log.debug("Initialisation du contexte pour conversation: {}", conversationId);
        
        ConversationContext context = ConversationContext.builder()
            .topics(new ArrayList<>())
            .emotionalTone("NEUTRAL")
            .messageCount(0)
            .durationMinutes(0L)
            .companionEmotionalState("NEUTRAL")
            .keyMemories(new ArrayList<>())
            .build();
        
        return Mono.fromRunnable(() -> 
            redisTemplate.opsForValue().set(
                getContextKey(conversationId), 
                context,
                Duration.ofHours(24)
            )
        );
    }
    
    /**
     * Récupère le contexte actuel d'une conversation
     */
    public Mono<ConversationContext> getContext(String conversationId) {
        return Mono.fromCallable(() -> 
            redisTemplate.opsForValue().get(getContextKey(conversationId))
        ).switchIfEmpty(Mono.defer(() -> initializeAndGetContext(conversationId)));
    }
    
    /**
     * Met à jour le contexte après un nouveau message
     */
    public Mono<Void> updateContext(String conversationId, MessageEntity message) {
        return getContext(conversationId)
            .flatMap(context -> {
                // Incrémenter le compteur de messages
                context.setMessageCount(context.getMessageCount() + 1);
                
                // Extraire et ajouter les nouveaux sujets
                List<String> newTopics = extractTopics(message.getContent());
                newTopics.forEach(topic -> {
                    if (!context.getTopics().contains(topic)) {
                        context.getTopics().add(topic);
                    }
                });
                
                // Mettre à jour le ton émotionnel
                if (message.getDetectedEmotion() != null) {
                    context.setEmotionalTone(message.getDetectedEmotion());
                }
                
                // Générer un résumé tous les 20 messages
                if (context.getMessageCount() % 20 == 0) {
                    return generateAndUpdateSummary(conversationId, context);
                }
                
                return saveContext(conversationId, context);
            });
    }
    
    /**
     * Supprime le contexte d'une conversation
     */
    public Mono<Void> clearContext(String conversationId) {
        return Mono.fromRunnable(() -> 
            redisTemplate.delete(getContextKey(conversationId))
        );
    }
    
    // Méthodes privées
    
    private String getContextKey(String conversationId) {
        return "conversation:context:" + conversationId;
    }
    
    private Mono<ConversationContext> initializeAndGetContext(String conversationId) {
        return initializeContext(conversationId)
            .then(getContext(conversationId));
    }
    
    private List<String> extractTopics(String content) {
        // Extraction simple de topics (mots-clés)
        // Dans une vraie implémentation, utiliser NLP
        return Arrays.asList(content.split(" "))
            .stream()
            .filter(word -> word.length() > 5)
            .limit(3)
            .toList();
    }
    
    private Mono<Void> generateAndUpdateSummary(
            String conversationId, 
            ConversationContext context) {
        
        return llmService.generateSummary(conversationId)
            .flatMap(summary -> {
                context.setLastSummary(summary);
                return saveContext(conversationId, context);
            });
    }
    
    private Mono<Void> saveContext(String conversationId, ConversationContext context) {
        return Mono.fromRunnable(() -> 
            redisTemplate.opsForValue().set(
                getContextKey(conversationId),
                context,
                Duration.ofHours(24)
            )
        );
    }
}

// ============================================================================
// SERVICE D'ÉVÉNEMENTS
// ============================================================================

/**
 * Service de publication d'événements Kafka
 * 
 * Émet des événements pour notifier les autres modules des changements
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * Publie un événement de création de conversation
     */
    public void publishConversationCreated(String conversationId) {
        ConversationEvent event = ConversationEvent.builder()
            .eventType("CONVERSATION_CREATED")
            .conversationId(conversationId)
            .timestamp(Instant.now())
            .build();
        
        kafkaTemplate.send(
            ConversationConstants.TOPIC_CONVERSATION_CREATED, 
            conversationId, 
            event
        );
        
        log.info("Événement CONVERSATION_CREATED publié: {}", conversationId);
    }
    
    /**
     * Publie un événement d'envoi de message
     */
    public void publishMessageSent(String conversationId, String messageId) {
        MessageEvent event = MessageEvent.builder()
            .eventType("MESSAGE_SENT")
            .conversationId(conversationId)
            .messageId(messageId)
            .timestamp(Instant.now())
            .build();
        
        kafkaTemplate.send(
            ConversationConstants.TOPIC_MESSAGE_SENT,
            conversationId,
            event
        );
        
        log.debug("Événement MESSAGE_SENT publié: {}", messageId);
    }
    
    /**
     * Publie un événement de suppression de conversation
     */
    public void publishConversationDeleted(String conversationId) {
        ConversationEvent event = ConversationEvent.builder()
            .eventType("CONVERSATION_DELETED")
            .conversationId(conversationId)
            .timestamp(Instant.now())
            .build();
        
        kafkaTemplate.send(
            ConversationConstants.TOPIC_CONVERSATION_DELETED,
            conversationId,
            event
        );
        
        log.info("Événement CONVERSATION_DELETED publié: {}", conversationId);
    }
}
