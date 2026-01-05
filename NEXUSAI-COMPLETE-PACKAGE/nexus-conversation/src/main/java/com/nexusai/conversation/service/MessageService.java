package com.nexusai.conversation.service;

import com.nexusai.ai.dto.AIRequest;
import com.nexusai.ai.dto.AIResponse;
import com.nexusai.ai.service.AIService;
import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.commons.exception.ValidationException;
import com.nexusai.conversation.dto.MessageDTO;
import com.nexusai.conversation.dto.SendMessageRequest;
import com.nexusai.core.entity.Companion;
import com.nexusai.core.entity.Conversation;
import com.nexusai.core.entity.Message;
import com.nexusai.core.enums.MessageRole;
import com.nexusai.core.enums.MessageType;
import com.nexusai.core.repository.CompanionRepository;
import com.nexusai.core.repository.ConversationRepository;
import com.nexusai.core.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des messages avec intégration IA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final CompanionRepository companionRepository;
    private final AIService aiService;
    private final ContextService contextService;

    private static final int CONTEXT_HISTORY_SIZE = 10;

    /**
     * Envoie un message et génère la réponse IA.
     */
    @Transactional
    public MessageDTO sendMessage(UUID conversationId, UUID userId, SendMessageRequest request) {
        log.info("Sending message to conversation: {}", conversationId);

        // Validation
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ValidationException("EMPTY_MESSAGE", "Message content cannot be empty");
        }

        // Sauvegarder le message de l'utilisateur
        Message userMessage = saveUserMessage(conversation, request);

        // Récupérer le companion
        Companion companion = companionRepository.findById(conversation.getCompanionId())
                .orElseThrow(() -> new ResourceNotFoundException("Companion", conversation.getCompanionId().toString()));

        // Générer la réponse IA
        AIResponse aiResponse = generateAIResponse(conversation, companion, request.getContent());

        // Sauvegarder la réponse IA
        Message aiMessage = saveAIMessage(conversation, aiResponse);

        // Mettre à jour la conversation
        updateConversation(conversation, userMessage.getTokensUsed() + aiResponse.getTotalTokens());

        return mapToDTO(aiMessage);
    }

    /**
     * Envoie un message et stream la réponse IA.
     */
    public Flux<String> streamMessage(UUID conversationId, UUID userId, SendMessageRequest request) {
        log.info("Streaming message to conversation: {}", conversationId);

        // Validation
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        // Sauvegarder le message utilisateur
        Message userMessage = saveUserMessage(conversation, request);

        // Récupérer le companion
        Companion companion = companionRepository.findById(conversation.getCompanionId())
                .orElseThrow(() -> new ResourceNotFoundException("Companion", conversation.getCompanionId().toString()));

        // Stream la réponse
        StringBuilder fullResponse = new StringBuilder();
        
        return streamAIResponse(conversation, companion, request.getContent())
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // Sauvegarder le message complet à la fin
                    saveAIMessageFromStream(conversation, fullResponse.toString());
                    log.info("AI streaming completed, message saved");
                })
                .doOnError(e -> log.error("Error in AI streaming", e));
    }

    /**
     * Édite un message existant.
     */
    @Transactional
    public MessageDTO editMessage(UUID messageId, UUID userId, String newContent) {
        log.info("Editing message: {}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId.toString()));

        // Vérifier que l'utilisateur est le propriétaire de la conversation
        conversationRepository.findByIdAndUserId(message.getConversationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", message.getConversationId().toString()));

        if (!message.isUserMessage()) {
            throw new ValidationException("CANNOT_EDIT_AI_MESSAGE", "Cannot edit AI messages");
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        message = messageRepository.save(message);

        return mapToDTO(message);
    }

    /**
     * Supprime un message.
     */
    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        log.info("Deleting message: {}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId.toString()));

        conversationRepository.findByIdAndUserId(message.getConversationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", message.getConversationId().toString()));

        messageRepository.delete(message);
        log.info("Message {} deleted by user", messageId);
    }

    /**
     * Sauvegarde le message de l'utilisateur.
     */
    private Message saveUserMessage(Conversation conversation, SendMessageRequest request) {
        MessageType type = parseMessageType(request.getType());
        
        Message message = Message.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.USER)
                .type(type)
                .content(request.getContent())
                .tokensUsed(aiService.countTokens(request.getContent(), "openai"))
                .build();

        message = messageRepository.save(message);
        log.info("User message saved: {}", message.getId());
        
        return message;
    }

    /**
     * Génère la réponse IA.
     */
    private AIResponse generateAIResponse(Conversation conversation, Companion companion, String userMessage) {
        log.debug("Generating AI response for conversation: {}", conversation.getId());

        // Récupérer l'historique
        List<Message> history = messageRepository.findRecentMessages(
                conversation.getId(), 
                CONTEXT_HISTORY_SIZE
        );

        // Construire le contexte
        String systemPrompt = buildSystemPrompt(companion);
        List<AIRequest.ConversationMessage> conversationHistory = history.stream()
                .map(msg -> AIRequest.ConversationMessage.builder()
                        .role(msg.getRole() == MessageRole.USER ? "user" : "assistant")
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());

        // Créer la requête IA
        AIRequest aiRequest = AIRequest.builder()
                .message(userMessage)
                .systemPrompt(systemPrompt)
                .conversationHistory(conversationHistory)
                .temperature(0.7)
                .maxTokens(1000)
                .build();

        // Appeler l'IA
        return aiService.generateResponse(aiRequest);
    }

    /**
     * Stream la réponse IA.
     */
    private Flux<String> streamAIResponse(Conversation conversation, Companion companion, String userMessage) {
        // Récupérer l'historique
        List<Message> history = messageRepository.findRecentMessages(
                conversation.getId(), 
                CONTEXT_HISTORY_SIZE
        );

        // Construire le contexte
        String systemPrompt = buildSystemPrompt(companion);
        List<AIRequest.ConversationMessage> conversationHistory = history.stream()
                .map(msg -> AIRequest.ConversationMessage.builder()
                        .role(msg.getRole() == MessageRole.USER ? "user" : "assistant")
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());

        // Créer la requête IA
        AIRequest aiRequest = AIRequest.builder()
                .message(userMessage)
                .systemPrompt(systemPrompt)
                .conversationHistory(conversationHistory)
                .temperature(0.7)
                .maxTokens(1000)
                .stream(true)
                .build();

        // Stream l'IA
        return aiService.generateStreamResponse(aiRequest);
    }

    /**
     * Construit le prompt système basé sur la personnalité du companion.
     */
    private String buildSystemPrompt(Companion companion) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are ").append(companion.getName());
        
        if (companion.getDescription() != null && !companion.getDescription().isEmpty()) {
            prompt.append(". ").append(companion.getDescription());
        }
        
        if (companion.getPersonality() != null && !companion.getPersonality().isEmpty()) {
            prompt.append("\n\nPersonality traits: ").append(companion.getPersonality());
        }
        
        prompt.append("\n\nRespond naturally and stay in character.");
        
        return prompt.toString();
    }

    /**
     * Sauvegarde le message IA.
     */
    private Message saveAIMessage(Conversation conversation, AIResponse aiResponse) {
        Message message = Message.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.ASSISTANT)
                .type(MessageType.TEXT)
                .content(aiResponse.getContent())
                .tokensUsed(aiResponse.getTotalTokens())
                .metadata(String.format("{\"provider\":\"%s\",\"model\":\"%s\"}", 
                        aiResponse.getProvider(), aiResponse.getModel()))
                .build();

        message = messageRepository.save(message);
        log.info("AI message saved: {} ({} tokens)", message.getId(), message.getTokensUsed());
        
        return message;
    }

    /**
     * Sauvegarde le message IA depuis le streaming.
     */
    @Transactional
    public void saveAIMessageFromStream(Conversation conversation, String content) {
        int tokens = aiService.countTokens(content, "openai");
        
        Message message = Message.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.ASSISTANT)
                .type(MessageType.TEXT)
                .content(content)
                .tokensUsed(tokens)
                .build();

        messageRepository.save(message);
        updateConversation(conversation, tokens);
    }

    /**
     * Met à jour la conversation après un message.
     */
    private void updateConversation(Conversation conversation, int tokensUsed) {
        conversation.incrementMessages();
        conversation.addTokens(tokensUsed);
        conversationRepository.save(conversation);
    }

    /**
     * Parse le type de message.
     */
    private MessageType parseMessageType(String type) {
        if (type == null) return MessageType.TEXT;
        try {
            return MessageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MessageType.TEXT;
        }
    }

    /**
     * Mappe Message vers DTO.
     */
    private MessageDTO mapToDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .role(message.getRole().name())
                .type(message.getType().name())
                .content(message.getContent())
                .tokensUsed(message.getTokensUsed())
                .mediaUrl(message.getMediaUrl())
                .mediaType(message.getMediaType())
                .isEdited(message.getIsEdited())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
