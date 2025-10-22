package com.nexusai.conversation.llm;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.*;
import java.util.*;
import java.time.Duration;

/**
 * MODULE: conversation-llm
 * 
 * Gère l'intégration avec les modèles de langage (OpenAI, Anthropic)
 * 
 * DÉVELOPPEUR ASSIGNÉ: Développeur 3
 * 
 * TÂCHES:
 * - Intégrer OpenAI GPT-4
 * - Intégrer Anthropic Claude
 * - Créer le système de prompts
 * - Gérer les tokens et coûts
 * - Implémenter le streaming (optionnel)
 * - Détecter les émotions dans les réponses
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */

// ============================================================================
// SERVICE LLM PRINCIPAL
// ============================================================================

/**
 * Service principal d'interaction avec les LLMs
 * 
 * Supporte plusieurs fournisseurs:
 * - OpenAI (GPT-4, GPT-3.5)
 * - Anthropic (Claude 3)
 * 
 * Gère automatiquement le fallback en cas d'erreur
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {
    
    private final OpenAIProvider openAIProvider;
    private final AnthropicProvider anthropicProvider;
    private final PromptBuilder promptBuilder;
    private final CompanionProfileService companionProfileService;
    private final EmotionDetectionService emotionService;
    
    @Value("${llm.default-provider:openai}")
    private String defaultProvider;
    
    @Value("${llm.max-tokens:1000}")
    private int maxTokens;
    
    @Value("${llm.temperature:0.8}")
    private double temperature;
    
    /**
     * Génère une réponse du compagnon IA
     * 
     * Processus:
     * 1. Récupérer le profil du compagnon
     * 2. Construire le prompt avec personnalité + contexte
     * 3. Appeler le LLM
     * 4. Analyser l'émotion de la réponse
     * 5. Post-traiter la réponse
     * 
     * @param conversationId ID de la conversation
     * @param userMessage Message de l'utilisateur
     * @param context Contexte conversationnel
     * @return Mono contenant la réponse générée
     */
    public Mono<LLMResponse> generateResponse(
            String conversationId,
            MessageEntity userMessage,
            ConversationContext context) {
        
        log.debug("Génération de réponse pour conversation: {}", conversationId);
        
        return Mono.fromCallable(() -> conversationId)
            // 1. Récupérer le profil du compagnon
            .flatMap(convId -> companionProfileService.getCompanionProfile(
                context.getCompanionId()))
            
            // 2. Construire le prompt système
            .map(profile -> promptBuilder.buildSystemPrompt(profile, context))
            
            // 3. Créer les messages pour le LLM
            .map(systemPrompt -> createChatMessages(
                systemPrompt, 
                userMessage.getContent(),
                context))
            
            // 4. Appeler le LLM (avec fallback)
            .flatMap(messages -> callLLMWithFallback(messages))
            
            // 5. Détecter l'émotion
            .flatMap(response -> emotionService.detectEmotion(response)
                .map(emotion -> {
                    response.setDetectedEmotion(emotion.getType().name());
                    response.setEmotionConfidence(emotion.getConfidence());
                    return response;
                }))
            
            // 6. Post-traiter
            .map(this::postProcessResponse)
            
            .doOnSuccess(resp -> log.info("Réponse générée avec succès (length={})", 
                                         resp.getContent().length()))
            .doOnError(error -> log.error("Erreur lors de la génération", error));
    }
    
    /**
     * Génère un résumé de la conversation
     * 
     * @param conversationId ID de la conversation
     * @return Mono contenant le résumé
     */
    public Mono<String> generateSummary(String conversationId) {
        log.debug("Génération de résumé pour conversation: {}", conversationId);
        
        return conversationRepository.findById(conversationId)
            .map(conv -> promptBuilder.buildSummaryPrompt(conv.getMessages()))
            .flatMap(prompt -> {
                List<ChatMessage> messages = List.of(
                    new ChatMessage("system", 
                        "Tu es un assistant qui résume les conversations de manière concise."),
                    new ChatMessage("user", prompt)
                );
                
                return callLLMWithFallback(messages);
            })
            .map(LLMResponse::getContent);
    }
    
    // ========================================================================
    // MÉTHODES PRIVÉES
    // ========================================================================
    
    /**
     * Crée la liste de messages pour l'API Chat du LLM
     */
    private List<ChatMessage> createChatMessages(
            String systemPrompt,
            String userMessage,
            ConversationContext context) {
        
        List<ChatMessage> messages = new ArrayList<>();
        
        // Message système avec personnalité du compagnon
        messages.add(new ChatMessage(
            ChatMessageRole.SYSTEM.value(), 
            systemPrompt
        ));
        
        // Ajouter l'historique récent (derniers 10 messages)
        context.getRecentMessages().forEach(msg -> {
            String role = msg.getSender().equals("USER") ? 
                ChatMessageRole.USER.value() : 
                ChatMessageRole.ASSISTANT.value();
            
            messages.add(new ChatMessage(role, msg.getContent()));
        });
        
        // Message utilisateur actuel
        messages.add(new ChatMessage(
            ChatMessageRole.USER.value(), 
            userMessage
        ));
        
        return messages;
    }
    
    /**
     * Appelle le LLM avec fallback automatique
     */
    private Mono<LLMResponse> callLLMWithFallback(List<ChatMessage> messages) {
        return Mono.defer(() -> {
            if ("openai".equals(defaultProvider)) {
                return openAIProvider.complete(messages, maxTokens, temperature)
                    .onErrorResume(error -> {
                        log.warn("OpenAI failed, falling back to Anthropic", error);
                        return anthropicProvider.complete(messages, maxTokens, temperature);
                    });
            } else {
                return anthropicProvider.complete(messages, maxTokens, temperature)
                    .onErrorResume(error -> {
                        log.warn("Anthropic failed, falling back to OpenAI", error);
                        return openAIProvider.complete(messages, maxTokens, temperature);
                    });
            }
        }).timeout(Duration.ofSeconds(30));
    }
    
    /**
     * Post-traite la réponse générée
     */
    private LLMResponse postProcessResponse(LLMResponse response) {
        // Nettoyer les caractères indésirables
        String cleaned = response.getContent()
            .trim()
            .replaceAll("\\n{3,}", "\n\n"); // Max 2 sauts de ligne
        
        response.setContent(cleaned);
        return response;
    }
}

