package com.nexusai.api.controller;

import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.conversation.dto.*;
import com.nexusai.conversation.service.ConversationService;
import com.nexusai.conversation.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Chat conversation management endpoints")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<ConversationDTO> createConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationDTO conversation = conversationService.createConversation(
                principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    @GetMapping
    @Operation(summary = "Get user's conversations")
    public ResponseEntity<Page<ConversationDTO>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Page<ConversationDTO> conversations = conversationService.getUserConversations(
                principal.getUserId(), pageable);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation by ID")
    public ResponseEntity<ConversationDTO> getConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId) {
        ConversationDTO conversation = conversationService.getConversation(
                conversationId, principal.getUserId());
        return ResponseEntity.ok(conversation);
    }

    @PatchMapping("/{conversationId}/title")
    @Operation(summary = "Update conversation title")
    public ResponseEntity<ConversationDTO> updateTitle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @RequestBody Map<String, String> request) {
        ConversationDTO conversation = conversationService.updateTitle(
                conversationId, principal.getUserId(), request.get("title"));
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/{conversationId}/archive")
    @Operation(summary = "Archive a conversation")
    public ResponseEntity<Void> archiveConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId) {
        conversationService.archiveConversation(conversationId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId) {
        conversationService.deleteConversation(conversationId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    // Message endpoints

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get messages in a conversation")
    public ResponseEntity<Page<MessageDTO>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            Pageable pageable) {
        Page<MessageDTO> messages = conversationService.getMessagesPaginated(
                conversationId, principal.getUserId(), pageable);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send a message")
    public ResponseEntity<MessageDTO> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageDTO message = messageService.sendMessage(
                conversationId, principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @PostMapping("/{conversationId}/generate")
    @Operation(summary = "Generate AI response")
    public ResponseEntity<MessageDTO> generateResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId) {
        MessageDTO response = messageService.generateResponse(
                conversationId, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{conversationId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream AI response (SSE)")
    public Flux<StreamChunk> streamResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId) {
        return messageService.streamResponse(conversationId, principal.getUserId());
    }

    @PutMapping("/{conversationId}/messages/{messageId}")
    @Operation(summary = "Edit a message")
    public ResponseEntity<MessageDTO> editMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @RequestBody Map<String, String> request) {
        MessageDTO message = messageService.editMessage(
                messageId, principal.getUserId(), request.get("content"));
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    @Operation(summary = "Delete a message")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {
        messageService.deleteMessage(messageId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{conversationId}/search")
    @Operation(summary = "Search messages in conversation")
    public ResponseEntity<Page<MessageDTO>> searchMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID conversationId,
            @RequestParam String query,
            Pageable pageable) {
        Page<MessageDTO> messages = conversationService.searchMessages(
                conversationId, principal.getUserId(), query, pageable);
        return ResponseEntity.ok(messages);
    }
}
