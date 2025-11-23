package com.nexusai.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaChatResponse {

    private String model;
    private Message message;
    private Boolean done;

    @JsonProperty("eval_count")
    private Integer evalCount;

    @JsonProperty("prompt_eval_count")
    private Integer promptEvalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
