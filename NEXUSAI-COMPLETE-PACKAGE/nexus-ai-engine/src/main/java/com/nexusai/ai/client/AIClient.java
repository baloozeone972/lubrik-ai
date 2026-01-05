package com.nexusai.ai.client;

import com.nexusai.ai.dto.AIRequest;
import com.nexusai.ai.dto.AIResponse;
import reactor.core.publisher.Flux;

/**
 * Interface commune pour les clients IA (OpenAI, Anthropic, etc.)
 * 
 * @author NexusAI Team
 * @version 1.0
 */
public interface AIClient {
    
    /**
     * Génère une réponse IA de manière synchrone.
     * 
     * @param request La requête contenant le message et le contexte
     * @return La réponse complète de l'IA
     */
    AIResponse generateResponse(AIRequest request);
    
    /**
     * Génère une réponse IA en streaming (SSE).
     * 
     * @param request La requête contenant le message et le contexte
     * @return Un flux de chunks de réponse
     */
    Flux<String> generateStreamResponse(AIRequest request);
    
    /**
     * Compte les tokens d'un message.
     * 
     * @param message Le message à analyser
     * @return Le nombre de tokens estimé
     */
    int countTokens(String message);
    
    /**
     * Retourne le nom du provider (openai, anthropic, etc.)
     */
    String getProviderName();
}
