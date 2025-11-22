// =============================================================================
// FICHIER 1: ModerationLevel.java
// =============================================================================
package com.nexusai.moderation.model.enums;

/**
 * Niveaux de modération applicables selon le plan d'abonnement.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
public enum ModerationLevel {
    /**
     * Modération stricte (FREE/STANDARD) - Bloque tout contenu sensible
     */
    STRICT,
    
    /**
     * Modération légère (PREMIUM) - Permet certains contenus adultes
     */
    LIGHT,
    
    /**
     * Modération optionnelle (VIP+ avec KYC Level 3) - Minimal filtering
     */
    OPTIONAL;
    
    /**
     * Détermine si ce niveau permet du contenu adulte explicite
     */
    public boolean allowsExplicitContent() {
        return this == OPTIONAL;
    }
    
    /**
     * Détermine si ce niveau nécessite un consentement explicite
     */
    public boolean requiresConsent() {
        return this == OPTIONAL;
    }
}

// =============================================================================
// FICHIER 2: IncidentType.java
// =============================================================================
package com.nexusai.moderation.model.enums;

/**
 * Types d'incidents de modération détectables.
 */
public enum IncidentType {
    SEXUAL_CONTENT,
    SEXUAL_MINORS,      // TOUJOURS bloqué - illégal
    VIOLENCE,
    GRAPHIC_VIOLENCE,
    HATE_SPEECH,
    HARASSMENT,
    SELF_HARM,
    TERRORISM,          // TOUJOURS bloqué - illégal
    SPAM,
    ILLEGAL_ACTIVITY,
    DISTRESS_DETECTED,  // Détresse psychologique
    OTHER
}

// =============================================================================
// FICHIER 3: Severity.java
// =============================================================================
package com.nexusai.moderation.model.enums;

/**
 * Niveau de sévérité d'un incident de modération.
 */
public enum Severity {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);  // Contenu illégal, escalade immédiate
    
    private final int level;
    
    Severity(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isCritical() {
        return this == CRITICAL;
    }
}

// =============================================================================
// FICHIER 4: ModerationIncident.java (Entity)
// =============================================================================
package com.nexusai.moderation.model.entity;

import com.nexusai.moderation.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entité représentant un incident de modération détecté.
 * 
 * Un incident est créé quand du contenu inapproprié est détecté,
 * qu'il soit bloqué ou simplement signalé.
 * 
 * @author NexusAI Team
 */
