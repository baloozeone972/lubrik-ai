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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Transactional
    public MessageDTO sendMessage(UUID conversationId, UUID userId, SendMessageRequest request) {
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

    @Transactional
    public MessageDTO generateResponse(UUID conversationId, UUID userId) {
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

    public Flux<StreamChunk> streamResponse(UUID conversationId, UUID userId) {
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

    @Transactional
    public MessageDTO editMessage(UUID messageId, UUID userId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId.toString()));

        // Verify ownership via conversation
        final UUID msgConversationId = message.getConversationId();
        conversationRepository.findByIdAndUserId(msgConversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", msgConversationId.toString()));

        if (message.getRole() != MessageRole.USER) {
            throw new BusinessException("EDIT_NOT_ALLOWED", "Only user messages can be edited");
        }

        if (!contentFilterService.isContentSafe(newContent)) {
            throw new BusinessException("CONTENT_BLOCKED", "Message contains inappropriate content");
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        Message savedMessage = messageRepository.save(message);

        log.info("Message {} edited by user", messageId);
        return mapToDTO(savedMessage);
    }

    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId.toString()));

        conversationRepository.findByIdAndUserId(message.getConversationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", message.getConversationId().toString()));

        messageRepository.delete(message);
        log.info("Message {} deleted by user", messageId);
    }

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

    private MessageType parseMessageType(String type) {
        if (type == null) return MessageType.TEXT;
        try {
            return MessageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MessageType.TEXT;
        }
    }

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

    private record StreamContext(Conversation conversation, Companion companion, String context) {}
}
