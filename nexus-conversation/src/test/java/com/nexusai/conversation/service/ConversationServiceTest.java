package com.nexusai.conversation.service;

import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.commons.exception.ValidationException;
import com.nexusai.conversation.dto.ConversationDTO;
import com.nexusai.conversation.dto.CreateConversationRequest;
import com.nexusai.conversation.dto.MessageDTO;
import com.nexusai.core.entity.Companion;
import com.nexusai.core.entity.Conversation;
import com.nexusai.core.entity.Message;
import com.nexusai.core.enums.ConversationStatus;
import com.nexusai.core.enums.MessageRole;
import com.nexusai.core.enums.MessageType;
import com.nexusai.core.repository.CompanionRepository;
import com.nexusai.core.repository.ConversationRepository;
import com.nexusai.core.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationService Tests")
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private CompanionRepository companionRepository;

    @Mock
    private ContextService contextService;

    @InjectMocks
    private ConversationService conversationService;

    private UUID userId;
    private UUID companionId;
    private UUID conversationId;
    private Companion testCompanion;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companionId = UUID.randomUUID();
        conversationId = UUID.randomUUID();

        testCompanion = Companion.builder()
               // .id(companionId)
                .userId(userId)
                .name("Test Companion")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        testConversation = Conversation.builder()
               // .id(conversationId)
                .userId(userId)
                .companionId(companionId)
                .title("Test Conversation")
                .status(ConversationStatus.ACTIVE)
                .messageCount(5)
                .totalTokens(500L)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("CreateConversation Tests")
    class CreateConversationTests {

        @Test
        @DisplayName("Should create conversation with custom title")
        void shouldCreateConversationWithCustomTitle() {
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .companionId(companionId)
                    .title("My Custom Chat")
                    .build();

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
                Conversation saved = invocation.getArgument(0);
                saved.setId(conversationId);
                return saved;
            });

            ConversationDTO result = conversationService.createConversation(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("My Custom Chat");
            assertThat(result.getCompanionId()).isEqualTo(companionId);
            assertThat(result.getCompanionName()).isEqualTo("Test Companion");

            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should create conversation with default title when none provided")
        void shouldCreateConversationWithDefaultTitle() {
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .companionId(companionId)
                    .build();

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
                Conversation saved = invocation.getArgument(0);
                saved.setId(conversationId);
                return saved;
            });

            ConversationDTO result = conversationService.createConversation(userId, request);

            assertThat(result.getTitle()).isEqualTo("Chat with Test Companion");
        }

        @Test
        @DisplayName("Should throw exception when companion not found")
        void shouldThrowExceptionWhenCompanionNotFound() {
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .companionId(companionId)
                    .build();

            when(companionRepository.findById(companionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.createConversation(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(conversationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("GetUserConversations Tests")
    class GetUserConversationsTests {

        @Test
        @DisplayName("Should return paginated conversations for user")
        void shouldReturnPaginatedConversationsForUser() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Conversation> conversationPage = new PageImpl<>(List.of(testConversation), pageable, 1);

            when(conversationRepository.findByUserIdAndStatusOrderByLastActivityAtDesc(userId, ConversationStatus.ACTIVE, pageable))
                    .thenReturn(conversationPage);
            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            Page<ConversationDTO> result = conversationService.getUserConversations(userId, pageable);

            assertThat(result).isNotEmpty();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Conversation");
        }

        @Test
        @DisplayName("Should return empty page when user has no conversations")
        void shouldReturnEmptyPageWhenNoConversations() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Conversation> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(conversationRepository.findByUserIdAndStatusOrderByLastActivityAtDesc(userId, ConversationStatus.ACTIVE, pageable))
                    .thenReturn(emptyPage);

            Page<ConversationDTO> result = conversationService.getUserConversations(userId, pageable);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("GetConversation Tests")
    class GetConversationTests {

        @Test
        @DisplayName("Should return conversation when found")
        void shouldReturnConversationWhenFound() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            ConversationDTO result = conversationService.getConversation(conversationId, userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(conversationId);
            assertThat(result.getTitle()).isEqualTo("Test Conversation");
            assertThat(result.getMessageCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw exception when conversation not found")
        void shouldThrowExceptionWhenConversationNotFound() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.getConversation(conversationId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GetMessages Tests")
    class GetMessagesTests {

        @Test
        @DisplayName("Should return messages for conversation")
        void shouldReturnMessagesForConversation() {
            Message message = Message.builder()
                    //.id(UUID.randomUUID())
                    .conversationId(conversationId)
                    .role(MessageRole.USER)
                    .type(MessageType.TEXT)
                    .content("Hello!")
                    .build();

            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(messageRepository.findRecentMessages(conversationId, 20))
                    .thenReturn(List.of(message));

            List<MessageDTO> result = conversationService.getMessages(conversationId, userId, 20);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("Hello!");
            assertThat(result.get(0).getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("Should throw exception for unauthorized conversation access")
        void shouldThrowExceptionForUnauthorizedAccess() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.getMessages(conversationId, userId, 20))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ArchiveConversation Tests")
    class ArchiveConversationTests {

        @Test
        @DisplayName("Should archive conversation successfully")
        void shouldArchiveConversationSuccessfully() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

            conversationService.archiveConversation(conversationId, userId);

            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ConversationStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Should throw exception when archiving non-existent conversation")
        void shouldThrowExceptionWhenArchivingNonExistent() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.archiveConversation(conversationId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(conversationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("DeleteConversation Tests")
    class DeleteConversationTests {

        @Test
        @DisplayName("Should delete conversation and clear context")
        void shouldDeleteConversationAndClearContext() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));

            conversationService.deleteConversation(conversationId, userId);

            verify(conversationRepository).delete(testConversation);
            verify(contextService).clearContext(conversationId);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent conversation")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.deleteConversation(conversationId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(conversationRepository, never()).delete(any());
            verify(contextService, never()).clearContext(any());
        }
    }

    @Nested
    @DisplayName("UpdateTitle Tests")
    class UpdateTitleTests {

        @Test
        @DisplayName("Should update conversation title successfully")
        void shouldUpdateTitleSuccessfully() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            ConversationDTO result = conversationService.updateTitle(conversationId, userId, "New Title");

            assertThat(result.getTitle()).isEqualTo("New Title");

            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("Should throw exception for empty title")
        void shouldThrowExceptionForEmptyTitle() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));

            assertThatThrownBy(() -> conversationService.updateTitle(conversationId, userId, ""))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Title");

            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for null title")
        void shouldThrowExceptionForNullTitle() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));

            assertThatThrownBy(() -> conversationService.updateTitle(conversationId, userId, null))
                    .isInstanceOf(ValidationException.class);

            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should trim whitespace from title")
        void shouldTrimWhitespaceFromTitle() {
            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            conversationService.updateTitle(conversationId, userId, "  Trimmed Title  ");

            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            verify(conversationRepository).save(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("Trimmed Title");
        }
    }

    @Nested
    @DisplayName("SearchMessages Tests")
    class SearchMessagesTests {

        @Test
        @DisplayName("Should search messages in conversation")
        void shouldSearchMessagesInConversation() {
            Pageable pageable = PageRequest.of(0, 10);
            Message message = Message.builder()
                   // .id(UUID.randomUUID())
                    .conversationId(conversationId)
                    .role(MessageRole.USER)
                    .type(MessageType.TEXT)
                    .content("Hello world!")
                    .build();
            Page<Message> messagePage = new PageImpl<>(List.of(message), pageable, 1);

            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(messageRepository.searchInConversation(conversationId, "hello", pageable))
                    .thenReturn(messagePage);

            Page<MessageDTO> result = conversationService.searchMessages(conversationId, userId, "hello", pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).getContent()).contains("Hello");
        }

        @Test
        @DisplayName("Should return empty when no search results")
        void shouldReturnEmptyWhenNoSearchResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Message> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(conversationRepository.findByIdAndUserId(conversationId, userId))
                    .thenReturn(Optional.of(testConversation));
            when(messageRepository.searchInConversation(conversationId, "nonexistent", pageable))
                    .thenReturn(emptyPage);

            Page<MessageDTO> result = conversationService.searchMessages(conversationId, userId, "nonexistent", pageable);

            assertThat(result).isEmpty();
        }
    }
}
