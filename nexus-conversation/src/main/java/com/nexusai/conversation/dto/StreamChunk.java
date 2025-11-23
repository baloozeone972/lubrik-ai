package com.nexusai.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamChunk {
    private UUID messageId;
    private UUID conversationId;
    private String type;
    private String content;
    private Boolean isComplete;
    private Integer tokensUsed;
    private String error;

    public static StreamChunk text(UUID messageId, UUID conversationId, String content) {
        return StreamChunk.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .type("text")
                .content(content)
                .isComplete(false)
                .build();
    }

    public static StreamChunk complete(UUID messageId, UUID conversationId, int tokens) {
        return StreamChunk.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .type("complete")
                .isComplete(true)
                .tokensUsed(tokens)
                .build();
    }

    public static StreamChunk error(UUID conversationId, String error) {
        return StreamChunk.builder()
                .conversationId(conversationId)
                .type("error")
                .error(error)
                .isComplete(true)
                .build();
    }
}
