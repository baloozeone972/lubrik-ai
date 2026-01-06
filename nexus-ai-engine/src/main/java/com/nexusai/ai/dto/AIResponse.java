package com.nexusai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO représentant une réponse de l'IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponse {
    
    /**
     * Le contenu de la réponse
     */
    private String content;
    
    /**
     * Nombre de tokens utilisés dans la requête
     */
    private Integer promptTokens;
    
    /**
     * Nombre de tokens générés dans la réponse
     */
    private Integer completionTokens;
    
    /**
     * Nombre total de tokens
     */
    private Integer totalTokens;
    
    /**
     * Provider utilisé (openai, anthropic)
     */
    private String provider;
    
    /**
     * Modèle utilisé
     */
    private String model;
    
    /**
     * Durée de traitement en ms
     */
    private Long processingTimeMs;
    
    /**
     * Indique si la réponse a été tronquée
     */
    @Builder.Default
    private Boolean truncated = false;
    
    /**
     * Raison de l'arrêt (stop, length, content_filter)
     */
    private String finishReason;
}
