package com.nexusai.conversation.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * MODULE: conversation-api
 * 
 * Expose les APIs REST et WebSocket pour les conversations
 * 
 * DÉVELOPPEUR ASSIGNÉ: Développeur 1
 * 
 * TÂCHES:
 * - Créer les controllers REST
 * - Implémenter les WebSocket handlers
 * - Gérer la validation des DTOs
 * - Documenter les APIs (Swagger)
 * - Gérer les erreurs HTTP
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */

// ============================================================================
// CONTROLLER REST PRINCIPAL
// ============================================================================

/**
 * Controller REST pour la gestion des conversations
 * 
 * Endpoints:
 * - POST   /api/v1/conversations          Créer une conversation
 * - GET    /api/v1/conversations/{id}     Récupérer une conversation
 * - DELETE /api/v1/conversations/{id}     Supprimer une conversation
 * - GET    /api/v1/conversations/user/{userId}  Lister les conversations
 */
@RestController
@RequestMapping("/api/v1/conversations")
@Slf4j
@RequiredArgsConstructor
public class ConversationController {
    
    private final ConversationService conversationService;
    
    /**
     * Crée une nouvelle conversation
     * 
     * POST /api/v1/conversations
     * 
     * Request body:
     * {
     *   "userId": "uuid",
     *   "companionId": "uuid",
     *   "title": "Ma conversation",
     *   "isEphemeral": false,
     *   "tags": ["work", "casual"]
     * }
     * 
     * Response 201:
     * {
     *   "id": "uuid",
     *   "userId": "uuid",
     *   "companionId": "uuid",
     *   "title": "Ma conversation",
     *   "messages": [],
     *   "createdAt": "2025-01-15T10:00:00Z"
     * }
     */
    @PostMapping
    public Mono<ResponseEntity<ConversationDTO>> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        
        log.info("API: Création de conversation pour userId={}", request.getUserId());
        
        return conversationService.createConversation(request)
            .map(conv -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(conv))
            .onErrorResume(error -> handleError(error, "création de conversation"));
    }
    
    /**
     * Récupère une conversation par son ID
     * 
     * GET /api/v1/conversations/{id}
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ConversationDTO>> getConversation(
            @PathVariable String id) {
        
        log.debug("API: Récupération de conversation id={}", id);
        
        return conversationService.getConversation(id)
            .map(ResponseEntity::ok)
            .onErrorResume(ConversationNotFoundException.class, 
                error -> Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(error -> handleError(error, "récupération de conversation"));
    }
    
    /**
     * Supprime une conversation
     * 
     * DELETE /api/v1/conversations/{id}
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteConversation(
            @PathVariable String id) {
        
        log.info("API: Suppression de conversation id={}", id);
        
        return conversationService.deleteConversation(id)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .onErrorResume(ConversationNotFoundException.class,
                error -> Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(error -> handleError(error, "suppression de conversation"));
    }
    
    /**
     * Liste toutes les conversations d'un utilisateur
     * 
     * GET /api/v1/conversations/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<List<ConversationDTO>>> getUserConversations(
            @PathVariable String userId) {
        
        log.debug("API: Liste des conversations pour userId={}", userId);
        
        return conversationService.getUserConversations(userId)
            .collectList()
            .map(ResponseEntity::ok)
            .onErrorResume(error -> handleError(error, "liste des conversations"));
    }
    
    /**
     * Exporte une conversation en JSON
     * 
     * GET /api/v1/conversations/{id}/export
     */
    @GetMapping("/{id}/export")
    public Mono<ResponseEntity<String>> exportConversation(
            @PathVariable String id) {
        
        log.info("API: Export de conversation id={}", id);
        
        return conversationService.exportConversation(id)
            .map(json -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Disposition", 
                       "attachment; filename=conversation-" + id + ".json")
                .body(json))
            .onErrorResume(error -> handleError(error, "export de conversation"));
    }
    
    // Gestion d'erreurs
    private <T> Mono<ResponseEntity<T>> handleError(Throwable error, String context) {
        log.error("Erreur lors de {}: {}", context, error.getMessage(), error);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}

// ============================================================================
// CONTROLLER POUR LES MESSAGES
// ============================================================================

/**
 * Controller REST pour la gestion des messages
 * 
 * Endpoints:
 * - POST /api/v1/conversations/{id}/messages     Envoyer un message
 * - GET  /api/v1/conversations/{id}/messages     Récupérer les messages
 * - POST /api/v1/conversations/{id}/search       Rechercher dans l'historique
 */
@RestController
@RequestMapping("/api/v1/conversations")
@Slf4j
@RequiredArgsConstructor
public class MessageController {
    
    private final ConversationService conversationService;
    
