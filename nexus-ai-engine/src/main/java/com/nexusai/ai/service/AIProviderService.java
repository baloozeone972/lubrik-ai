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
}
