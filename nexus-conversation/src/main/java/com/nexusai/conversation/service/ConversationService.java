package com.nexusai.conversation.service;

import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.commons.exception.ValidationException;
import com.nexusai.conversation.dto.*;
import com.nexusai.core.entity.Companion;
import com.nexusai.core.entity.Conversation;
import com.nexusai.core.entity.Message;
import com.nexusai.core.enums.ConversationStatus;
import com.nexusai.core.enums.MessageRole;
import com.nexusai.core.enums.MessageType;
import com.nexusai.core.repository.CompanionRepository;
import com.nexusai.core.repository.ConversationRepository;
import com.nexusai.core.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CompanionRepository companionRepository;
    private final ContextService contextService;

    @Transactional
    public ConversationDTO createConversation(UUID userId, CreateConversationRequest request) {
        Companion companion = companionRepository.findById(request.getCompanionId())
                .orElseThrow(() -> new ResourceNotFoundException("Companion", request.getCompanionId().toString()));

        Conversation conversation = Conversation.builder()
                .userId(userId)
                .companionId(companion.getId())
                .title(request.getTitle() != null ? request.getTitle() : "Chat with " + companion.getName())
                .status(ConversationStatus.ACTIVE)
                .lastActivityAt(LocalDateTime.now())
                .messageCount(0)
                .totalTokens(0L)
                .build();

        conversation = conversationRepository.save(conversation);
        log.info("Created conversation {} for user {} with companion {}",
                conversation.getId(), userId, companion.getId());

        return mapToDTO(conversation, companion);
    }

    @Transactional(readOnly = true)
    public Page<ConversationDTO> getUserConversations(UUID userId, Pageable pageable) {
        return conversationRepository
                .findByUserIdAndStatusOrderByLastActivityAtDesc(userId, ConversationStatus.ACTIVE, pageable)
                .map(conv -> {
                    Companion companion = companionRepository.findById(conv.getCompanionId()).orElse(null);
                    return mapToDTO(conv, companion);
                });
    }

    @Transactional(readOnly = true)
    public ConversationDTO getConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        Companion companion = companionRepository.findById(conversation.getCompanionId()).orElse(null);
        return mapToDTO(conversation, companion);
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(UUID conversationId, UUID userId, int limit) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        return messageRepository.findRecentMessages(conversationId, limit)
                .stream()
                .map(this::mapMessageToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MessageDTO> getMessagesPaginated(UUID conversationId, UUID userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::mapMessageToDTO);
    }

    @Transactional
    public void archiveConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        conversation.setStatus(ConversationStatus.ARCHIVED);
        conversationRepository.save(conversation);
        log.info("Archived conversation {} for user {}", conversationId, userId);
    }

    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        conversationRepository.delete(conversation);
        contextService.clearContext(conversationId);
        log.info("Deleted conversation {} for user {}", conversationId, userId);
    }

    @Transactional
    public ConversationDTO updateTitle(UUID conversationId, UUID userId, String newTitle) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new ValidationException("INVALID_TITLE", "Title cannot be empty");
        }

        conversation.setTitle(newTitle.trim());
        conversation = conversationRepository.save(conversation);

        Companion companion = companionRepository.findById(conversation.getCompanionId()).orElse(null);
        return mapToDTO(conversation, companion);
    }

    @Transactional(readOnly = true)
    public Page<MessageDTO> searchMessages(UUID conversationId, UUID userId, String query, Pageable pageable) {
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        return messageRepository.searchInConversation(conversationId, query, pageable)
                .map(this::mapMessageToDTO);
    }

    private ConversationDTO mapToDTO(Conversation conversation, Companion companion) {
        return ConversationDTO.builder()
                .id(conversation.getId())
                .userId(conversation.getUserId())
                .companionId(conversation.getCompanionId())
                .companionName(companion != null ? companion.getName() : null)
                .companionAvatar(companion != null ? companion.getAvatarUrl() : null)
                .title(conversation.getTitle())
                .status(conversation.getStatus().name())
                .messageCount(conversation.getMessageCount())
                .totalTokens(conversation.getTotalTokens())
                .lastActivityAt(conversation.getLastActivityAt())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private MessageDTO mapMessageToDTO(Message message) {
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