    /**
     * Envoie un message dans une conversation
     * 
     * POST /api/v1/conversations/{id}/messages
     * 
     * Request body:
     * {
     *   "content": "Bonjour! Comment vas-tu?",
     *   "type": "TEXT",
     *   "metadata": {}
     * }
     * 
     * Response 200:
     * {
     *   "id": "uuid",
     *   "sender": "COMPANION",
     *   "content": "Bonjour! Je vais bien, merci!",
     *   "timestamp": "2025-01-15T10:01:00Z",
     *   "detectedEmotion": "JOY"
     * }
     */
    @PostMapping("/{id}/messages")
    public Mono<ResponseEntity<MessageDTO>> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request) {
        
        log.info("API: Envoi de message dans conversation id={}", id);
        
        // Ajouter l'ID de conversation à la requête
        request.setConversationId(id);
        
        return conversationService.sendMessage(request)
            .map(ResponseEntity::ok)
            .onErrorResume(ConversationNotFoundException.class,
                error -> Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume(InvalidMessageException.class,
                error -> Mono.just(ResponseEntity.badRequest().build()))
            .onErrorResume(error -> {
                log.error("Erreur envoi message", error);
                return Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build());
            });
    }
    
    /**
     * Récupère les messages d'une conversation
     * 
     * GET /api/v1/conversations/{id}/messages?limit=50&offset=0
     */
    @GetMapping("/{id}/messages")
    public Mono<ResponseEntity<List<MessageDTO>>> getMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        log.debug("API: Récupération messages conversation id={}", id);
        
        return conversationService.getConversation(id)
            .map(conv -> conv.getMessages().stream()
                .skip(offset)
                .limit(limit)
                .toList())
            .map(ResponseEntity::ok)
            .onErrorResume(ConversationNotFoundException.class,
                error -> Mono.just(ResponseEntity.notFound().build()));
    }
    
    /**
     * Recherche dans l'historique d'une conversation
     * 
     * POST /api/v1/conversations/{id}/search
     * 
     * Request body:
     * {
     *   "query": "restaurant",
     *   "limit": 20,
     *   "offset": 0
     * }
     */
    @PostMapping("/{id}/search")
    public Mono<ResponseEntity<List<MessageDTO>>> searchMessages(
            @PathVariable String id,
            @Valid @RequestBody SearchConversationRequest request) {
        
        log.info("API: Recherche dans conversation id={}, query={}", 
                 id, request.getQuery());
        
        request.setConversationId(id);
        
        return conversationService.searchConversation(request)
            .collectList()
            .map(ResponseEntity::ok)
            .onErrorResume(error -> {
                log.error("Erreur recherche", error);
                return Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build());
            });
    }
}

// ============================================================================
// WEBSOCKET HANDLER
// ============================================================================

