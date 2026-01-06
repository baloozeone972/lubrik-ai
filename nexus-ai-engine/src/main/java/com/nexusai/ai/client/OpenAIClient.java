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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implémentation du client OpenAI.
 * Utilise l'API Chat Completions de OpenAI (GPT-4, GPT-3.5-turbo, etc.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIClient implements AIClient {
    
    @Value("${openai.api.key}")
    private String apiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4-turbo-preview}")
    private String defaultModel;
    
    @Value("${openai.timeout:30}")
    private int timeoutSeconds;
    
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    
    @Override
    public AIResponse generateResponse(AIRequest request) {
        log.info("Generating OpenAI response for message: {}", 
                request.getMessage().substring(0, Math.min(50, request.getMessage().length())));
        
        long startTime = System.currentTimeMillis();
        
        try {
            WebClient webClient = createWebClient();
            
            Map<String, Object> requestBody = buildRequestBody(request, false);
            
            JsonNode response = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(timeoutSeconds));
            
            return parseResponse(response, startTime);
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to generate AI response: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> generateStreamResponse(AIRequest request) {
        log.info("Starting OpenAI streaming response");
        
        WebClient webClient = createWebClient();
        Map<String, Object> requestBody = buildRequestBody(request, true);
        
        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
                .map(line -> line.substring(6)) // Remove "data: " prefix
                .mapNotNull(this::extractContentFromStreamChunk)
                .doOnComplete(() -> log.info("OpenAI streaming completed"))
                .doOnError(e -> log.error("Error in OpenAI streaming", e));
    }
    
    @Override
    public int countTokens(String message) {
        // Estimation approximative : 1 token ≈ 4 caractères en anglais
        // Pour une estimation plus précise, utiliser tiktoken (library Python)
        return (int) Math.ceil(message.length() / 4.0);
    }
    
    @Override
    public String getProviderName() {
        return "openai";
    }
    
    /**
     * Crée un WebClient configuré pour OpenAI
     */
    private WebClient createWebClient() {
        return webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    /**
     * Construit le corps de la requête pour OpenAI
     */
    private Map<String, Object> buildRequestBody(AIRequest request, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        
        // Modèle
        body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        
        // Messages
        List<Map<String, String>> messages = new ArrayList<>();
        
        // System prompt
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(Map.of(
                "role", "system",
                "content", request.getSystemPrompt()
            ));
        }
        
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
        body.put("temperature", request.getTemperature());
        body.put("max_tokens", request.getMaxTokens());
        body.put("stream", stream);
        
        return body;
    }
    
    /**
     * Parse la réponse JSON d'OpenAI
     */
    private AIResponse parseResponse(JsonNode response, long startTime) {
        try {
            JsonNode choice = response.get("choices").get(0);
            JsonNode message = choice.get("message");
            JsonNode usage = response.get("usage");
            
            return AIResponse.builder()
                    .content(message.get("content").asText())
                    .promptTokens(usage.get("prompt_tokens").asInt())
                    .completionTokens(usage.get("completion_tokens").asInt())
                    .totalTokens(usage.get("total_tokens").asInt())
                    .provider("openai")
                    .model(response.get("model").asText())
                    .finishReason(choice.get("finish_reason").asText())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .truncated("length".equals(choice.get("finish_reason").asText()))
                    .build();
        } catch (Exception e) {
            log.error("Error parsing OpenAI response", e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }
    
    /**
     * Extrait le contenu d'un chunk de streaming
     */
    private String extractContentFromStreamChunk(String chunk) {
        try {
            JsonNode json = objectMapper.readTree(chunk);
            JsonNode delta = json.get("choices").get(0).get("delta");
            
            if (delta.has("content")) {
                return delta.get("content").asText();
            }
            return null;
        } catch (Exception e) {
            log.warn("Error parsing stream chunk: {}", chunk, e);
            return null;
        }
    }
}
