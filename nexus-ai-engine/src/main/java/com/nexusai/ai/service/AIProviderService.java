package com.nexusai.ai.service;

import com.nexusai.ai.dto.ChatRequest;
import com.nexusai.ai.dto.ChatResponse;
import com.nexusai.core.enums.MessageRole;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Interface for AI provider services.
 */
public interface AIProviderService {

    /**
     * Generates a chat completion.
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Generates a streaming chat completion.
     */
    Flux<String> chatStream(ChatRequest request);

    /**
     * Returns the provider name.
     */
    String getProviderName();

    /**
     * Checks if the provider is available.
     */
    boolean isAvailable();

    /**
     * Generates a response using the specified model.
     */
    default String generateResponse(String systemPrompt, String context, String modelProvider, String modelName) {
        List<ChatRequest.Message> messages = List.of(
                ChatRequest.Message.builder().role(MessageRole.SYSTEM).content(systemPrompt).build(),
                ChatRequest.Message.builder().role(MessageRole.USER).content(context).build()
        );
        ChatRequest request = ChatRequest.builder()
                .model(modelName)
                .messages(messages)
                .build();
        ChatResponse response = chat(request);
        return response.getContent();
    }

    /**
     * Streams a response using the specified model.
     */
    default Flux<String> streamResponse(String systemPrompt, String context, String modelProvider, String modelName) {
        List<ChatRequest.Message> messages = List.of(
                ChatRequest.Message.builder().role(MessageRole.SYSTEM).content(systemPrompt).build(),
                ChatRequest.Message.builder().role(MessageRole.USER).content(context).build()
        );
        ChatRequest request = ChatRequest.builder()
                .model(modelName)
                .messages(messages)
                .build();
        return chatStream(request);
    }

    /**
     * Estimates the number of tokens in the given text.
     * Default implementation uses a simple word-based estimate.
     */
    default int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Rough estimate: ~4 characters per token on average
        return (int) Math.ceil(text.length() / 4.0);
    }
}