/**
 * Handler WebSocket pour le chat en temps réel
 * 
 * Endpoint: ws://localhost:8080/ws/conversations/{conversationId}
 * 
 * Messages entrants (client -> serveur):
 * {
 *   "type": "MESSAGE",
 *   "content": "Bonjour!",
 *   "metadata": {}
 * }
 * 
 * Messages sortants (serveur -> client):
 * {
 *   "type": "MESSAGE",
 *   "sender": "COMPANION",
 *   "content": "Bonjour! Comment vas-tu?",
 *   "timestamp": "2025-01-15T10:00:00Z",
 *   "emotion": "JOY"
 * }
 * 
 * {
 *   "type": "TYPING",
 *   "isTyping": true
 * }
 * 
 * {
 *   "type": "ERROR",
 *   "message": "Erreur lors du traitement"
 * }
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConversationWebSocketHandler extends TextWebSocketHandler {
    
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    
    // Map session -> conversationId
    private final Map<String, String> sessionConversationMap = 
        new ConcurrentHashMap<>();
    
    // Map conversationId -> sessions
    private final Map<String, Set<WebSocketSession>> conversationSessions = 
        new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String conversationId = extractConversationId(session);
        
        log.info("WebSocket connecté: session={}, conversation={}", 
                 session.getId(), conversationId);
        
        sessionConversationMap.put(session.getId(), conversationId);
        conversationSessions
            .computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet())
            .add(session);
        
        // Envoyer message de confirmation
        sendToSession(session, WebSocketMessage.builder()
            .type("CONNECTED")
            .data(Map.of("conversationId", conversationId))
            .timestamp(Instant.now())
            .build());
    }
    
    @Override
    protected void handleTextMessage(
            WebSocketSession session, 
            TextMessage message) {
        
        try {
            String conversationId = sessionConversationMap.get(session.getId());
            
            if (conversationId == null) {
                sendError(session, "Session non initialisée");
                return;
            }
            
            // Parser le message
            WebSocketIncomingMessage incoming = 
                objectMapper.readValue(message.getPayload(), 
                                     WebSocketIncomingMessage.class);
            
            log.debug("Message WebSocket reçu: type={}, conversation={}", 
                     incoming.getType(), conversationId);
            
            switch (incoming.getType()) {
                case "MESSAGE":
                    handleUserMessage(session, conversationId, incoming);
                    break;
                    
                case "TYPING":
                    handleTypingIndicator(conversationId, incoming);
                    break;
                    
                default:
                    sendError(session, "Type de message inconnu: " + incoming.getType());
            }
            
        } catch (Exception e) {
            log.error("Erreur traitement message WebSocket", e);
            sendError(session, "Erreur: " + e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(
            WebSocketSession session, 
            CloseStatus status) {
        
        String conversationId = sessionConversationMap.remove(session.getId());
        
        if (conversationId != null) {
            Set<WebSocketSession> sessions = conversationSessions.get(conversationId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    conversationSessions.remove(conversationId);
                }
            }
        }
        
        log.info("WebSocket déconnecté: session={}, conversation={}", 
                 session.getId(), conversationId);
    }
    
    // ========================================================================
    // MÉTHODES PRIVÉES
    // ========================================================================
    
    private void handleUserMessage(
            WebSocketSession session,
            String conversationId,
            WebSocketIncomingMessage incoming) {
        
        // Envoyer indicateur "en train d'écrire"
        broadcastToConversation(conversationId, WebSocketMessage.builder()
            .type("TYPING")
            .data(Map.of("sender", "COMPANION", "isTyping", true))
            .timestamp(Instant.now())
            .build());
        
        // Créer la requête
        SendMessageRequest request = SendMessageRequest.builder()
            .conversationId(conversationId)
            .content(incoming.getContent())
            .type(MessageType.TEXT)
            .metadata(incoming.getMetadata())
            .build();
        
        // Traiter le message de manière asynchrone
        conversationService.sendMessage(request)
            .subscribe(
                companionMessage -> {
                    // Arrêter l'indicateur "en train d'écrire"
                    broadcastToConversation(conversationId, WebSocketMessage.builder()
                        .type("TYPING")
                        .data(Map.of("sender", "COMPANION", "isTyping", false))
                        .timestamp(Instant.now())
                        .build());
                    
                    // Envoyer la réponse
                    broadcastToConversation(conversationId, WebSocketMessage.builder()
                        .type("MESSAGE")
                        .data(companionMessage)
                        .timestamp(Instant.now())
                        .build());
                },
                error -> {
                    log.error("Erreur génération réponse", error);
                    sendError(session, "Erreur lors de la génération de la réponse");
                }
            );
    }
    
    private void handleTypingIndicator(
            String conversationId,
            WebSocketIncomingMessage incoming) {
        
        // Relayer l'indicateur aux autres sessions
        broadcastToConversation(conversationId, WebSocketMessage.builder()
            .type("TYPING")
            .data(Map.of(
                "sender", "USER",
                "isTyping", incoming.getMetadata().get("isTyping")
            ))
            .timestamp(Instant.now())
            .build());
    }
    
    private void broadcastToConversation(
            String conversationId, 
            WebSocketMessage message) {
        
        Set<WebSocketSession> sessions = conversationSessions.get(conversationId);
        if (sessions != null) {
            sessions.forEach(session -> sendToSession(session, message));
        }
    }
    
    private void sendToSession(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Erreur envoi message WebSocket", e);
        }
    }
    
    private void sendError(WebSocketSession session, String errorMessage) {
        sendToSession(session, WebSocketMessage.builder()
            .type("ERROR")
            .data(Map.of("message", errorMessage))
            .timestamp(Instant.now())
            .build());
    }
    
    private String extractConversationId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}

// ============================================================================
// WEBSOCKET CONFIGURATION
// ============================================================================

/**
 * Configuration WebSocket
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private ConversationWebSocketHandler webSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/conversations/{conversationId}")
            .setAllowedOrigins("*") // À configurer selon l'environnement
            .withSockJS(); // Support fallback
    }
}

// ============================================================================
// DTOs WEBSOCKET
// ============================================================================

@Data
@Builder
class WebSocketMessage {
    private String type;
    private Object data;
    private Instant timestamp;
}

@Data
class WebSocketIncomingMessage {
    private String type;
    private String content;
    private Map<String, Object> metadata;
}

// ============================================================================
// EXCEPTION HANDLER GLOBAL
// ============================================================================

/**
 * Gestionnaire global d'exceptions pour les controllers REST
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFound(
            ConversationNotFoundException ex) {
        
        log.warn("Conversation non trouvée: {}", ex.getConversationId());
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .error("CONVERSATION_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build());
    }
    
    @ExceptionHandler(InvalidMessageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMessage(
            InvalidMessageException ex) {
        
        log.warn("Message invalide: {}", ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .error("INVALID_MESSAGE")
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build());
    }
    
    @ExceptionHandler(MessageQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(
            MessageQuotaExceededException ex) {
        
        log.warn("Quota dépassé pour utilisateur: {}", ex.getUserId());
        
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ErrorResponse.builder()
                .error("QUOTA_EXCEEDED")
                .message(ex.getMessage())
                .data(Map.of(
                    "currentCount", ex.getCurrentCount(),
                    "maxCount", ex.getMaxCount()
                ))
                .timestamp(Instant.now())
                .build());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("Erreur inattendue", ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("Une erreur est survenue")
                .timestamp(Instant.now())
                .build());
    }
}

@Data
@Builder
class ErrorResponse {
    private String error;
    private String message;
    private Map<String, Object> data;
    private Instant timestamp;
}
