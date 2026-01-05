package com.nexusai.conversation.controller;

import com.nexusai.conversation.dto.SendMessageRequest;
import com.nexusai.conversation.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Controller pour le streaming des messages IA en temps réel.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/stream")
@RequiredArgsConstructor
public class MessageStreamController {

    private final MessageService messageService;

    /**
     * Stream un message avec réponse IA progressive (SSE).
     * 
     * @param conversationId ID de la conversation
     * @param request Requête contenant le message
     * @param authentication Authentification de l'utilisateur
     * @return Flux de chunks de texte
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(
            @PathVariable UUID conversationId,
            @RequestBody SendMessageRequest request,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        
        log.info("Starting message stream for conversation: {} by user: {}", conversationId, userId);
        
        return messageService.streamMessage(conversationId, userId, request)
                .map(chunk -> "data: " + chunk + "\n\n")
                .doOnComplete(() -> log.info("Stream completed for conversation: {}", conversationId))
                .doOnError(e -> log.error("Stream error for conversation: {}", conversationId, e));
    }
}
