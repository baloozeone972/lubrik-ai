package com.nexusai.ai.dto;

import com.nexusai.core.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer maxTokens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private MessageRole role;
        private String content;
    }
}
