package com.nexusai.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.conversation.dto.*;
import com.nexusai.conversation.service.ConversationService;
import com.nexusai.conversation.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationController.class)
@DisplayName("ConversationController Integration Tests")
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private MessageService messageService;

    private UUID userId;
    private UUID conversationId;
    private UUID companionId;
    private ConversationDTO mockConversation;
    private MessageDTO mockMessage;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        companionId = UUID.randomUUID();

        mockConversation = ConversationDTO.builder()
                .id(conversationId)
                .userId(userId)
                .companionId(companionId)
                .companionName("Test Companion")
                .title("Test Conversation")
                .status("ACTIVE")
                .messageCount(5)
                .totalTokens(500L)
                .lastActivityAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        mockMessage = MessageDTO.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .role("USER")
                .type("TEXT")
                .content("Hello, World!")
                .isEdited(false)
                .createdAt(LocalDateTime.now())
                .build();

        userPrincipal = new UserPrincipal(userId, "test@example.com", "USER");
    }

    @Nested
    @DisplayName("POST /api/v1/conversations")
    class CreateConversationTests {

        @Test
        @DisplayName("Should create conversation successfully")
        void shouldCreateConversationSuccessfully() throws Exception {
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .companionId(companionId)
                    .title("New Chat")
                    .build();

            when(conversationService.createConversation(any(UUID.class), any(CreateConversationRequest.class)))
                    .thenReturn(mockConversation);

            mockMvc.perform(post("/api/v1/conversations")
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(conversationId.toString()))
                    .andExpect(jsonPath("$.title").value("Test Conversation"))
                    .andExpect(jsonPath("$.companionName").value("Test Companion"));
        }

        @Test
        @DisplayName("Should return 400 when companionId is missing")
        @WithMockUser
        void shouldReturn400WhenCompanionIdMissing() throws Exception {
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .title("New Chat")
                    .build();

            mockMvc.perform(post("/api/v1/conversations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            CreateConversationRequest request = CreateConversationRequest.builder()
                    .companionId(companionId)
                    .build();

            mockMvc.perform(post("/api/v1/conversations")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/conversations")
    class GetConversationsTests {

        @Test
        @DisplayName("Should return user conversations")
        void shouldReturnUserConversations() throws Exception {
            Page<ConversationDTO> page = new PageImpl<>(List.of(mockConversation), PageRequest.of(0, 10), 1);
            when(conversationService.getUserConversations(any(UUID.class), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/conversations")
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(conversationId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return empty page when no conversations")
        void shouldReturnEmptyPageWhenNoConversations() throws Exception {
            Page<ConversationDTO> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(conversationService.getUserConversations(any(UUID.class), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/conversations")
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/conversations/{id}")
    class GetConversationTests {

        @Test
        @DisplayName("Should return conversation by ID")
        void shouldReturnConversationById() throws Exception {
            when(conversationService.getConversation(conversationId, userId)).thenReturn(mockConversation);

            mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(conversationId.toString()))
                    .andExpect(jsonPath("$.title").value("Test Conversation"));
        }

        @Test
        @DisplayName("Should return 404 when conversation not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(conversationService.getConversation(any(UUID.class), any(UUID.class)))
                    .thenThrow(new ResourceNotFoundException("Conversation", conversationId.toString()));

            mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/conversations/{id}/title")
    class UpdateTitleTests {

        @Test
        @DisplayName("Should update conversation title")
        void shouldUpdateConversationTitle() throws Exception {
            mockConversation.setTitle("Updated Title");
            when(conversationService.updateTitle(conversationId, userId, "Updated Title"))
                    .thenReturn(mockConversation);

            mockMvc.perform(patch("/api/v1/conversations/" + conversationId + "/title")
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Updated Title\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/conversations/{id}/archive")
    class ArchiveConversationTests {

        @Test
        @DisplayName("Should archive conversation successfully")
        void shouldArchiveConversationSuccessfully() throws Exception {
            doNothing().when(conversationService).archiveConversation(conversationId, userId);

            mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/archive")
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isNoContent());

            verify(conversationService).archiveConversation(conversationId, userId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/conversations/{id}")
    class DeleteConversationTests {

        @Test
        @DisplayName("Should delete conversation successfully")
        void shouldDeleteConversationSuccessfully() throws Exception {
            doNothing().when(conversationService).deleteConversation(conversationId, userId);

            mockMvc.perform(delete("/api/v1/conversations/" + conversationId)
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isNoContent());

            verify(conversationService).deleteConversation(conversationId, userId);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent conversation")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(new ResourceNotFoundException("Conversation", conversationId.toString()))
                    .when(conversationService).deleteConversation(any(UUID.class), any(UUID.class));

            mockMvc.perform(delete("/api/v1/conversations/" + conversationId)
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Message Endpoints")
    class MessageTests {

        @Test
        @DisplayName("Should get messages for conversation")
        void shouldGetMessagesForConversation() throws Exception {
            Page<MessageDTO> page = new PageImpl<>(List.of(mockMessage), PageRequest.of(0, 10), 1);
            when(conversationService.getMessagesPaginated(eq(conversationId), any(UUID.class), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].content").value("Hello, World!"))
                    .andExpect(jsonPath("$.content[0].role").value("USER"));
        }

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() throws Exception {
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("Hello!")
                    .type("TEXT")
                    .build();

            when(messageService.sendMessage(eq(conversationId), any(UUID.class), any(SendMessageRequest.class)))
                    .thenReturn(mockMessage);

            mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("Hello, World!"));
        }

        @Test
        @DisplayName("Should return 400 for empty message content")
        @WithMockUser
        void shouldReturn400ForEmptyContent() throws Exception {
            SendMessageRequest request = SendMessageRequest.builder()
                    .content("")
                    .build();

            mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should generate AI response")
        void shouldGenerateAIResponse() throws Exception {
            MessageDTO aiResponse = MessageDTO.builder()
                    .id(UUID.randomUUID())
                    .conversationId(conversationId)
                    .role("ASSISTANT")
                    .type("TEXT")
                    .content("I'm doing well, thank you!")
                    .tokensUsed(25)
                    .build();

            when(messageService.generateResponse(eq(conversationId), any(UUID.class)))
                    .thenReturn(aiResponse);

            mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/generate")
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ASSISTANT"))
                    .andExpect(jsonPath("$.content").value("I'm doing well, thank you!"));
        }

        @Test
        @DisplayName("Should edit message")
        void shouldEditMessage() throws Exception {
            UUID messageId = mockMessage.getId();
            MessageDTO editedMessage = MessageDTO.builder()
                    .id(messageId)
                    .conversationId(conversationId)
                    .role("USER")
                    .type("TEXT")
                    .content("Edited content")
                    .isEdited(true)
                    .build();

            when(messageService.editMessage(eq(messageId), any(UUID.class), eq("Edited content")))
                    .thenReturn(editedMessage);

            mockMvc.perform(put("/api/v1/conversations/" + conversationId + "/messages/" + messageId)
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Edited content\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("Edited content"))
                    .andExpect(jsonPath("$.isEdited").value(true));
        }

        @Test
        @DisplayName("Should delete message")
        void shouldDeleteMessage() throws Exception {
            UUID messageId = mockMessage.getId();
            doNothing().when(messageService).deleteMessage(messageId, userId);

            mockMvc.perform(delete("/api/v1/conversations/" + conversationId + "/messages/" + messageId)
                            .with(csrf())
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isNoContent());

            verify(messageService).deleteMessage(messageId, userId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/conversations/{id}/search")
    class SearchMessagesTests {

        @Test
        @DisplayName("Should search messages in conversation")
        void shouldSearchMessagesInConversation() throws Exception {
            Page<MessageDTO> page = new PageImpl<>(List.of(mockMessage), PageRequest.of(0, 10), 1);
            when(conversationService.searchMessages(eq(conversationId), any(UUID.class), eq("hello"), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/search")
                            .with(user(userPrincipal.getEmail()).roles("USER"))
                            .param("query", "hello"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].content").value("Hello, World!"));
        }

        @Test
        @DisplayName("Should return 400 without query parameter")
        void shouldReturn400WithoutQueryParameter() throws Exception {
            mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/search")
                            .with(user(userPrincipal.getEmail()).roles("USER")))
                    .andExpect(status().isBadRequest());
        }
    }
}
