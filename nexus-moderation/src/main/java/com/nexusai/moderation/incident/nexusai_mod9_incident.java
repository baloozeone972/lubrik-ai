// =============================================================================
// FICHIER 1: EmotionAnalysisService.java
// =============================================================================
package com.nexusai.moderation.service.detection;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service d'analyse émotionnelle de texte.
 * 
 * Détecte et quantifie les émotions dans le texte :
 * - Joie, Tristesse, Colère, Peur, Surprise, Dégoût
 * - Intensité émotionnelle globale
 * - Sentiment général (positif/négatif/neutre)
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionAnalysisService {
    
    // Lexiques d'émotions (en production, utiliser un modèle ML)
    private static final Map<String, List<Pattern>> EMOTION_PATTERNS = initEmotionPatterns();
    
    /**
     * Analyse les émotions dans un texte.
     *
 * @param text Texte à analyser
     * @return Résultat d'analyse émotionnelle
     */
    public EmotionAnalysisResult analyzeEmotion(String text) {
        log.debug("Analyzing emotions in text");
        
        if (text == null || text.isBlank()) {
            return new EmotionAnalysisResult(
                Map.of(),
                0.0,
                "NEUTRAL",
                0.0
            );
        }
        
        String lowerText = text.toLowerCase();
        
        // Détecter chaque émotion
        Map<String, Double> emotions = new HashMap<>();
        
        for (Map.Entry<String, List<Pattern>> entry : EMOTION_PATTERNS.entrySet()) {
            String emotion = entry.getKey();
            List<Pattern> patterns = entry.getValue();
            
            int matchCount = 0;
            for (Pattern pattern : patterns) {
                if (pattern.matcher(lowerText).find()) {
                    matchCount++;
                }
            }
            
            // Score basé sur le nombre de matches
            double score = Math.min(100.0, matchCount * 20.0);
            if (score > 0) {
                emotions.put(emotion, score);
            }
        }
        
        // Calculer intensité globale
        double overallIntensity = emotions.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        // Déterminer sentiment dominant
        String dominantEmotion = emotions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("NEUTRAL");
        
        // Score de sentiment général (-1 à +1)
        double sentimentScore = calculateSentimentScore(emotions);
        
        return new EmotionAnalysisResult(
            emotions,
            overallIntensity,
            dominantEmotion,
            sentimentScore
        );
    }
    
    /**
     * Calcule un score de sentiment de -1 (très négatif) à +1 (très positif).
     */
    private double calculateSentimentScore(Map<String, Double> emotions) {
        double positive = emotions.getOrDefault("JOY", 0.0) 
                        + emotions.getOrDefault("SURPRISE", 0.0) * 0.5;
        
        double negative = emotions.getOrDefault("SADNESS", 0.0)
                        + emotions.getOrDefault("ANGER", 0.0)
                        + emotions.getOrDefault("FEAR", 0.0)
                        + emotions.getOrDefault("DISGUST", 0.0);
        
        double total = positive + negative;
        
        if (total == 0) {
            return 0.0; // Neutre
        }
        
        return (positive - negative) / total;
    }
    
    /**
     * Initialise les patterns d'émotions.
     */
    private static Map<String, List<Pattern>> initEmotionPatterns() {
        Map<String, List<Pattern>> patterns = new HashMap<>();
        
        // Joie
        patterns.put("JOY", Arrays.asList(
            Pattern.compile("\\b(heureux|joyeux|content|ravi|enchanté)\\b"),
            Pattern.compile("\\b(super|génial|excellent|magnifique)\\b"),
            Pattern.compile("(😊|😀|😃|😄|🎉|❤️)"),
            Pattern.compile("\\b(adore|j'aime|amour)\\b")
        ));
        
        // Tristesse
        patterns.put("SADNESS", Arrays.asList(
            Pattern.compile("\\b(triste|malheureux|déprimé|abattu)\\b"),
            Pattern.compile("\\b(pleure|larmes|chagrin)\\b"),
            Pattern.compile("(😢|😭|💔)"),
            Pattern.compile("\\b(seul|isolé|abandonné)\\b")
        ));
        
        // Colère
        patterns.put("ANGER", Arrays.asList(
            Pattern.compile("\\b(en colère|furieux|énervé|irrité)\\b"),
            Pattern.compile("\\b(rage|haine|déteste)\\b"),
            Pattern.compile("(😠|😡|🤬)"),
            Pattern.compile("\\b(insupportable|inacceptable)\\b")
        ));
        
        // Peur
        patterns.put("FEAR", Arrays.asList(
            Pattern.compile("\\b(peur|effrayé|terrifié|anxieux)\\b"),
            Pattern.compile("\\b(inquiet|stressé|angoissé)\\b"),
            Pattern.compile("(😨|😰|😱)"),
            Pattern.compile("\\b(danger|menace|risque)\\b")
        ));
        
        // Surprise
        patterns.put("SURPRISE", Arrays.asList(
            Pattern.compile("\\b(surpris|étonné|stupéfait)\\b"),
            Pattern.compile("\\b(wow|incroyable|impressionnant)\\b"),
            Pattern.compile("(😮|😲|🤯)")
        ));
        
        // Dégoût
        patterns.put("DISGUST", Arrays.asList(
            Pattern.compile("\\b(dégoûtant|répugnant|écœurant)\\b"),
            Pattern.compile("\\b(horrible|affreux|ignoble)\\b"),
            Pattern.compile("(🤢|🤮)")
        ));
        
        return patterns;
    }
    
    // DTO
    
    @Data
    public static class EmotionAnalysisResult {
        private final Map<String, Double> emotions;
        private final double overallIntensity;
        private final String dominantEmotion;
        private final double sentimentScore;
        
        public boolean isHighlyEmotional() {
            return overallIntensity > 60.0;
        }
        
        public boolean isPositive() {
            return sentimentScore > 0.3;
        }
        
        public boolean isNegative() {
            return sentimentScore < -0.3;
        }
        
        public boolean isNeutral() {
            return Math.abs(sentimentScore) <= 0.3;
        }
    }
}

