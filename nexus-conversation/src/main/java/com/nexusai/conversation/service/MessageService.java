package com.nexusai.conversation.service;

import com.nexusai.ai.service.AIProviderService;
import com.nexusai.commons.exception.BusinessException;
import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.conversation.dto.MessageDTO;
import com.nexusai.conversation.dto.SendMessageRequest;
import com.nexusai.conversation.dto.StreamChunk;
import com.nexusai.core.entity.Companion;
import com.nexusai.core.entity.Conversation;
import com.nexusai.core.entity.Message;
import com.nexusai.core.enums.MessageRole;
import com.nexusai.core.enums.MessageType;
import com.nexusai.core.repository.CompanionRepository;
import com.nexusai.core.repository.ConversationRepository;
import com.nexusai.core.repository.MessageRepository;
import com.nexusai.moderation.service.ContentFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for managing conversation messages with AI integration.
 *
 * Features:
 * - Send messages with content moderation
 * - Generate AI responses
 * - Stream AI responses in real-time (Reactive)
 * - Context management
 * - Token counting and tracking
 *
 * @author NexusAI Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final CompanionRepository companionRepository;
    private final AIProviderService aiProviderService;
    private final ContentFilterService contentFilterService;
    private final ContextService contextService;

    private static final int CONTEXT_MESSAGE_LIMIT = 20;

    /**
     * Send a user message (without generating AI response).
     *
     * @param conversationId Conversation ID
     * @param userId User ID
     * @param request Message request
     * @return Saved message DTO
     */
    @Transactional
    public MessageDTO sendMessage(UUID conversationId, UUID userId, SendMessageRequest request) {
        log.debug("Sending message from user {} to conversation {}", userId, conversationId);

        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        // Content moderation check
        if (!contentFilterService.isContentSafe(request.getContent())) {
            throw new BusinessException("CONTENT_BLOCKED", "Message contains inappropriate content");
        }

        // Save user message
        Message userMessage = Message.builder()
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .type(parseMessageType(request.getType()))
                .content(request.getContent())
                .parentMessageId(request.getParentMessageId())
                .build();

        userMessage = messageRepository.save(userMessage);

        // Update conversation
        conversationRepository.incrementMessageCount(conversationId, LocalDateTime.now());

        log.info("User message saved for conversation {}", conversationId);
        return mapToDTO(userMessage);
    }

    /**
     * Generate AI response for the last user message.
     *
     * @param conversationId Conversation ID
     * @param userId User ID
     * @return AI message DTO
     */
    @Transactional
    public MessageDTO generateResponse(UUID conversationId, UUID userId) {
        log.debug("Generating AI response for conversation {}", conversationId);

        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        Companion companion = companionRepository.findById(conversation.getCompanionId())
                .orElseThrow(() -> new ResourceNotFoundException("Companion", conversation.getCompanionId().toString()));

        // Get context messages
        List<Message> contextMessages = messageRepository.findRecentMessages(conversationId, CONTEXT_MESSAGE_LIMIT);
        String context = contextService.buildContext(conversationId, contextMessages);

        // Generate AI response
        long startTime = System.currentTimeMillis();
        String response = aiProviderService.generateResponse(
                companion.getSystemPrompt(),
                context,
                companion.getModelProvider(),
                companion.getModelName()
        );
        long generationTime = System.currentTimeMillis() - startTime;

        int tokensUsed = aiProviderService.estimateTokens(response);

        // Save AI message
        Message aiMessage = Message.builder()
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .type(MessageType.TEXT)
                .content(response)
                .tokensUsed(tokensUsed)
                .build();

        aiMessage = messageRepository.save(aiMessage);

        // Update conversation stats
        conversation.incrementMessages();
        conversation.addTokens(tokensUsed);
        conversationRepository.save(conversation);

        log.info("AI response generated for conversation {} in {}ms", conversationId, generationTime);
        return mapToDTO(aiMessage);
    }

    /**
     * Stream a message with AI response in real-time.
     * This method saves the user message first, then streams the AI response.
     * Returns Flux<String> for SSE (Server-Sent Events) compatibility.
     *
     * @param conversationId Conversation ID
     * @param userId User ID
     * @param request Message request
     * @return Flux of response text chunks (String)
     */
    public Flux<String> streamMessage(UUID conversationId, UUID userId, SendMessageRequest request) {
        log.info("Starting message stream for conversation: {} by user: {}", conversationId, userId);

        return Mono.fromCallable(() -> {
                    // Validate conversation ownership
                    Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

                    // Content moderation check
                    if (!contentFilterService.isContentSafe(request.getContent())) {
                        throw new BusinessException("CONTENT_BLOCKED", "Message contains inappropriate content");
                    }

                    // Save user message
                    Message userMessage = Message.builder()
                            .conversationId(conversationId)
                            .role(MessageRole.USER)
                            .type(parseMessageType(request.getType()))
                            .content(request.getContent())
                            .parentMessageId(request.getParentMessageId())
                            .build();

                    messageRepository.save(userMessage);
                    conversationRepository.incrementMessageCount(conversationId, LocalDateTime.now());

                    // Get companion and context for AI response
                    Companion companion = companionRepository.findById(conversation.getCompanionId())
                            .orElseThrow(() -> new ResourceNotFoundException("Companion", conversation.getCompanionId().toString()));

                    List<Message> contextMessages = messageRepository.findRecentMessages(conversationId, CONTEXT_MESSAGE_LIMIT);
                    String context = contextService.buildContext(conversationId, contextMessages);

                    return new StreamContext(conversation, companion, context);
                })
                .flatMapMany(ctx -> {
                    // Stream AI response
                    AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());

                    return aiProviderService.streamResponse(
                                    ctx.companion.getSystemPrompt(),
                                    ctx.context,
                                    ctx.companion.getModelProvider(),
                                    ctx.companion.getModelName()
                            )
                            .doOnNext(chunk -> fullResponse.get().append(chunk))
                            .doOnComplete(() -> {
                                // Save complete AI message after streaming completes
                                try {
                                    String completeResponse = fullResponse.get().toString();
                                    int tokens = aiProviderService.estimateTokens(completeResponse);

                                    saveStreamedMessage(conversationId, completeResponse, tokens);

                                    log.info("Streaming completed for conversation {}, total tokens: {}",
                                            conversationId, tokens);
                                } catch (Exception e) {
                                    log.error("Error saving streamed message for conversation {}", conversationId, e);
                                }
                            })
                            .doOnError(e -> log.error("Stream error for conversation: {}", conversationId, e));
                });
    }

    /**
     * Stream AI response for existing conversation (without saving new user message).
     * Returns Flux<StreamChunk> for WebSocket compatibility.
     *
     * @param conversationId Conversation ID
     * @param userId User ID
     * @return Flux of stream chunks
     */
    public Flux<StreamChunk> streamResponse(UUID conversationId, UUID userId) {
        log.debug("Streaming response for conversation {}", conversationId);

        return Mono.fromCallable(() -> {
            Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

            Companion companion = companionRepository.findById(conversation.getCompanionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Companion", conversation.getCompanionId().toString()));

            List<Message> contextMessages = messageRepository.findRecentMessages(conversationId, CONTEXT_MESSAGE_LIMIT);
            String context = contextService.buildContext(conversationId, contextMessages);

            return new StreamContext(conversation, companion, context);
        }).flatMapMany(ctx -> {
            UUID messageId = UUID.randomUUID();
            StringBuilder fullResponse = new StringBuilder();

            return aiProviderService.streamResponse(
                    ctx.companion.getSystemPrompt(),
                    ctx.context,
                    ctx.companion.getModelProvider(),
                    ctx.companion.getModelName()
            ).map(chunk -> {
                fullResponse.append(chunk);
                // CORRECTED: StreamChunk.text takes 3 params: messageId, conversationId, content
                return StreamChunk.text(messageId, conversationId, chunk);
            }).concatWith(Mono.defer(() -> {
                // Save complete message
                int tokens = aiProviderService.estimateTokens(fullResponse.toString());
                saveStreamedMessage(conversationId, fullResponse.toString(), tokens);
                return Mono.just(StreamChunk.complete(messageId, conversationId, tokens));
            })).onErrorResume(e -> {
                log.error("Error streaming response for conversation {}", conversationId, e);
                return Mono.just(StreamChunk.error(conversationId, e.getMessage()));
            });
        });
    }

    /**
     * Get messages for a conversation with pagination.
     *
     * @param conversationId Conversation ID
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of messages
     */
    @Transactional(readOnly = true)
    public Page<MessageDTO> getMessages(UUID conversationId, UUID userId, Pageable pageable) {
        // Verify conversation ownership
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        // CORRECTED: Use the correct method name
        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        return messages.map(this::mapToDTO);
    }

    /**
     * Edit a message.
     *
     * @param messageId Message ID
     * @param userId User ID
     * @param newContent New content
     * @return Updated message DTO
     */
    @Transactional
    public MessageDTO editMessage(UUID messageId, UUID userId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId.toString()));

        // Verify ownership
        final UUID msgConversationId = message.getConversationId();
        conversationRepository.findByIdAndUserId(msgConversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", msgConversationId.toString()));

        if (message.getRole() != MessageRole.USER) {
            throw new BusinessException("EDIT_NOT_ALLOWED", "Only user messages can be edited");
        }

        // Content moderation
        if (!contentFilterService.isContentSafe(newContent)) {
            throw new BusinessException("CONTENT_BLOCKED", "Message contains inappropriate content");
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        Message savedMessage = messageRepository.save(message);

        log.info("Message {} edited by user {}", messageId, userId);
        return mapToDTO(savedMessage);
    }

    /**
     * Delete a message.
     *
     * @param messageId Message ID
     * @param userId User ID
     */
    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId.toString()));

        // Verify ownership
        conversationRepository.findByIdAndUserId(message.getConversationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", message.getConversationId().toString()));

        messageRepository.delete(message);
        log.info("Message {} deleted by user {}", messageId, userId);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Save a streamed AI message after streaming completes.
     */
    @Transactional
    protected void saveStreamedMessage(UUID conversationId, String content, int tokens) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .type(MessageType.TEXT)
                .content(content)
                .tokensUsed(tokens)
                .build();
        messageRepository.save(message);

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null) {
            conversation.incrementMessages();
            conversation.addTokens(tokens);
            conversationRepository.save(conversation);
        }
    }

    /**
     * Parse message type from string.
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
     * Convert Message entity to DTO.
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

    /**
     * Internal record for streaming context.
     */
    private record StreamContext(Conversation conversation, Companion companion, String context) {}
}