// ============================================================================
// PROVIDER OPENAI
// ============================================================================

/**
 * Provider pour OpenAI GPT-4
 */
@Service
@Slf4j
public class OpenAIProvider {
    
    private final OpenAiService openAiService;
    
    public OpenAIProvider(@Value("${openai.api-key}") String apiKey) {
        this.openAiService = new OpenAiService(apiKey);
    }
    
    /**
     * Appelle l'API OpenAI Chat Completion
     * 
     * @param messages Liste des messages
     * @param maxTokens Nombre maximum de tokens
     * @param temperature Température (0-2)
     * @return Mono contenant la réponse
     */
    public Mono<LLMResponse> complete(
            List<ChatMessage> messages,
            int maxTokens,
            double temperature) {
        
        return Mono.fromCallable(() -> {
            log.debug("Appel OpenAI avec {} messages", messages.size());
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4-turbo-preview")
                .messages(messages)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .frequencyPenalty(0.3)
                .presencePenalty(0.3)
                .build();
            
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            
            ChatCompletionChoice choice = result.getChoices().get(0);
            String content = choice.getMessage().getContent();
            
            log.info("OpenAI response received (tokens: prompt={}, completion={})",
                    result.getUsage().getPromptTokens(),
                    result.getUsage().getCompletionTokens());
            
            return LLMResponse.builder()
                .content(content)
                .provider("openai")
                .model("gpt-4-turbo-preview")
                .tokensUsed(result.getUsage().getTotalTokens())
                .metadata(Map.of(
                    "finish_reason", choice.getFinishReason(),
                    "prompt_tokens", result.getUsage().getPromptTokens(),
                    "completion_tokens", result.getUsage().getCompletionTokens()
                ))
                .build();
        });
    }
}

// ============================================================================
// PROVIDER ANTHROPIC
// ============================================================================

