package com.nexusai.conversation.service;

import com.nexusai.ai.service.AIProviderService;
import com.nexusai.commons.exception.BusinessException;
import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.conversation.dto.MessageDTO;
import com.nexusai.conversation.dto.SendMessageRequest;
import com.nexusai.core.entity.Companion;
import com.nexusai.core.entity.Conversation;
import com.nexusai.core.entity.Message;
import com.nexusai.core.enums.ConversationStatus;
import com.nexusai.core.enums.MessageRole;
import com.nexusai.core.enums.MessageType;
import com.nexusai.core.repository.CompanionRepository;
import com.nexusai.core.repository.ConversationRepository;
import com.nexusai.core.repository.MessageRepository;
import com.nexusai.moderation.service.ContentFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Tests")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private CompanionRepository companionRepository;

    @Mock
    private AIProviderService aiProviderService;

    @Mock
    private ContentFilterService contentFilterService;

    @Mock
    private ContextService contextService;

    @InjectMocks
    private MessageService messageService;

    private UUID userId;
    private UUID companionId;
    private UUID conversationId;
    private UUID messageId;
    private Companion testCompanion;
    private Conversation testConversation;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companionId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        testCompanion = Companion.builder()
                .id(companionId)
                .userId(userId)
                .name("Test Companion")
                .systemPrompt("You are a helpful assistant.")
                .modelProvider("ollama")
                .modelName("llama3")
                .build();

        testConversation = Conversation.builder()
                .id(conversationId)
                .userId(userId)
                .companionId(companionId)
                .title("Test Conversation")
                .status(ConversationStatus.ACTIVE)
                .messageCount(0)
                .totalTokens(0L)
                .lastActivityAt(LocalDateTime.now())
                .build();

        testMessage = Message.builder()
                .id(messageId)
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .type(MessageType.TEXT)
                .content("Hello!")
                .build();
    }

    @Nested
    @DisplayName("SendMessage Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() {
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Hello, how are you?")
                    .type("TEXT")
                    .build();

            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(contentFilterService.isContentSafe(anyString())).thenReturn(true);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message saved = invocation.getArgument(0);
                saved.setId(messageId);
                return saved;
            });

            MessageDTO result = messageService.sendMessage(conversationId, userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Hello, how are you?");
            assertThat(result.getRole()).isEqualTo("USER");
            assertThat(result.getType()).isEqualTo("TEXT");

            verify(conversationRepository).incrementMessageCount(eq(conversationId), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should reject inappropriate content")
        void shouldRejectInappropriateContent() {
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("inappropriate content")
                    .build();

            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(contentFilterService.isContentSafe(anyString())).thenReturn(false);

            assertThatThrownBy(() -> messageService.sendMessage(conversationId, userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CONTENT_BLOCKED");

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for non-existent conversation")
        void shouldThrowExceptionForNonExistentConversation() {
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Hello")
                    .build();

            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.sendMessage(conversationId, userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle null message type as TEXT")
        void shouldHandleNullMessageTypeAsText() {
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Hello")
                    .type(null)
                    .build();

            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(contentFilterService.isContentSafe(anyString())).thenReturn(true);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message saved = invocation.getArgument(0);
                saved.setId(messageId);
                return saved;
            });

            MessageDTO result = messageService.sendMessage(conversationId, userId, request);

            assertThat(result.getType()).isEqualTo("TEXT");
        }

        @Test
        @DisplayName("Should handle parent message ID")
        void shouldHandleParentMessageId() {
            UUID parentId = UUID.randomUUID();
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Reply to parent")
                    .parentMessageId(parentId)
                    .build();

            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(contentFilterService.isContentSafe(anyString())).thenReturn(true);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

            messageService.sendMessage(conversationId, userId, request);

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().getParentMessageId()).isEqualTo(parentId);
        }
    }

    @Nested
    @DisplayName("GenerateResponse Tests")
    class GenerateResponseTests {

        @Test
        @DisplayName("Should generate AI response successfully")
        void shouldGenerateAIResponseSuccessfully() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(companionRepository.findById(companionId))
                    .thenReturn(Optional.of(testCompanion));
            when(messageRepository.findRecentMessages(conversationId, 20))
                    .thenReturn(List.of(testMessage));
            when(contextService.buildContext(eq(conversationId), anyList()))
                    .thenReturn("Context text");
            when(aiProviderService.generateResponse(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("AI response text");
            when(aiProviderService.estimateTokens(anyString())).thenReturn(50);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

            MessageDTO result = messageService.generateResponse(conversationId, userId);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("AI response text");
            assertThat(result.getRole()).isEqualTo("ASSISTANT");
            assertThat(result.getTokensUsed()).isEqualTo(50);

            verify(aiProviderService).generateResponse(
                    eq("You are a helpful assistant."),
                    eq("Context text"),
                    eq("ollama"),
                    eq("llama3")
            );
        }

        @Test
        @DisplayName("Should throw exception when conversation not found")
        void shouldThrowExceptionWhenConversationNotFound() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.generateResponse(conversationId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(aiProviderService, never()).generateResponse(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when companion not found")
        void shouldThrowExceptionWhenCompanionNotFound() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(companionRepository.findById(companionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.generateResponse(conversationId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(aiProviderService, never()).generateResponse(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should update conversation stats after generating response")
        void shouldUpdateConversationStatsAfterGenerating() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(companionRepository.findById(companionId))
                    .thenReturn(Optional.of(testCompanion));
            when(messageRepository.findRecentMessages(conversationId, 20))
                    .thenReturn(Collections.emptyList());
            when(contextService.buildContext(eq(conversationId), anyList()))
                    .thenReturn("Context");
            when(aiProviderService.generateResponse(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("Response");
            when(aiProviderService.estimateTokens(anyString())).thenReturn(100);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

            messageService.generateResponse(conversationId, userId);

            verify(conversationRepository).save(any(Conversation.class));
        }
    }

    @Nested
    @DisplayName("EditMessage Tests")
    class EditMessageTests {

        @Test
        @DisplayName("Should edit user message successfully")
        void shouldEditUserMessageSuccessfully() {
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(contentFilterService.isContentSafe(anyString())).thenReturn(true);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

            MessageDTO result = messageService.editMessage(messageId, userId, "Updated content");

            assertThat(result.getContent()).isEqualTo("Updated content");
            assertThat(result.getIsEdited()).isTrue();

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().getIsEdited()).isTrue();
        }

        @Test
        @DisplayName("Should not allow editing assistant messages")
        void shouldNotAllowEditingAssistantMessages() {
            Message assistantMessage = Message.builder()
                    .id(messageId)
                    .conversationId(conversationId)
                    .role(MessageRole.ASSISTANT)
                    .type(MessageType.TEXT)
                    .content("AI response")
                    .build();

            when(messageRepository.findById(messageId)).thenReturn(Optional.of(assistantMessage));
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));

            assertThatThrownBy(() -> messageService.editMessage(messageId, userId, "New content"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("EDIT_NOT_ALLOWED");

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject inappropriate content when editing")
        void shouldRejectInappropriateContentWhenEditing() {
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(contentFilterService.isContentSafe(anyString())).thenReturn(false);

            assertThatThrownBy(() -> messageService.editMessage(messageId, userId, "Bad content"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CONTENT_BLOCKED");

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for non-existent message")
        void shouldThrowExceptionForNonExistentMessage() {
            when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.editMessage(messageId, userId, "New content"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should verify user ownership before editing")
        void shouldVerifyUserOwnershipBeforeEditing() {
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.editMessage(messageId, userId, "New content"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("DeleteMessage Tests")
    class DeleteMessageTests {

        @Test
        @DisplayName("Should delete message successfully")
        void shouldDeleteMessageSuccessfully() {
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));

            messageService.deleteMessage(messageId, userId);

            verify(messageRepository).delete(testMessage);
        }

        @Test
        @DisplayName("Should throw exception for non-existent message")
        void shouldThrowExceptionForNonExistentMessage() {
            when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.deleteMessage(messageId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(messageRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should verify user ownership before deleting")
        void shouldVerifyUserOwnershipBeforeDeleting() {
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(testMessage));
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.deleteMessage(messageId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(messageRepository, never()).delete(any());
        }
    }
}
