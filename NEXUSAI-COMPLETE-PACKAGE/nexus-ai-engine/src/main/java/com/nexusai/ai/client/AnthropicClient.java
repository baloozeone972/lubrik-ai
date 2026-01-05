package com.nexusai.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.ai.dto.AIRequest;
import com.nexusai.ai.dto.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implémentation du client Anthropic (Claude).
 * Utilise l'API Messages de Anthropic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnthropicClient implements AIClient {
    
    @Value("${anthropic.api.key}")
    private String apiKey;
    
    @Value("${anthropic.api.url:https://api.anthropic.com/v1}")
    private String apiUrl;
    
    @Value("${anthropic.model:claude-3-5-sonnet-20241022}")
    private String defaultModel;
    
    @Value("${anthropic.version:2023-06-01}")
    private String apiVersion;
    
    @Value("${anthropic.timeout:30}")
    private int timeoutSeconds;
    
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    
    @Override
    public AIResponse generateResponse(AIRequest request) {
        log.info("Generating Anthropic response for message: {}", 
                request.getMessage().substring(0, Math.min(50, request.getMessage().length())));
        
        long startTime = System.currentTimeMillis();
        
        try {
            WebClient webClient = createWebClient();
            
            Map<String, Object> requestBody = buildRequestBody(request, false);
            
            JsonNode response = webClient.post()
                    .uri("/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
            
            return parseResponse(response, startTime);
            
        } catch (Exception e) {
            log.error("Error calling Anthropic API", e);
            throw new RuntimeException("Failed to generate AI response: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> generateStreamResponse(AIRequest request) {
        log.info("Starting Anthropic streaming response");
        
        WebClient webClient = createWebClient();
        Map<String, Object> requestBody = buildRequestBody(request, true);
        
        return webClient.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .mapNotNull(this::extractContentFromStreamChunk)
                .doOnComplete(() -> log.info("Anthropic streaming completed"))
                .doOnError(e -> log.error("Error in Anthropic streaming", e));
    }
    
    @Override
    public int countTokens(String message) {
        // Estimation approximative pour Claude
        return (int) Math.ceil(message.length() / 4.0);
    }
    
    @Override
    public String getProviderName() {
        return "anthropic";
    }
    
    /**
     * Crée un WebClient configuré pour Anthropic
     */
    private WebClient createWebClient() {
        return webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", apiVersion)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    /**
     * Construit le corps de la requête pour Anthropic
     */
    private Map<String, Object> buildRequestBody(AIRequest request, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        
        // Modèle
        body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        
        // System prompt
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            body.put("system", request.getSystemPrompt());
        }
        
        // Messages
        List<Map<String, String>> messages = new ArrayList<>();
        
        // Historique de conversation
        if (request.getConversationHistory() != null) {
            for (AIRequest.ConversationMessage msg : request.getConversationHistory()) {
                messages.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()
                ));
            }
        }
        
        // Message actuel
        messages.add(Map.of(
            "role", "user",
            "content", request.getMessage()
        ));
        
        body.put("messages", messages);
        
        // Paramètres
        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());
        body.put("stream", stream);
        
        return body;
    }
    
    /**
     * Parse la réponse JSON d'Anthropic
     */
    private AIResponse parseResponse(JsonNode response, long startTime) {
        try {
            JsonNode content = response.get("content").get(0);
            JsonNode usage = response.get("usage");
            
            return AIResponse.builder()
                    .content(content.get("text").asText())
                    .promptTokens(usage.get("input_tokens").asInt())
                    .completionTokens(usage.get("output_tokens").asInt())
                    .totalTokens(usage.get("input_tokens").asInt() + usage.get("output_tokens").asInt())
                    .provider("anthropic")
                    .model(response.get("model").asText())
                    .finishReason(response.get("stop_reason").asText())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .truncated("max_tokens".equals(response.get("stop_reason").asText()))
                    .build();
        } catch (Exception e) {
            log.error("Error parsing Anthropic response", e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }
    
    /**
     * Extrait le contenu d'un chunk de streaming
     */
    private String extractContentFromStreamChunk(String chunk) {
        try {
            JsonNode json = objectMapper.readTree(chunk);
            String type = json.get("type").asText();
            
            if ("content_block_delta".equals(type)) {
                JsonNode delta = json.get("delta");
                if (delta.has("text")) {
                    return delta.get("text").asText();
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Error parsing stream chunk: {}", chunk, e);
            return null;
        }
    }
}