// =============================================================================
// FICHIER 2: ContentClassificationService.java
// =============================================================================
package com.nexusai.moderation.service.detection;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service de classification de contenu.
 * 
 * Classifie le contenu selon différentes catégories :
 * - Type : News, Opinion, Fiction, Question, etc.
 * - Spam/Phishing
 * - Manipulation/Misinformation
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentClassificationService {
    
    private static final List<Pattern> SPAM_PATTERNS = initSpamPatterns();
    private static final List<Pattern> PHISHING_PATTERNS = initPhishingPatterns();
    
    /**
     * Classifie un contenu textuel.
     */
    public ContentClassification classifyContent(String content) {
        log.debug("Classifying content");
        
        if (content == null || content.isBlank()) {
            return ContentClassification.builder()
                .contentType("UNKNOWN")
                .isSpam(false)
                .isPhishing(false)
                .confidence(0.0)
                .build();
        }
        
        String lowerContent = content.toLowerCase();
        
        // Détection spam
        boolean isSpam = detectSpam(lowerContent);
        
        // Détection phishing
        boolean isPhishing = detectPhishing(lowerContent);
        
        // Classification type de contenu
        String contentType = classifyContentType(content);
        
        // Score de confiance
        double confidence = calculateConfidence(isSpam, isPhishing, contentType);
        
        return ContentClassification.builder()
            .contentType(contentType)
            .isSpam(isSpam)
            .isPhishing(isPhishing)
            .confidence(confidence)
            .metadata(extractMetadata(content))
            .build();
    }
    
    /**
     * Détecte si le contenu est du spam.
     */
    private boolean detectSpam(String content) {
        for (Pattern pattern : SPAM_PATTERNS) {
            if (pattern.matcher(content).find()) {
                log.debug("Spam pattern matched: {}", pattern.pattern());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Détecte si le contenu est du phishing.
     */
    private boolean detectPhishing(String content) {
        for (Pattern pattern : PHISHING_PATTERNS) {
            if (pattern.matcher(content).find()) {
                log.warn("Phishing pattern matched: {}", pattern.pattern());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Classifie le type de contenu.
     */
    private String classifyContentType(String content) {
        String lower = content.toLowerCase();
        
        // Question
        if (lower.contains("?") && 
            (lower.startsWith("qui") || lower.startsWith("quoi") || 
             lower.startsWith("où") || lower.startsWith("comment") || 
             lower.startsWith("pourquoi"))) {
            return "QUESTION";
        }
        
        // Opinion
        if (lower.contains("je pense") || lower.contains("selon moi") || 
            lower.contains("à mon avis")) {
            return "OPINION";
        }
        
        // News (indicateurs)
        if (lower.contains("aujourd'hui") || lower.contains("hier") ||
            lower.contains("selon") || lower.contains("annonce")) {
            return "NEWS";
        }
        
        // Fiction (indicateurs)
        if (lower.contains("il était une fois") || lower.contains("imaginons")) {
            return "FICTION";
        }
        
        return "GENERAL";
    }
    
    /**
     * Calcule la confiance de la classification.
     */
    private double calculateConfidence(boolean isSpam, boolean isPhishing, String type) {
        if (isSpam || isPhishing) {
            return 0.85; // Haute confiance pour spam/phishing détecté
        }
        
        return switch (type) {
            case "QUESTION" -> 0.9;
            case "OPINION", "NEWS" -> 0.7;
            case "FICTION" -> 0.6;
            default -> 0.5;
        };
    }
    
    /**
     * Extrait des métadonnées du contenu.
     */
    private Map<String, Object> extractMetadata(String content) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("length", content.length());
        metadata.put("wordCount", content.split("\\s+").length);
        metadata.put("hasUrls", content.matches(".*https?://.*"));
        metadata.put("hasEmojis", content.matches(".*[\\p{So}].*"));
        
        return metadata;
    }
    
    /**
     * Initialise les patterns de spam.
     */
    private static List<Pattern> initSpamPatterns() {
        return Arrays.asList(
            Pattern.compile("gagnez.*€|win.*\\$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("cliquez ici.*gratuit", Pattern.CASE_INSENSITIVE),
            Pattern.compile("offre limitée.*maintenant", Pattern.CASE_INSENSITIVE),
            Pattern.compile("felicitation.*vous avez gagné", Pattern.CASE_INSENSITIVE),
            Pattern.compile("viagra|cialis|pharmacy", Pattern.CASE_INSENSITIVE)
        );
    }
    
    /**
     * Initialise les patterns de phishing.
     */
    private static List<Pattern> initPhishingPatterns() {
        return Arrays.asList(
            Pattern.compile("vérifiez votre compte.*urgent", Pattern.CASE_INSENSITIVE),
            Pattern.compile("votre.*suspendu.*cliquez", Pattern.CASE_INSENSITIVE),
            Pattern.compile("confirm.*password|mot de passe", Pattern.CASE_INSENSITIVE),
            Pattern.compile("paypal.*verify|vérifier", Pattern.CASE_INSENSITIVE),
            Pattern.compile("banking.*urgent.*action", Pattern.CASE_INSENSITIVE)
        );
    }
    
    // DTO
    
    @Data
    @lombok.Builder
    public static class ContentClassification {
        private String contentType;
        private boolean isSpam;
        private boolean isPhishing;
        private double confidence;
        private Map<String, Object> metadata;
        
        public boolean isSuspicious() {
            return isSpam || isPhishing;
        }
    }
}

// =============================================================================
// FICHIER 3: IncidentManagementService.java
// =============================================================================
package com.nexusai.moderation.service.incident;

import com.nexusai.moderation.model.entity.ModerationIncident;
import com.nexusai.moderation.model.enums.Severity;
import com.nexusai.moderation.repository.ModerationIncidentRepository;
import com.nexusai.moderation.event.ModerationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de gestion des incidents de modération.
 * 
 * Responsabilités :
 * - CRUD incidents
 * - Workflow de review (PENDING → IN_REVIEW → RESOLVED)
 * - Escalade automatique
 * - Statistiques
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentManagementService {
    
    private final ModerationIncidentRepository incidentRepository;
    private final EscalationService escalationService;
    private final ModerationEventPublisher eventPublisher;
    
    /**
     * Récupère un incident par ID.
     */
    public Optional<ModerationIncident> getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId);
    }
    
    /**
     * Liste les incidents avec pagination.
     */
    public Page<ModerationIncident> getIncidents(
            String status,
            Severity severity,
            Pageable pageable) {
        
        if (status != null && severity != null) {
            // Filtrer par statut ET sévérité
            return incidentRepository.findByStatusAndSeverity(
                status, severity, pageable);
        } else if (status != null) {
            return incidentRepository.findByStatus(status, pageable);
        } else if (severity != null) {
            return incidentRepository.findBySeverity(severity, pageable);
        }
        
        return incidentRepository.findAll(pageable);
    }
    
    /**
     * Récupère les incidents d'un utilisateur.
     */
    public List<ModerationIncident> getUserIncidents(UUID userId) {
        return incidentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Met à jour le statut d'un incident.
     */
    @Transactional
    public ModerationIncident updateIncidentStatus(
            UUID incidentId,
            String newStatus,
            UUID reviewerId,
            String notes) {
        
        ModerationIncident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentNotFoundException(incidentId));
        
        // Mettre à jour
        incident.setStatus(newStatus);
        incident.setReviewedBy(reviewerId);
        incident.setReviewedAt(LocalDateTime.now());
        
        if (notes != null) {
            incident.setNotes(
                (incident.getNotes() != null ? incident.getNotes() + "\n" : "") + notes
            );
        }
        
        ModerationIncident saved = incidentRepository.save(incident);
        
        log.info("Incident {} status updated to: {}", incidentId, newStatus);
        
        // Événement
        eventPublisher.publishIncidentReviewed(saved);
        
        return saved;
    }
    
    /**
     * Escalade un incident si nécessaire.
     */
    @Transactional
    public boolean escalateIfNeeded(UUID incidentId) {
        ModerationIncident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentNotFoundException(incidentId));
        
        if (escalationService.shouldEscalate(incident)) {
            escalationService.escalate(incident);
            
            incident.setStatus("ESCALATED");
            incidentRepository.save(incident);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Statistiques des incidents.
     */
    public IncidentStatistics getStatistics(LocalDateTime since) {
        long totalIncidents = incidentRepository.countByCreatedAtAfter(since);
        long criticalIncidents = incidentRepository
            .countByCreatedAtAfterAndSeverity(since, Severity.CRITICAL);
        long pendingIncidents = incidentRepository
            .countByCreatedAtAfterAndStatus(since, "PENDING");
        
        // Incidents par type
        List<Object[]> incidentsByType = incidentRepository
            .getIncidentStatistics(since);
        
        Map<String, Long> typeBreakdown = new HashMap<>();
        for (Object[] row : incidentsByType) {
            typeBreakdown.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        
        return new IncidentStatistics(
            totalIncidents,
            criticalIncidents,
            pendingIncidents,
            typeBreakdown
        );
    }
    
    /**
     * Supprime les anciens incidents (> 3 ans).
     */
    @Transactional
    public int cleanupOldIncidents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(3);
        
        List<ModerationIncident> oldIncidents = incidentRepository
            .findByCreatedAtBeforeAndStatusNot(cutoffDate, "ESCALATED");
        
        incidentRepository.deleteAll(oldIncidents);
        
        log.info("Cleaned up {} old incidents", oldIncidents.size());
        
        return oldIncidents.size();
    }
    
    // DTOs
    
    public record IncidentStatistics(
        long totalIncidents,
        long criticalIncidents,
        long pendingIncidents,
        Map<String, Long> incidentsByType
    ) {}
    
    // Exceptions
    
    public static class IncidentNotFoundException extends RuntimeException {
        public IncidentNotFoundException(UUID incidentId) {
            super("Incident not found: " + incidentId);
        }
    }
}

// =============================================================================
// FICHIER 4: WarningService.java
// =============================================================================
package com.nexusai.moderation.service.incident;

import com.nexusai.moderation.model.entity.UserWarning;
import com.nexusai.moderation.repository.UserWarningRepository;
import com.nexusai.moderation.event.ModerationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de gestion des avertissements utilisateurs.
 * 
 * Système de points :
 * - 1 avertissement LOW = 1 point
 * - 1 avertissement MEDIUM = 2 points
 * - 1 avertissement HIGH = 3 points
 * - 1 avertissement CRITICAL = 5 points
 * 
 * Seuils :
 * - 5 points = Ban temporaire 24h
 * - 10 points = Ban temporaire 7 jours
 * - 15+ points = Ban permanent
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WarningService {
    
    private final UserWarningRepository warningRepository;
    private final ModerationEventPublisher eventPublisher;
    
    /**
     * Émet un avertissement à un utilisateur.
     */
    @Transactional
    public UserWarning issueWarning(
            UUID userId,
            UUID incidentId,
            String warningType,
            String description) {
        
        log.info("Issuing warning to user: {}, type: {}", userId, warningType);
        
        UserWarning warning = UserWarning.builder()
            .userId(userId)
            .incidentId(incidentId)
            .warningType(warningType)
            .description(description)
            .acknowledged(false)
            .expiresAt(LocalDateTime.now().plusDays(30)) // 30 jours
            .build();
        
        UserWarning saved = warningRepository.save(warning);
        
        // Vérifier si ban nécessaire
        checkAndApplyBanIfNeeded(userId);
        
        // Événement
        eventPublisher.publishUserWarned(saved);
        
        return saved;
    }
    
    /**
     * Récupère les avertissements actifs d'un utilisateur.
     */
    public List<UserWarning> getUserActiveWarnings(UUID userId) {
        return warningRepository.findByUserIdAndExpiresAtAfter(
            userId, 
            LocalDateTime.now()
        );
    }
    
    /**
     * Accuse réception d'un avertissement.
     */
    @Transactional
    public UserWarning acknowledgeWarning(UUID warningId) {
        UserWarning warning = warningRepository.findById(warningId)
            .orElseThrow(() -> new WarningNotFoundException(warningId));
        
        warning.setAcknowledged(true);
        warning.setAcknowledgedAt(LocalDateTime.now());
        
        return warningRepository.save(warning);
    }
    
    /**
     * Calcule le score de points d'un utilisateur.
     */
    public int calculateUserWarningPoints(UUID userId) {
        List<UserWarning> activeWarnings = getUserActiveWarnings(userId);
        
        int totalPoints = 0;
        
        for (UserWarning warning : activeWarnings) {
            totalPoints += getPointsForWarningType(warning.getWarningType());
        }
        
        return totalPoints;
    }
    
    /**
     * Vérifie et applique un ban si le seuil est atteint.
     */
    private void checkAndApplyBanIfNeeded(UUID userId) {
        int points = calculateUserWarningPoints(userId);
        
        log.info("User {} has {} warning points", userId, points);
        
        if (points >= 15) {
            // Ban permanent
            eventPublisher.publishUserBanned(
                userId, 
                "PERMANENT", 
                "15+ warning points"
            );
            log.warn("User {} PERMANENTLY BANNED (15+ points)", userId);
            
        } else if (points >= 10) {
            // Ban 7 jours
            eventPublisher.publishUserBanned(
                userId, 
                "TEMPORARY_7D", 
                "10+ warning points"
            );
            log.warn("User {} banned for 7 days (10+ points)", userId);
            
        } else if (points >= 5) {
            // Ban 24h
            eventPublisher.publishUserBanned(
                userId, 
                "TEMPORARY_24H", 
                "5+ warning points"
            );
            log.warn("User {} banned for 24 hours (5+ points)", userId);
        }
    }
    
    /**
     * Retourne les points pour un type d'avertissement.
     */
    private int getPointsForWarningType(String warningType) {
        return switch (warningType.toUpperCase()) {
            case "CRITICAL" -> 5;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 1;
        };
    }
    
    /**
     * Nettoie les avertissements expirés.
     */
    @Transactional
    public int cleanupExpiredWarnings() {
        List<UserWarning> expired = warningRepository
            .findByExpiresAtBefore(LocalDateTime.now());
        
        warningRepository.deleteAll(expired);
        
        log.info("Cleaned up {} expired warnings", expired.size());
        
        return expired.size();
    }
    
    // Exceptions
    
    public static class WarningNotFoundException extends RuntimeException {
        public WarningNotFoundException(UUID warningId) {
            super("Warning not found: " + warningId);
        }
    }
}

// =============================================================================
// FICHIER 5: EscalationService.java
// =============================================================================
package com.nexusai.moderation.service.incident;

import com.nexusai.moderation.model.entity.ModerationIncident;
import com.nexusai.moderation.model.enums.*;
import com.nexusai.moderation.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service d'escalade des incidents critiques.
 * 
 * Escalade automatique vers :
 * - Modérateurs humains (incidents CRITICAL non reviewés > 1h)
 * - Équipe légale (CSAM, terrorisme)
 * - Management (incidents répétés)
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationService {
    
    private final NotificationService notificationService;
    
    /**
     * Détermine si un incident doit être escaladé.
     */
    public boolean shouldEscalate(ModerationIncident incident) {
        // 1. CSAM ou terrorisme → Escalade immédiate
        if (incident.getIncidentType() == IncidentType.SEXUAL_MINORS ||
            incident.getIncidentType() == IncidentType.TERRORISM) {
            return true;
        }
        
        // 2. Incident CRITICAL non reviewé depuis > 1h
        if (incident.getSeverity() == Severity.CRITICAL &&
            "PENDING".equals(incident.getStatus())) {
            
            Duration timeSinceCreation = Duration.between(
                incident.getCreatedAt(),
                LocalDateTime.now()
            );
            
            if (timeSinceCreation.toHours() >= 1) {
                return true;
            }
        }
        
        // 3. Incident HIGH non reviewé depuis > 24h
        if (incident.getSeverity() == Severity.HIGH &&
            "PENDING".equals(incident.getStatus())) {
            
            Duration timeSinceCreation = Duration.between(
                incident.getCreatedAt(),
                LocalDateTime.now()
            );
            
            if (timeSinceCreation.toHours() >= 24) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Escalade un incident.
     */
    public void escalate(ModerationIncident incident) {
        log.warn("Escalating incident: {}", incident.getId());
        
        EscalationLevel level = determineEscalationLevel(incident);
        
        switch (level) {
            case LEGAL:
                escalateToLegal(incident);
                break;
                
            case MODERATORS:
                escalateToModerators(incident);
                break;
                
            case MANAGEMENT:
                escalateToManagement(incident);
                break;
        }
    }
    
    /**
     * Escalade vers l'équipe légale.
     * 
     * Utilisé pour CSAM, terrorisme, etc.
     */
    private void escalateToLegal(ModerationIncident incident) {
        log.error("LEGAL ESCALATION for incident: {}", incident.getId());
        
        String message = String.format(
            "🚨 LEGAL ESCALATION REQUIRED\n\n" +
            "Incident ID: %s\n" +
            "Type: %s\n" +
            "Severity: %s\n" +
            "User ID: %s\n" +
            "Confidence: %.2f\n\n" +
            "Immediate action required. Law enforcement may need to be notified.",
            incident.getId(),
            incident.getIncidentType(),
            incident.getSeverity(),
            incident.getUserId(),
            incident.getConfidence()
        );
        
        // Notifier équipe légale
        notificationService.alertLegalTeam(message);
        
        // Notifier management
        notificationService.alertManagement(message);
    }
    
    /**
     * Escalade vers les modérateurs.
     */
    private void escalateToModerators(ModerationIncident incident) {
        log.warn("MODERATOR ESCALATION for incident: {}", incident.getId());
        
        String message = String.format(
            "⚠️  Incident requires human review\n\n" +
            "Incident ID: %s\n" +
            "Type: %s\n" +
            "Severity: %s\n" +
            "Time since creation: %s\n\n" +
            "Please review and take appropriate action.",
            incident.getId(),
            incident.getIncidentType(),
            incident.getSeverity(),
            formatDuration(incident.getCreatedAt())
        );
        
        notificationService.alertModerators(message);
    }
    
    /**
     * Escalade vers le management.
     */
    private void escalateToManagement(ModerationIncident incident) {
        log.warn("MANAGEMENT ESCALATION for incident: {}", incident.getId());
        
        String message = String.format(
            "📊 High-priority incident requires management attention\n\n" +
            "Incident ID: %s\n" +
            "Type: %s\n" +
            "Details available in admin dashboard.",
            incident.getId(),
            incident.getIncidentType()
        );
        
        notificationService.alertManagement(message);
    }
    
    /**
     * Détermine le niveau d'escalade nécessaire.
     */
    private EscalationLevel determineEscalationLevel(ModerationIncident incident) {
        // Contenu illégal → Légal
        if (incident.getIncidentType() == IncidentType.SEXUAL_MINORS ||
            incident.getIncidentType() == IncidentType.TERRORISM) {
            return EscalationLevel.LEGAL;
        }
        
        // CRITICAL → Modérateurs
        if (incident.getSeverity() == Severity.CRITICAL) {
            return EscalationLevel.MODERATORS;
        }
        
        // Autres cas → Management
        return EscalationLevel.MANAGEMENT;
    }
    
    /**
     * Formate une durée depuis une date.
     */
    private String formatDuration(LocalDateTime since) {
        Duration duration = Duration.between(since, LocalDateTime.now());
        
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        
        return String.format("%dh %dm", hours, minutes);
    }
    
    /**
     * Niveaux d'escalade.
     */
    private enum EscalationLevel {
        MODERATORS,
        LEGAL,
        MANAGEMENT
    }
}

// =============================================================================
// FIN - Detection & Incident Management
// =============================================================================
