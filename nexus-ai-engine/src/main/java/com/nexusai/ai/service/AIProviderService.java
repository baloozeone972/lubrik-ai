package com.nexusai.ai.service;

import com.nexusai.ai.dto.ChatRequest;
import com.nexusai.ai.dto.ChatResponse;
import reactor.core.publisher.Flux;

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
