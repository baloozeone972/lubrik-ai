package com.nexusai.ai.service;

import com.nexusai.ai.client.AIClient;
import com.nexusai.ai.dto.AIRequest;
import com.nexusai.ai.dto.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Service principal pour la génération de réponses IA.
 * Gère la sélection du provider et l'orchestration des appels.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    @Value("${ai.default.provider:openai}")
    private String defaultProvider;
    
    private final Map<String, AIClient> aiClients;
    
    /**
     * Génère une réponse IA avec le provider par défaut.
     */
    public AIResponse generateResponse(AIRequest request) {
        String provider = request.getModel() != null && request.getModel().contains("claude") 
                ? "anthropic" 
                : defaultProvider;
        
        return generateResponse(request, provider);
    }
    
    /**
     * Génère une réponse IA avec un provider spécifique.
     */
    public AIResponse generateResponse(AIRequest request, String providerName) {
        log.info("Generating AI response with provider: {}", providerName);
        
        AIClient client = getClient(providerName);
        
        try {
            AIResponse response = client.generateResponse(request);
            log.info("AI response generated: {} tokens used", response.getTotalTokens());
            return response;
        } catch (Exception e) {
            log.error("Error generating AI response with provider: {}", providerName, e);
            throw new RuntimeException("Failed to generate AI response", e);
        }
    }
    
    /**
     * Génère une réponse IA en streaming.
     */
    public Flux<String> generateStreamResponse(AIRequest request) {
        String provider = request.getModel() != null && request.getModel().contains("claude") 
                ? "anthropic" 
                : defaultProvider;
        
        return generateStreamResponse(request, provider);
    }
    
    /**
     * Génère une réponse IA en streaming avec un provider spécifique.
     */
    public Flux<String> generateStreamResponse(AIRequest request, String providerName) {
        log.info("Starting AI streaming with provider: {}", providerName);
        
        AIClient client = getClient(providerName);
        request.setStream(true);
        
        return client.generateStreamResponse(request)
                .doOnComplete(() -> log.info("AI streaming completed"))
                .doOnError(e -> log.error("Error in AI streaming", e));
    }
    
    /**
     * Compte les tokens d'un message.
     */
    public int countTokens(String message, String providerName) {
        AIClient client = getClient(providerName);
        return client.countTokens(message);
    }
    
    /**
     * Liste les providers disponibles.
     */
    public List<String> getAvailableProviders() {
        return aiClients.values().stream()
                .map(AIClient::getProviderName)
                .toList();
    }
    
    /**
     * Récupère un client par son nom.
     */
    private AIClient getClient(String providerName) {
        AIClient client = aiClients.values().stream()
                .filter(c -> c.getProviderName().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown AI provider: " + providerName));
        
        log.debug("Using AI provider: {}", client.getProviderName());
        return client;
    }
}
