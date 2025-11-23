package com.nexusai.ai.service;

import com.nexusai.ai.dto.ChatRequest;
import com.nexusai.ai.dto.ChatResponse;
import com.nexusai.ai.dto.OllamaChatRequest;
import com.nexusai.ai.dto.OllamaChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OllamaService implements AIProviderService {

    private final WebClient webClient;

    @Value("${nexusai.ai.ollama.model:llama3}")
    private String defaultModel;

    public OllamaService(@Value("${nexusai.ai.ollama.base-url:http://localhost:11434}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        OllamaChatRequest ollamaRequest = buildOllamaRequest(request);

        OllamaChatResponse response = webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ollamaRequest)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .timeout(Duration.ofSeconds(120))
                .block();

        return ChatResponse.builder()
                .content(response != null && response.getMessage() != null
                        ? response.getMessage().getContent() : "")
                .model(defaultModel)
                .tokensUsed(response != null ? response.getEvalCount() : 0)
                .build();
    }

    @Override
    public Flux<String> chatStream(ChatRequest request) {
        OllamaChatRequest ollamaRequest = buildOllamaRequest(request);
        ollamaRequest.setStream(true);

        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ollamaRequest)
                .retrieve()
                .bodyToFlux(OllamaChatResponse.class)
                .map(response -> response.getMessage() != null
                        ? response.getMessage().getContent() : "")
                .filter(content -> content != null && !content.isEmpty());
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public boolean isAvailable() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("Ollama service is not available: {}", e.getMessage());
            return false;
        }
    }

    private OllamaChatRequest buildOllamaRequest(ChatRequest request) {
        List<OllamaChatRequest.Message> messages = request.getMessages().stream()
                .map(m -> OllamaChatRequest.Message.builder()
                        .role(m.getRole().name().toLowerCase())
                        .content(m.getContent())
                        .build())
                .collect(Collectors.toList());

        return OllamaChatRequest.builder()
                .model(request.getModel() != null ? request.getModel() : defaultModel)
                .messages(messages)
                .stream(false)
                .build();
    }
}
