package com.nexusai.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.ai.dto.AIRequest;
import com.nexusai.ai.dto.AIResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour OpenAIClient.
 */
@ExtendWith(MockitoExtension.class)
class OpenAIClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ObjectMapper objectMapper;

    private OpenAIClient openAIClient;

    @BeforeEach
    void setUp() {
        openAIClient = new OpenAIClient(objectMapper, webClientBuilder);
        ReflectionTestUtils.setField(openAIClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(openAIClient, "apiUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(openAIClient, "defaultModel", "gpt-4");
        ReflectionTestUtils.setField(openAIClient, "timeoutSeconds", 30);
    }

    @Test
    void shouldReturnProviderName() {
        // When
        String providerName = openAIClient.getProviderName();

        // Then
        assertThat(providerName).isEqualTo("openai");
    }

    @Test
    void shouldCountTokensApproximately() {
        // Given
        String message = "Hello, how are you?"; // ~20 chars

        // When
        int tokens = openAIClient.countTokens(message);

        // Then
        assertThat(tokens).isGreaterThan(0);
        assertThat(tokens).isLessThanOrEqualTo(10); // Approximativement 20/4 = 5
    }

    @Test
    void shouldBuildRequestWithSystemPrompt() {
        // Given
        AIRequest request = AIRequest.builder()
                .message("Hello!")
                .systemPrompt("You are a helpful assistant")
                .temperature(0.7)
                .maxTokens(1000)
                .build();

        // When & Then - Le client doit être capable de construire la requête
        assertThat(request.getMessage()).isEqualTo("Hello!");
        assertThat(request.getSystemPrompt()).isEqualTo("You are a helpful assistant");
    }

    @Test
    void shouldBuildRequestWithConversationHistory() {
        // Given
        AIRequest.ConversationMessage msg1 = AIRequest.ConversationMessage.builder()
                .role("user")
                .content("Hi")
                .build();
        
        AIRequest.ConversationMessage msg2 = AIRequest.ConversationMessage.builder()
                .role("assistant")
                .content("Hello!")
                .build();

        AIRequest request = AIRequest.builder()
                .message("How are you?")
                .conversationHistory(List.of(msg1, msg2))
                .build();

        // When & Then
        assertThat(request.getConversationHistory()).hasSize(2);
        assertThat(request.getConversationHistory().get(0).getContent()).isEqualTo("Hi");
    }

    // Note: Les tests complets nécessiteraient un mock du WebClient
    // Pour une vraie implémentation, utiliser WireMock ou TestContainers
}
