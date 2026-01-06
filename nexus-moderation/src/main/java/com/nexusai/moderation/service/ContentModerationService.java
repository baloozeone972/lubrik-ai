package com.nexusai.moderation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.commons.exception.ValidationException;
import com.nexusai.core.entity.ContentModeration;
import com.nexusai.core.enums.ContentType;
import com.nexusai.core.enums.ModerationAction;
import com.nexusai.core.enums.ModerationStatus;
import com.nexusai.core.repository.ContentModerationRepository;
import com.nexusai.moderation.dto.ModerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service de modération de contenu avec Azure Content Moderator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    @Value("${azure.content-moderator.endpoint:}")
    private String azureEndpoint;

    @Value("${azure.content-moderator.key:}")
    private String azureKey;

    @Value("${moderation.auto-approve-threshold:0.8}")
    private double autoApproveThreshold;

    @Value("${moderation.auto-reject-threshold:0.3}")
    private double autoRejectThreshold;

    private final ContentModerationRepository moderationRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    /**
     * Modère du contenu texte de manière asynchrone.
     */
    @Async
    public CompletableFuture<ModerationResult> moderateTextAsync(
            UUID userId, 
            UUID contentId, 
            ContentType contentType, 
            String text) {
        
        log.info("Starting async moderation for content: {}", contentId);
        
        try {
            ModerationResult result = moderateText(userId, contentId, contentType, text);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error in async moderation", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Modère du contenu texte.
     */
    @Transactional
    public ModerationResult moderateText(
            UUID userId, 
            UUID contentId, 
            ContentType contentType, 
            String text) {
        
        log.info("Moderating text content for user: {}", userId);

        // Créer l'entrée de modération
        ContentModeration moderation = ContentModeration.builder()
                .userId(userId)
                .contentId(contentId)
                .contentType(contentType)
                .contentText(text)
                .status(ModerationStatus.PENDING)
                .build();

        // Vérifier si Azure est configuré
        if (azureEndpoint == null || azureEndpoint.isEmpty()) {
            log.warn("Azure Content Moderator not configured, using basic moderation");
            return performBasicModeration(moderation, text);
        }

        try {
            // Appeler Azure Content Moderator
            JsonNode azureResponse = callAzureModerator(text);
            
            // Parser la réponse
            ModerationResult result = parseAzureResponse(azureResponse);
            
            // Mettre à jour l'entité
            moderation.setConfidenceScore(result.getConfidenceScore());
            moderation.setFlaggedCategories(serializeCategories(result.getFlaggedCategories()));
            
            // Déterminer le statut automatique
            if (result.getConfidenceScore() >= autoApproveThreshold) {
                moderation.setStatus(ModerationStatus.APPROVED);
                moderation.setActionTaken(ModerationAction.NONE);
            } else if (result.getConfidenceScore() <= autoRejectThreshold) {
                moderation.setStatus(ModerationStatus.REJECTED);
                moderation.setActionTaken(ModerationAction.CONTENT_DELETED);
            } else {
                moderation.setStatus(ModerationStatus.FLAGGED);
            }
            
            moderationRepository.save(moderation);
            
            log.info("Moderation completed: {} with score {}", 
                    moderation.getStatus(), result.getConfidenceScore());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error calling Azure moderator, falling back to basic", e);
            return performBasicModeration(moderation, text);
        }
    }

    /**
     * Modération basique sans Azure (fallback).
     */
    private ModerationResult performBasicModeration(ContentModeration moderation, String text) {
        log.info("Performing basic keyword-based moderation");

        Set<String> flaggedCategories = new HashSet<>();
        double confidenceScore = 1.0;

        // Liste de mots-clés interdits (à étendre)
        List<String> bannedKeywords = Arrays.asList(
                "violence", "hate", "illegal", "drug", "weapon",
                "suicide", "terrorism", "exploit", "abuse"
        );

        String lowerText = text.toLowerCase();
        
        for (String keyword : bannedKeywords) {
            if (lowerText.contains(keyword)) {
                flaggedCategories.add("inappropriate_content");
                confidenceScore -= 0.3;
            }
        }

        // Vérifier la longueur excessive de spam
        if (text.length() > 5000) {
            flaggedCategories.add("spam");
            confidenceScore -= 0.2;
        }

        // Vérifier répétitions suspectes
        if (hasExcessiveRepetition(text)) {
            flaggedCategories.add("spam");
            confidenceScore -= 0.2;
        }

        confidenceScore = Math.max(0.0, Math.min(1.0, confidenceScore));

        // Déterminer le statut
        if (confidenceScore >= autoApproveThreshold) {
            moderation.setStatus(ModerationStatus.APPROVED);
        } else if (confidenceScore <= autoRejectThreshold) {
            moderation.setStatus(ModerationStatus.REJECTED);
            moderation.setActionTaken(ModerationAction.CONTENT_DELETED);
        } else {
            moderation.setStatus(ModerationStatus.FLAGGED);
        }

        moderation.setConfidenceScore(confidenceScore);
        moderation.setFlaggedCategories(serializeCategories(flaggedCategories));
        moderationRepository.save(moderation);

        return ModerationResult.builder()
                .approved(moderation.getStatus() == ModerationStatus.APPROVED)
                .confidenceScore(confidenceScore)
                .flaggedCategories(flaggedCategories)
                .needsHumanReview(moderation.getStatus() == ModerationStatus.FLAGGED)
                .build();
    }

    /**
     * Appelle Azure Content Moderator API.
     */
    private JsonNode callAzureModerator(String text) {
        String url = azureEndpoint + "/contentmoderator/moderate/v1.0/ProcessText/Screen";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("Ocp-Apim-Subscription-Key", azureKey);

        HttpEntity<String> request = new HttpEntity<>(text, headers);

        try {
            String response = restTemplate.postForObject(url, request, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Error calling Azure Content Moderator", e);
            throw new RuntimeException("Moderation API error", e);
        }
    }

    /**
     * Parse la réponse d'Azure.
     */
    private ModerationResult parseAzureResponse(JsonNode response) {
        Set<String> flaggedCategories = new HashSet<>();
        double confidenceScore = 1.0;

        // Analyser les catégories flaggées
        if (response.has("Terms")) {
            JsonNode terms = response.get("Terms");
            if (terms.isArray() && terms.size() > 0) {
                flaggedCategories.add("inappropriate_language");
                confidenceScore -= 0.3;
            }
        }

        if (response.has("Classification")) {
            JsonNode classification = response.get("Classification");
            
            if (classification.has("ReviewRecommended") && 
                classification.get("ReviewRecommended").asBoolean()) {
                flaggedCategories.add("review_recommended");
                confidenceScore -= 0.2;
            }

            // Catégories spécifiques
            if (hasCategory(classification, "Category1")) {
                flaggedCategories.add("sexual_content");
                confidenceScore -= 0.4;
            }
            if (hasCategory(classification, "Category2")) {
                flaggedCategories.add("violent_content");
                confidenceScore -= 0.4;
            }
            if (hasCategory(classification, "Category3")) {
                flaggedCategories.add("offensive_content");
                confidenceScore -= 0.3;
            }
        }

        confidenceScore = Math.max(0.0, Math.min(1.0, confidenceScore));

        boolean needsReview = confidenceScore < autoApproveThreshold && 
                              confidenceScore > autoRejectThreshold;

        return ModerationResult.builder()
                .approved(confidenceScore >= autoApproveThreshold)
                .confidenceScore(confidenceScore)
                .flaggedCategories(flaggedCategories)
                .needsHumanReview(needsReview)
                .build();
    }

    /**
     * Vérifie si une catégorie est présente.
     */
    private boolean hasCategory(JsonNode classification, String category) {
        if (!classification.has(category)) return false;
        JsonNode cat = classification.get(category);
        return cat.has("Score") && cat.get("Score").asDouble() > 0.5;
    }

    /**
     * Revue manuelle d'un contenu.
     */
    @Transactional
    public void approveContent(UUID moderationId, UUID moderatorId, String notes) {
        log.info("Manually approving content: {}", moderationId);

        ContentModeration moderation = moderationRepository.findById(moderationId)
                .orElseThrow(() -> new ValidationException("MODERATION_NOT_FOUND", "Moderation not found"));

        moderation.approve(moderatorId, notes);
        moderationRepository.save(moderation);
    }

    /**
     * Rejet manuel d'un contenu.
     */
    @Transactional
    public void rejectContent(
            UUID moderationId, 
            UUID moderatorId, 
            String notes, 
            ModerationAction action) {
        
        log.info("Manually rejecting content: {} with action: {}", moderationId, action);

        ContentModeration moderation = moderationRepository.findById(moderationId)
                .orElseThrow(() -> new ValidationException("MODERATION_NOT_FOUND", "Moderation not found"));

        moderation.reject(moderatorId, notes, action);
        moderationRepository.save(moderation);

        // Appliquer l'action (ban, suspend, etc.)
        executeAction(moderation.getUserId(), action);
    }

    /**
     * Signalement d'un contenu par un utilisateur.
     */
    @Transactional
    public void reportContent(UUID userId, UUID contentId, ContentType contentType, String reason) {
        log.info("Content reported by user: {} for content: {}", userId, contentId);

        // Trouver ou créer la modération
        ContentModeration moderation = moderationRepository
                .findByContentIdAndContentType(contentId, contentType)
                .orElseGet(() -> {
                    ContentModeration newMod = ContentModeration.builder()
                            .userId(userId)
                            .contentId(contentId)
                            .contentType(contentType)
                            .status(ModerationStatus.FLAGGED)
                            .build();
                    return moderationRepository.save(newMod);
                });

        moderation.incrementReportCount();
        moderationRepository.save(moderation);

        // Si trop de signalements, action automatique
        if (moderation.getReportCount() >= 5) {
            moderation.setStatus(ModerationStatus.REJECTED);
            moderation.setActionTaken(ModerationAction.CONTENT_DELETED);
            moderationRepository.save(moderation);
            log.warn("Content auto-removed due to {} reports", moderation.getReportCount());
        }
    }

    /**
     * Exécute une action de modération.
     */
    private void executeAction(UUID userId, ModerationAction action) {
        switch (action) {
            case USER_WARNED:
                log.info("User {} warned", userId);
                // TODO: Envoyer email d'avertissement
                break;
            case USER_SUSPENDED:
                log.warn("User {} suspended", userId);
                // TODO: Suspendre le compte temporairement
                break;
            case USER_BANNED:
                log.error("User {} banned", userId);
                // TODO: Bannir le compte définitivement
                break;
            case REPORTED_AUTHORITY:
                log.error("Illegal content reported for user {}", userId);
                // TODO: Notifier les autorités
                break;
        }
    }

    /**
     * Vérifie les répétitions excessives.
     */
    private boolean hasExcessiveRepetition(String text) {
        String[] words = text.split("\\s+");
        if (words.length < 10) return false;

        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }

        // Si un mot représente plus de 30% du texte
        for (int count : wordCount.values()) {
            if ((double) count / words.length > 0.3) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sérialise les catégories en JSON.
     */
    private String serializeCategories(Set<String> categories) {
        try {
            return objectMapper.writeValueAsString(categories);
        } catch (Exception e) {
            return "[]";
        }
    }
}
