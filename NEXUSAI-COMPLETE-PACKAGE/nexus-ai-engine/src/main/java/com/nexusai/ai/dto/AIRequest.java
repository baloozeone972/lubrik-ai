package com.nexusai.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO représentant une requête vers l'IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {
    
    /**
     * Le message de l'utilisateur
     */
    private String message;
    
    /**
     * L'historique de conversation (contexte)
     */
    private List<ConversationMessage> conversationHistory;
    
    /**
     * Les instructions système (personnalité du companion)
     */
    private String systemPrompt;
    
    /**
     * Température de génération (0.0 - 1.0)
     */
    @Builder.Default
    private Double temperature = 0.7;
    
    /**
     * Nombre maximum de tokens dans la réponse
     */
    @Builder.Default
    private Integer maxTokens = 1000;
    
    /**
     * Modèle à utiliser (optionnel)
     */
    private String model;
    
    /**
     * Activer le streaming
     */
    @Builder.Default
    private Boolean stream = false;
    
    /**
     * Message dans l'historique
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMessage {
        private String role; // "user" ou "assistant"
        private String content;
    }
}