@Entity
@Table(name = "moderation_incidents", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_severity", columnList = "severity"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationIncident {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * ID de l'utilisateur concerné par l'incident
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    /**
     * Type de contenu modéré (TEXT, IMAGE, VIDEO)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 50)
    private ContentType contentType;
    
    /**
     * Hash SHA-256 du contenu (pas le contenu lui-même pour privacy)
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;
    
    /**
     * ID de la conversation (si applicable)
     */
    @Column(name = "conversation_id")
    private String conversationId;
    
    /**
     * ID du message (si applicable)
     */
    @Column(name = "message_id")
    private String messageId;
    
    /**
     * Type d'incident détecté
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 50)
    private IncidentType incidentType;
    
    /**
     * Niveau de sévérité
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;
    
    /**
     * Score de confiance du modèle ML (0.0 - 1.0)
     */
    @Column(name = "confidence")
    private Double confidence;
    
    /**
     * Scores détaillés de modération (JSON)
     * Exemple: {"sexual": 0.85, "violence": 0.12, "hate": 0.03}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "moderation_scores", columnDefinition = "jsonb")
    private Map<String, Double> moderationScores;
    
    /**
     * Statut de l'incident (PENDING, REVIEWED, DISMISSED, ESCALATED)
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";
    
    /**
     * Incident traité automatiquement par IA (true) ou par modérateur humain (false)
     */
    @Column(name = "automated")
    private Boolean automated = true;
    
    /**
     * ID du modérateur qui a reviewé l'incident
     */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    
    /**
     * Date de review par un modérateur
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    /**
     * Action prise suite à l'incident (BLOCKED, WARNING_ISSUED, USER_BANNED, etc.)
     */
    @Column(name = "action_taken", length = 100)
    private String actionTaken;
    
    /**
     * Notes du modérateur
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Date de création de l'incident
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

// =============================================================================
// FICHIER 5: ModerationRule.java (Entity)
// =============================================================================
package com.nexusai.moderation.model.entity;

import com.nexusai.moderation.model.enums.ModerationLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Règle de modération définissant les seuils et actions selon le niveau.
 * 
 * Permet de configurer dynamiquement le comportement de modération
 * sans recompiler l'application.
 * 
 * @author NexusAI Team
 */
@Entity
@Table(name = "moderation_rules", indexes = {
    @Index(name = "idx_level_category", columnList = "moderation_level, content_category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Niveau de modération (STRICT, LIGHT, OPTIONAL)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_level", nullable = false, length = 20)
    private ModerationLevel moderationLevel;
    
    /**
     * Catégorie de contenu concernée
     * Exemples: "sexual", "violence", "hate", "self-harm"
     */
    @Column(name = "content_category", nullable = false, length = 50)
    private String contentCategory;
    
    /**
     * Seuil de déclenchement (0.0 - 1.0)
     * Si le score ML > threshold, la règle s'applique
     */
    @Column(name = "threshold", nullable = false)
    private Double threshold;
    
    /**
     * Action à prendre: BLOCK, WARN, ALLOW
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    /**
     * Règle active (permet de désactiver temporairement)
     */
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// =============================================================================
// FICHIER 6: ModerationResponse.java (DTO)
// =============================================================================
package com.nexusai.moderation.model.dto;

import com.nexusai.moderation.model.enums.*;
import lombok.*;

import java.util.Map;

/**
 * Réponse du système de modération.
 * 
 * Indique si le contenu est accepté, bloqué ou nécessite un avertissement.
 * 
 * @author NexusAI Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationResponse {
    
    /**
     * Contenu autorisé (true) ou bloqué (false)
     */
    private boolean allowed;
    
    /**
     * Type d'incident détecté (null si aucun)
     */
    private IncidentType incidentType;
    
    /**
     * Niveau de sévérité (null si aucun incident)
     */
    private Severity severity;
    
    /**
     * Score de confiance (0.0 - 1.0)
     */
    private Double confidence;
    
    /**
     * Scores détaillés par catégorie
     */
    private Map<String, Double> detailedScores;
    
    /**
     * Message explicatif pour l'utilisateur
     */
    private String message;
    
    /**
     * ID de l'incident créé (si applicable)
     */
    private String incidentId;
    
    /**
     * Avertissement émis (true/false)
     */
    private boolean warningIssued;
    
    /**
     * Factory method pour contenu autorisé
     */
    public static ModerationResponse allowed() {
        return ModerationResponse.builder()
            .allowed(true)
            .message("Content approved")
            .build();
    }
    
    /**
     * Factory method pour contenu bloqué
     */
    public static ModerationResponse blocked(IncidentType type, Severity severity, String message) {
        return ModerationResponse.builder()
            .allowed(false)
            .incidentType(type)
            .severity(severity)
            .message(message)
            .build();
    }
}

// =============================================================================
// FICHIER 7: ModerationService.java (Interface)
// =============================================================================
package com.nexusai.moderation.service.moderation;

import com.nexusai.moderation.model.dto.ModerationResponse;

/**
 * Service principal de modération.
 * 
 * Interface unifiée pour modérer tout type de contenu.
 * Les implémentations spécifiques gèrent texte, images et vidéos.
 * 
 * @author NexusAI Team
 */
public interface ModerationService {
    
    /**
     * Modère du contenu textuel.
     * 
     * @param content Le texte à modérer
     * @param userId ID de l'utilisateur
     * @param conversationId ID de la conversation (optionnel)
     * @return Résultat de la modération
     */
    ModerationResponse moderateText(String content, String userId, String conversationId);
    
    /**
     * Modère une image.
     * 
     * @param imageUrl URL de l'image à modérer
     * @param userId ID de l'utilisateur
     * @return Résultat de la modération
     */
    ModerationResponse moderateImage(String imageUrl, String userId);
    
    /**
     * Modère une vidéo.
     * 
     * @param videoUrl URL de la vidéo à modérer
     * @param userId ID de l'utilisateur
     * @return Résultat de la modération
     */
    ModerationResponse moderateVideo(String videoUrl, String userId);
}

// =============================================================================
// FICHIER 8: TextModerationService.java (Implémentation)
// =============================================================================
package com.nexusai.moderation.service.moderation;

import com.nexusai.moderation.model.dto.ModerationResponse;
import com.nexusai.moderation.model.entity.ModerationIncident;
import com.nexusai.moderation.model.entity.ModerationRule;
import com.nexusai.moderation.model.enums.*;
import com.nexusai.moderation.repository.ModerationIncidentRepository;
import com.nexusai.moderation.service.client.OpenAIModerationClient;
import com.nexusai.moderation.service.client.UserServiceClient;
import com.nexusai.moderation.service.detection.DistressDetectionService;
import com.nexusai.moderation.util.ContentHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service de modération pour contenu textuel.
 * 
 * Workflow:
 * 1. Déterminer niveau de modération utilisateur
 * 2. Pré-filtrage avec blacklist
 * 3. Analyse IA (OpenAI Moderation)
 * 4. Application des règles
 * 5. Détection de détresse (toujours actif)
 * 6. Création incident si nécessaire
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TextModerationService {
    
    private final OpenAIModerationClient openAIClient;
    private final UserServiceClient userServiceClient;
    private final ModerationRulesService rulesService;
    private final DistressDetectionService distressDetectionService;
    private final ModerationIncidentRepository incidentRepository;
    private final BlacklistService blacklistService;
    
    /**
     * Modère un texte en appliquant toutes les règles.
     * 
     * @param content Le contenu textuel à modérer
     * @param userId ID de l'utilisateur (UUID en String)
     * @param conversationId ID de la conversation (peut être null)
     * @return Résultat de modération
     */
    @Transactional
    public ModerationResponse moderateText(String content, String userId, String conversationId) {
        log.info("Moderating text for user: {}, conversation: {}", userId, conversationId);
        
        // 1. Déterminer niveau de modération
        ModerationLevel level = getUserModerationLevel(userId);
        log.debug("User moderation level: {}", level);
        
        // 2. Pré-filtrage blacklist (rapide)
        if (blacklistService.containsBlacklistedTerms(content)) {
            log.warn("Blacklisted term detected for user: {}", userId);
            return handleBlacklistViolation(content, userId, conversationId);
        }
        
        // 3. Analyse IA
        Map<String, Double> aiScores = openAIClient.moderate(content);
        log.debug("AI moderation scores: {}", aiScores);
        
        // 4. Appliquer règles selon niveau
        ModerationDecision decision = applyRules(level, aiScores, content);
        
        // 5. Détection détresse (TOUJOURS actif, même pour VIP+)
        if (distressDetectionService.detectDistress(content, aiScores)) {
            log.warn("Distress detected for user: {}", userId);
            distressDetectionService.handleDistress(userId, conversationId);
            // Continue la modération normale en parallèle
        }
        
        // 6. Si bloqué, créer incident
        if (decision.isBlocked()) {
            ModerationIncident incident = createIncident(
                userId, 
                conversationId, 
                content, 
                decision,
                aiScores
            );
            
            return ModerationResponse.blocked(
                decision.getIncidentType(),
                decision.getSeverity(),
                decision.getMessage()
            ).toBuilder()
                .incidentId(incident.getId().toString())
                .detailedScores(aiScores)
                .confidence(decision.getConfidence())
                .build();
        }
        
        // 7. Si warning
        if (decision.isWarning()) {
            return ModerationResponse.builder()
                .allowed(true)
                .warningIssued(true)
                .message(decision.getMessage())
                .detailedScores(aiScores)
                .build();
        }
        
        // 8. Contenu autorisé
        return ModerationResponse.allowed();
    }
    
    /**
     * Détermine le niveau de modération applicable à l'utilisateur.
     * 
     * Logique:
     * - FREE/STANDARD → STRICT (forcé)
     * - PREMIUM → STRICT ou LIGHT (choix utilisateur)
     * - VIP+ → OPTIONAL si KYC Level 3 + consentement actif
     * 
     * @param userId ID utilisateur
     * @return Niveau de modération
     */
    private ModerationLevel getUserModerationLevel(String userId) {
        // Appel au User Service pour récupérer plan et préférences
        var userInfo = userServiceClient.getUserInfo(userId);
        var subscription = userInfo.getSubscription();
        
        // FREE/STANDARD → STRICT (non négociable)
        if (subscription.isFreeOrStandard()) {
            return ModerationLevel.STRICT;
        }
        
        // PREMIUM → Choix utilisateur entre STRICT et LIGHT
        if (subscription.isPremium()) {
            return userInfo.getPreferences().getModerationLevel();
        }
        
        // VIP+ → Vérifier KYC + consentement
        if (subscription.isVipPlus()) {
            boolean hasKyc = userServiceClient.hasValidKYC(userId, 3);
            boolean hasConsent = userServiceClient.hasActiveConsent(userId, "UNMODERATED_MODE");
            
            if (hasKyc && hasConsent) {
                return ModerationLevel.OPTIONAL;
            }
        }
        
        // Par défaut: LIGHT
        return ModerationLevel.LIGHT;
    }
    
    /**
     * Applique les règles de modération selon le niveau et les scores IA.
     * 
     * @param level Niveau de modération
     * @param aiScores Scores d'analyse IA
     * @param content Contenu original (pour analyse complémentaire)
     * @return Décision de modération
     */
    private ModerationDecision applyRules(
            ModerationLevel level, 
            Map<String, Double> aiScores,
            String content) {
        
        // Règles CRITIQUES: TOUJOURS appliquées, quel que soit le niveau
        
        // 1. Contenu pédopornographique (CSAM) - ZÉRO TOLÉRANCE
        Double csam Score = aiScores.getOrDefault("sexual/minors", 0.0);
        if (csamScore > 0.01) {  // Seuil ultra-bas
            log.error("CRITICAL: CSAM content detected! Score: {}", csamScore);
            return ModerationDecision.block(
                IncidentType.SEXUAL_MINORS,
                Severity.CRITICAL,
                "Contenu illégal détecté. Votre compte a été signalé aux autorités.",
                csamScore
            );
        }
        
        // 2. Terrorisme - ZÉRO TOLÉRANCE
        Double terrorismScore = aiScores.getOrDefault("terrorism", 0.0);
        if (terrorismScore > 0.1) {
            log.error("CRITICAL: Terrorism content detected! Score: {}", terrorismScore);
            return ModerationDecision.block(
                IncidentType.TERRORISM,
                Severity.CRITICAL,
                "Contenu illégal détecté.",
                terrorismScore
            );
        }
        
        // 3. Appliquer règles configurables selon niveau
        List<ModerationRule> rules = rulesService.getRulesForLevel(level);
        
        for (ModerationRule rule : rules) {
            String category = rule.getContentCategory();
            Double score = aiScores.getOrDefault(category, 0.0);
            
            if (score > rule.getThreshold()) {
                log.debug("Rule triggered: {} (score: {} > threshold: {})", 
                    category, score, rule.getThreshold());
                
                return switch (rule.getAction()) {
                    case "BLOCK" -> ModerationDecision.block(
                        mapCategoryToIncidentType(category),
                        determineSeverity(score),
                        "Contenu inapproprié détecté: " + category,
                        score
                    );
                    case "WARN" -> ModerationDecision.warn(
                        category,
                        "Attention: ce contenu pourrait enfreindre nos règles",
                        score
                    );
                    default -> null; // ALLOW, continuer les vérifications
                };
            }
        }
        
        // Aucune règle déclenchée → Contenu OK
        return ModerationDecision.allow();
    }
    
    /**
     * Crée un incident de modération dans la base de données.
     */
    private ModerationIncident createIncident(
            String userId,
            String conversationId,
            String content,
            ModerationDecision decision,
            Map<String, Double> aiScores) {
        
        ModerationIncident incident = ModerationIncident.builder()
            .userId(UUID.fromString(userId))
            .contentType(ContentType.TEXT)
            .contentHash(ContentHashUtil.sha256(content))
            .conversationId(conversationId)
            .incidentType(decision.getIncidentType())
            .severity(decision.getSeverity())
            .confidence(decision.getConfidence())
            .moderationScores(aiScores)
            .status("PENDING")
            .automated(true)
            .actionTaken("BLOCKED")
            .build();
        
        return incidentRepository.save(incident);
    }
    
    /**
     * Gère une violation de la blacklist.
     */
    private ModerationResponse handleBlacklistViolation(
            String content, 
            String userId, 
            String conversationId) {
        
        ModerationIncident incident = ModerationIncident.builder()
            .userId(UUID.fromString(userId))
            .contentType(ContentType.TEXT)
            .contentHash(ContentHashUtil.sha256(content))
            .conversationId(conversationId)
            .incidentType(IncidentType.HATE_SPEECH)
            .severity(Severity.HIGH)
            .confidence(1.0)
            .status("PENDING")
            .automated(true)
            .actionTaken("BLOCKED")
            .build();
        
        incidentRepository.save(incident);
        
        return ModerationResponse.blocked(
            IncidentType.HATE_SPEECH,
            Severity.HIGH,
            "Terme interdit détecté dans votre message."
        );
    }
    
    /**
     * Mappe une catégorie de modération vers un IncidentType.
     */
    private IncidentType mapCategoryToIncidentType(String category) {
        return switch (category.toLowerCase()) {
            case "sexual", "sexual/explicit" -> IncidentType.SEXUAL_CONTENT;
            case "sexual/minors" -> IncidentType.SEXUAL_MINORS;
            case "violence", "violence/graphic" -> IncidentType.VIOLENCE;
            case "hate" -> IncidentType.HATE_SPEECH;
            case "self-harm" -> IncidentType.SELF_HARM;
            case "terrorism" -> IncidentType.TERRORISM;
            default -> IncidentType.OTHER;
        };
    }
    
    /**
     * Détermine la sévérité en fonction du score.
     */
    private Severity determineSeverity(double score) {
        if (score >= 0.9) return Severity.CRITICAL;
        if (score >= 0.7) return Severity.HIGH;
        if (score >= 0.4) return Severity.MEDIUM;
        return Severity.LOW;
    }
}

// =============================================================================
// FICHIER 9: ModerationDecision.java (Helper Class)
// =============================================================================
package com.nexusai.moderation.service.moderation;

import com.nexusai.moderation.model.enums.*;
import lombok.*;

/**
 * Classe interne représentant une décision de modération.
 * 
 * @author NexusAI Team
 */
@Data
@Builder
public class ModerationDecision {
    private boolean blocked;
    private boolean warning;
    private IncidentType incidentType;
    private Severity severity;
    private String message;
    private Double confidence;
    
    public static ModerationDecision allow() {
        return ModerationDecision.builder()
            .blocked(false)
            .warning(false)
            .build();
    }
    
    public static ModerationDecision block(
            IncidentType type, 
            Severity severity, 
            String message,
            Double confidence) {
        return ModerationDecision.builder()
            .blocked(true)
            .incidentType(type)
            .severity(severity)
            .message(message)
            .confidence(confidence)
            .build();
    }
    
    public static ModerationDecision warn(
            String category, 
            String message,
            Double confidence) {
        return ModerationDecision.builder()
            .blocked(false)
            .warning(true)
            .message(message)
            .confidence(confidence)
            .build();
    }
}

// =============================================================================
// FICHIER 10: DistressDetectionService.java
// =============================================================================
package com.nexusai.moderation.service.detection;

import com.nexusai.moderation.event.ModerationEventPublisher;
import com.nexusai.moderation.event.events.DistressDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service de détection de détresse psychologique.
 * 
 * Détecte les signes de:
 * - Pensées suicidaires
 * - Auto-mutilation
 * - Détresse émotionnelle sévère
 * 
 * IMPORTANT: Toujours actif, même pour utilisateurs VIP+ avec modération minimale.
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistressDetectionService {
    
    private final ModerationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    
    // Patterns de détection (en production, utiliser un modèle ML)
    private static final List<Pattern> SUICIDAL_PATTERNS = Arrays.asList(
        Pattern.compile("\\b(je veux mourir|envie de mourir)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(me suicider|suicide|suicider)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(mettre fin à mes jours|en finir)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(plus envie de vivre|ne plus vivre)\\b", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Détecte des signes de détresse dans un texte.
     * 
     * @param content Contenu à analyser
     * @param moderationScores Scores de modération IA
     * @return true si détresse détectée
     */
    public boolean detectDistress(String content, Map<String, Double> moderationScores) {
        // 1. Vérifier score self-harm de l'IA
        Double selfHarmScore = moderationScores.getOrDefault("self-harm", 0.0);
        if (selfHarmScore > 0.3) {
            log.warn("High self-harm score detected: {}", selfHarmScore);
            return true;
        }
        
        // 2. Analyse par patterns
        String lowerContent = content.toLowerCase();
        for (Pattern pattern : SUICIDAL_PATTERNS) {
            if (pattern.matcher(lowerContent).find()) {
                log.warn("Suicidal pattern detected: {}", pattern.pattern());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gère la détection de détresse: notification + ressources.
     * 
     * @param userId ID utilisateur
     * @param conversationId ID conversation
     */
    public void handleDistress(String userId, String conversationId) {
        log.info("Handling distress for user: {}", userId);
        
        // 1. Émettre événement Kafka pour alerter modérateurs
        eventPublisher.publishDistressDetected(
            new DistressDetectedEvent(userId, conversationId)
        );
        
        // 2. Envoyer message empathique immédiat
        String empathyMessage = buildEmpathyMessage();
        notificationService.sendSystemMessage(userId, conversationId, empathyMessage);
        
        // 3. Envoyer ressources d'aide
        String helpResourcesMessage = buildHelpResourcesMessage();
        notificationService.sendSystemMessage(userId, conversationId, helpResourcesMessage);
        
        // 4. Notification équipe support (si disponible)
        notificationService.alertSupportTeam(userId, "DISTRESS_DETECTED");
    }
    
    /**
     * Construit un message empathique.
     */
    private String buildEmpathyMessage() {
        return """
            Je remarque que vous traversez peut-être un moment difficile.
            
            Sachez que vous n'êtes pas seul(e). Il existe des personnes qualifiées 
            qui peuvent vous aider et vous écouter, 24h/24 et 7j/7.
            
            Votre bien-être est important. 💙
            """;
    }
    
    /**
     * Construit un message avec les ressources d'aide.
     */
    private String buildHelpResourcesMessage() {
        return """
            **Ressources d'aide immédiate:**
            
            🇫🇷 France:
            - SOS Suicide Phénix: 01 40 44 46 45
            - Suicide Écoute: 01 45 39 40 00
            - SOS Amitié: 09 72 39 40 50
            
            🌍 International:
            - Ligne d'écoute 24/7: findahelpline.com
            
            🆘 Urgence: Appelez le 15 (SAMU) ou le 112
            
            N'hésitez pas à en parler à un proche de confiance ou à consulter 
            un professionnel de santé.
            """;
    }
}

// =============================================================================
// FICHIER 11: ModerationController.java
// =============================================================================
package com.nexusai.moderation.controller;

import com.nexusai.moderation.model.dto.*;
import com.nexusai.moderation.service.moderation.TextModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour les opérations de modération.
 * 
 * @author NexusAI Team
 */
@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Moderation", description = "Content moderation APIs")
public class ModerationController {
    
    private final TextModerationService textModerationService;
    
    /**
     * Modère du contenu textuel.
     * 
     * @param request Requête de modération
     * @param userId ID utilisateur (extrait du JWT)
     * @return Résultat de modération
     */
    @PostMapping("/text")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Moderate text content")
    public ResponseEntity<ModerationResponse> moderateText(
            @Valid @RequestBody ModerationRequest request,
            @AuthenticationPrincipal String userId) {
        
        log.info("Moderation request for user: {}", userId);
        
        ModerationResponse response = textModerationService.moderateText(
            request.getContent(),
            userId,
            request.getConversationId()
        );
        
        return ResponseEntity.ok(response);
    }
}

// =============================================================================
// FIN DU CODE
// =============================================================================

/**
 * NOTES D'IMPLÉMENTATION:
 * 
 * 1. Services manquants à implémenter:
 *    - OpenAIModerationClient (client HTTP vers OpenAI)
 *    - UserServiceClient (Feign client vers User Service)
 *    - BlacklistService (gestion blacklist)
 *    - NotificationService (envoi notifications)
 *    - ModerationRulesService (gestion règles)
 * 
 * 2. Repositories à créer:
 *    - ModerationIncidentRepository extends JpaRepository
 *    - ModerationRuleRepository extends JpaRepository
 * 
 * 3. Configuration:
 *    - application.yml (DB, Kafka, APIs externes)
 *    - SecurityConfig.java (JWT authentication)
 *    - KafkaConfig.java (topics, producers, consumers)
 * 
 * 4. Tests:
 *    - TextModerationServiceTest (tests unitaires)
 *    - ModerationIntegrationTest (tests intégration)
 *    - DistressDetectionServiceTest
 * 
 * 5. Migration DB:
 *    - V1__create_moderation_tables.sql
 *    - V2__insert_default_rules.sql
 */