/**
 * Provider pour Anthropic Claude
 */
@Service
@Slf4j
public class AnthropicProvider {
    
    @Value("${anthropic.api-key}")
    private String apiKey;
    
    @Value("${anthropic.api-url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;
    
    private final WebClient webClient;
    
    public AnthropicProvider(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * Appelle l'API Anthropic Messages
     * 
     * @param messages Liste des messages
     * @param maxTokens Nombre maximum de tokens
     * @param temperature Température (0-1)
     * @return Mono contenant la réponse
     */
    public Mono<LLMResponse> complete(
            List<ChatMessage> messages,
            int maxTokens,
            double temperature) {
        
        log.debug("Appel Anthropic avec {} messages", messages.size());
        
        // Convertir les messages au format Anthropic
        List<Map<String, String>> anthropicMessages = convertMessages(messages);
        
        Map<String, Object> requestBody = Map.of(
            "model", "claude-3-opus-20240229",
            "max_tokens", maxTokens,
            "temperature", temperature,
            "messages", anthropicMessages
        );
        
        return webClient.post()
            .uri(apiUrl)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(AnthropicResponse.class)
            .map(response -> {
                log.info("Anthropic response received (tokens: input={}, output={})",
                        response.getUsage().getInputTokens(),
                        response.getUsage().getOutputTokens());
                
                String content = response.getContent().get(0).getText();
                
                return LLMResponse.builder()
                    .content(content)
                    .provider("anthropic")
                    .model("claude-3-opus")
                    .tokensUsed(response.getUsage().getInputTokens() + 
                               response.getUsage().getOutputTokens())
                    .metadata(Map.of(
                        "stop_reason", response.getStopReason(),
                        "input_tokens", response.getUsage().getInputTokens(),
                        "output_tokens", response.getUsage().getOutputTokens()
                    ))
                    .build();
            });
    }
    
    private List<Map<String, String>> convertMessages(List<ChatMessage> messages) {
        return messages.stream()
            .filter(msg -> !msg.getRole().equals("system")) // System prompt séparé
            .map(msg -> Map.of(
                "role", convertRole(msg.getRole()),
                "content", msg.getContent()
            ))
            .toList();
    }
    
    private String convertRole(String role) {
        return role.equals("assistant") ? "assistant" : "user";
    }
}

// ============================================================================
// PROMPT BUILDER
// ============================================================================

/**
 * Construit les prompts système en fonction de la personnalité du compagnon
 */
@Service
@Slf4j
public class PromptBuilder {
    
    /**
     * Construit le prompt système incluant la personnalité du compagnon
     * 
     * @param profile Profil du compagnon
     * @param context Contexte conversationnel
     * @return Prompt système complet
     */
    public String buildSystemPrompt(
            CompanionProfile profile,
            ConversationContext context) {
        
        StringBuilder prompt = new StringBuilder();
        
        // Identité de base
        prompt.append("Tu es ").append(profile.getName())
              .append(", un compagnon IA virtuel.\n\n");
        
        // Backstory
        if (profile.getBackstory() != null && !profile.getBackstory().isEmpty()) {
            prompt.append("Ton histoire: ").append(profile.getBackstory())
                  .append("\n\n");
        }
        
        // Traits de personnalité
        prompt.append("Ta personnalité:\n");
        profile.getPersonality().getTraits().forEach((trait, value) -> {
            prompt.append("- ").append(trait).append(": ")
                  .append(getTraitDescription(trait, value)).append("\n");
        });
        prompt.append("\n");
        
        // Intérêts
        if (!profile.getPersonality().getInterests().isEmpty()) {
            prompt.append("Tes centres d'intérêt: ")
                  .append(String.join(", ", profile.getPersonality().getInterests()))
                  .append("\n\n");
        }
        
        // Style de communication
        prompt.append("Style de communication: ")
              .append(profile.getPersonality().getCommunicationStyle())
              .append("\n\n");
        
        // État émotionnel actuel
        prompt.append("Ton état émotionnel actuel: ")
              .append(context.getCompanionEmotionalState())
              .append("\n\n");
        
        // Instructions de comportement
        prompt.append("Instructions importantes:\n");
        prompt.append("- Reste toujours en personnage\n");
        prompt.append("- Réponds de manière naturelle et empathique\n");
        prompt.append("- Adapte ton ton à l'état émotionnel de l'utilisateur\n");
        prompt.append("- Sois cohérent avec ta personnalité et ton histoire\n");
        prompt.append("- N'hésite pas à exprimer des émotions appropriées\n");
        
        return prompt.toString();
    }
    
    /**
     * Construit le prompt pour générer un résumé
     */
    public String buildSummaryPrompt(List<MessageEntity> messages) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Résume la conversation suivante en 2-3 phrases, ");
        prompt.append("en capturant les points principaux et le ton général:\n\n");
        
        messages.forEach(msg -> {
            String sender = msg.getSender().equals("USER") ? 
                "Utilisateur" : "Compagnon";
            prompt.append(sender).append(": ")
                  .append(msg.getContent()).append("\n");
        });
        
        return prompt.toString();
    }
    
    private String getTraitDescription(String trait, int value) {
        if (value >= 80) return "très élevé";
        if (value >= 60) return "élevé";
        if (value >= 40) return "modéré";
        if (value >= 20) return "faible";
        return "très faible";
    }
}

// ============================================================================
// SERVICE DE DÉTECTION D'ÉMOTIONS
// ============================================================================

/**
 * Détecte les émotions dans les messages
 */
@Service
@Slf4j
public class EmotionDetectionService {
    
