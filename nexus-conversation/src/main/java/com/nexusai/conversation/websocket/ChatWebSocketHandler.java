package com.nexusai.conversation.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.conversation.dto.SendMessageRequest;
import com.nexusai.conversation.dto.StreamChunk;
import com.nexusai.conversation.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket session connected: {}", sessionId);

        return session.receive()
                .flatMap(message -> handleMessage(session, message))
                .doOnError(error -> log.error("WebSocket error for session {}: {}", sessionId, error.getMessage()))
                .doFinally(signal -> {
                    sessions.remove(sessionId);
                    log.info("WebSocket session disconnected: {}", sessionId);
                })
                .then();
    }

    private Flux<Void> handleMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String payload = message.getPayloadAsText();
            WebSocketRequest request = objectMapper.readValue(payload, WebSocketRequest.class);

            return switch (request.type()) {
                case "send_message" -> handleSendMessage(session, request);
                case "stream_response" -> handleStreamResponse(session, request);
                case "ping" -> handlePing(session);
                default -> sendError(session, "Unknown message type: " + request.type());
            };
        } catch (Exception e) {
            log.error("Error parsing WebSocket message", e);
            return sendError(session, "Invalid message format");
        }
    }

    private Flux<Void> handleSendMessage(WebSocketSession session, WebSocketRequest request) {
        try {
            UUID conversationId = UUID.fromString(request.conversationId());
            UUID userId = UUID.fromString(request.userId());

            SendMessageRequest sendRequest = objectMapper.convertValue(request.data(), SendMessageRequest.class);
            messageService.sendMessage(conversationId, userId, sendRequest);

            return sendAck(session, "message_sent", conversationId.toString());
        } catch (Exception e) {
            log.error("Error handling send message", e);
            return sendError(session, e.getMessage());
        }
    }

    private Flux<Void> handleStreamResponse(WebSocketSession session, WebSocketRequest request) {
        try {
            UUID conversationId = UUID.fromString(request.conversationId());
            UUID userId = UUID.fromString(request.userId());

            return messageService.streamResponse(conversationId, userId)
                    .flatMap(chunk -> sendChunk(session, chunk))
                    .onErrorResume(e -> {
                        log.error("Error streaming response", e);
                        return sendError(session, e.getMessage());
                    });
        } catch (Exception e) {
            log.error("Error handling stream response", e);
            return sendError(session, e.getMessage());
        }
    }

    private Flux<Void> handlePing(WebSocketSession session) {
        return sendMessage(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
    }

    private Flux<Void> sendChunk(WebSocketSession session, StreamChunk chunk) {
        return sendMessage(session, Map.of(
                "type", "stream_chunk",
                "data", chunk
        ));
    }

    private Flux<Void> sendAck(WebSocketSession session, String action, String data) {
        return sendMessage(session, Map.of(
                "type", "ack",
                "action", action,
                "data", data
        ));
    }

    private Flux<Void> sendError(WebSocketSession session, String error) {
        return sendMessage(session, Map.of(
                "type", "error",
                "message", error
        ));
    }

    private Flux<Void> sendMessage(WebSocketSession session, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return Flux.just(session.send(Mono.just(session.textMessage(json)))).flatMap(m -> m);
        } catch (Exception e) {
            log.error("Error sending WebSocket message", e);
            return Flux.empty();
        }
    }

    public void broadcastToConversation(UUID conversationId, StreamChunk chunk) {
        sessions.values().forEach(session -> {
            sendChunk(session, chunk).subscribe();
        });
    }

    private record WebSocketRequest(String type, String conversationId, String userId, Object data) {}
}