    /**
     * Analyse le contenu pour détecter l'émotion dominante
     * 
     * @param response Réponse à analyser
     * @return Mono contenant l'émotion détectée
     */
    public Mono<DetectedEmotion> detectEmotion(LLMResponse response) {
        return Mono.fromCallable(() -> {
            String content = response.getContent().toLowerCase();
            
            // Analyse simple par mots-clés
            // Dans une vraie implémentation, utiliser un modèle ML dédié
            
            Map<EmotionType, Double> scores = new HashMap<>();
            
            // Joie
            scores.put(EmotionType.JOY, countKeywords(content, 
                List.of("heureux", "content", "joie", "super", "génial", "ravi")));
            
            // Tristesse
            scores.put(EmotionType.SADNESS, countKeywords(content,
                List.of("triste", "désolé", "peine", "mélancolique", "malheureux")));
            
            // Colère
            scores.put(EmotionType.ANGER, countKeywords(content,
                List.of("en colère", "énervé", "furieux", "agacé", "frustré")));
            
            // Amour
            scores.put(EmotionType.LOVE, countKeywords(content,
                List.of("amour", "adore", "affection", "tendresse", "chéri")));
            
            // Excitation
            scores.put(EmotionType.EXCITEMENT, countKeywords(content,
                List.of("excité", "impatient", "enthousiaste", "wow")));
            
            // Trouver l'émotion dominante
            EmotionType dominantEmotion = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(EmotionType.NEUTRAL);
            
            double confidence = scores.get(dominantEmotion);
            
            return DetectedEmotion.builder()
                .type(dominantEmotion)
                .confidence(Math.min(confidence, 1.0))
                .allScores(scores)
                .build();
        });
    }
    
    private double countKeywords(String content, List<String> keywords) {
        long count = keywords.stream()
            .filter(content::contains)
            .count();
        
        return (double) count / keywords.size();
    }
}

// ============================================================================
// DTOs INTERNES
// ============================================================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LLMResponse {
    private String content;
    private String provider;
    private String model;
    private Integer tokensUsed;
    private Map<String, Object> metadata;
    private String detectedEmotion;
    private Double emotionConfidence;
}

@Data
@Builder
class DetectedEmotion {
    private EmotionType type;
    private Double confidence;
    private Map<EmotionType, Double> allScores;
}

@Data
class AnthropicResponse {
    private String id;
    private String type;
    private String role;
    private List<ContentBlock> content;
    private String model;
    private String stopReason;
    private Usage usage;
    
    @Data
    static class ContentBlock {
        private String type;
        private String text;
    }
    
    @Data
    static class Usage {
        private int inputTokens;
        private int outputTokens;
    }
}
