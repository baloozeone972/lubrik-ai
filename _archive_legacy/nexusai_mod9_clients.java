// =============================================================================
// FICHIER 1: OpenAIModerationClient.java
// =============================================================================
package com.nexusai.moderation.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client pour l'API de modération d'OpenAI.
 * 
 * Documentation: https://platform.openai.com/docs/guides/moderation
 * 
 * Catégories détectées:
 * - sexual: Contenu sexuel
 * - sexual/minors: Contenu pédopornographique (CSAM)
 * - hate: Discours haineux
 * - harassment: Harcèlement
 * - self-harm: Auto-mutilation
 * - violence: Violence
 * - violence/graphic: Violence graphique
 * 
 * @author NexusAI Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIModerationClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${moderation.openai.api-key}")
    private String apiKey;
    
    @Value("${moderation.openai.moderation-endpoint}")
    private String endpoint;
    
    /**
     * Analyse un texte avec l'API de modération OpenAI.
     * 
     * @param text Texte à analyser
     * @return Map avec scores par catégorie (0.0 - 1.0)
     */
    public Map<String, Double> moderate(String text) {
        try {
            log.debug("Calling OpenAI Moderation API");
            
            // Préparer la requête
            Map<String, Object> requestBody = Map.of("input", text);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Appel API
            ResponseEntity<OpenAIModerationResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                entity,
                OpenAIModerationResponse.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("OpenAI API call failed: {}", response.getStatusCode());
                return getFallbackScores();
            }
            
            // Extraire les scores
            OpenAIModerationResponse body = response.getBody();
            if (body.getResults() == null || body.getResults().isEmpty()) {
                return getFallbackScores();
            }
            
            ModerationResult result = body.getResults().get(0);
            Map<String, Double> scores = result.getCategoryScores();
            
            log.debug("OpenAI scores: {}", scores);
            return scores;
            
        } catch (Exception e) {
            log.error("Error calling OpenAI Moderation API", e);
            return getFallbackScores();
        }
    }
    
    /**
     * Retourne des scores par défaut en cas d'erreur API.
     */
    private Map<String, Double> getFallbackScores() {
        Map<String, Double> scores = new HashMap<>();
        scores.put("sexual", 0.0);
        scores.put("sexual/minors", 0.0);
        scores.put("hate", 0.0);
        scores.put("harassment", 0.0);
        scores.put("self-harm", 0.0);
        scores.put("violence", 0.0);
        scores.put("violence/graphic", 0.0);
        return scores;
    }
    
    // DTOs pour la réponse OpenAI
    
    @Data
    private static class OpenAIModerationResponse {
        private String id;
        private String model;
        private List<ModerationResult> results;
    }
    
    @Data
    private static class ModerationResult {
        private boolean flagged;
        private Map<String, Boolean> categories;
        private Map<String, Double> categoryScores;
        
        // Mapping des noms de catégories
        public Map<String, Double> getCategoryScores() {
            Map<String, Double> normalized = new HashMap<>();
            if (categoryScores != null) {
                categoryScores.forEach((key, value) -> {
                    // Normaliser les noms de clés
                    String normalizedKey = key.replace("_", "/");
                    normalized.put(normalizedKey, value);
                });
            }
            return normalized;
        }
    }
}

// =============================================================================
// FICHIER 2: UserServiceClient.java
// =============================================================================
package com.nexusai.moderation.service.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Client Feign pour communiquer avec le User Service.
 * 
 * Permet de récupérer:
 * - Informations utilisateur (plan, préférences)
 * - Statut KYC
 * - Consentements actifs
 * 
 * @author NexusAI Team
 */
@FeignClient(
    name = "user-service",
    url = "${moderation.services.user-service.url}",
    configuration = FeignClientConfig.class
)
public interface UserServiceClient {
    
    /**
     * Récupère les informations complètes d'un utilisateur.
     */
    @GetMapping("/api/v1/users/{userId}")
    UserInfo getUserInfo(@PathVariable String userId);
    
    /**
     * Vérifie si l'utilisateur a un KYC valide d'un certain niveau.
     */
    @GetMapping("/api/v1/users/{userId}/kyc/has-level")
    boolean hasValidKYC(@PathVariable String userId, @RequestParam int level);
    
    /**
     * Vérifie si l'utilisateur a un consentement actif.
     */
    @GetMapping("/api/v1/users/{userId}/consents/{consentType}/active")
    boolean hasActiveConsent(@PathVariable String userId, @PathVariable String consentType);
    
    // DTOs
    
    @Data
    class UserInfo {
        private String id;
        private String email;
        private SubscriptionInfo subscription;
        private UserPreferences preferences;
    }
    
    @Data
    class SubscriptionInfo {
        private String plan;  // FREE, STANDARD, PREMIUM, VIP_PLUS
        
        public boolean isFreeOrStandard() {
            return "FREE".equals(plan) || "STANDARD".equals(plan);
        }
        
        public boolean isPremium() {
            return "PREMIUM".equals(plan);
        }
        
        public boolean isVipPlus() {
            return "VIP_PLUS".equals(plan);
        }
    }
    
    @Data
    class UserPreferences {
        private String moderationLevel;  // STRICT, LIGHT, OPTIONAL
        
        public com.nexusai.moderation.model.enums.ModerationLevel getModerationLevel() {
            return com.nexusai.moderation.model.enums.ModerationLevel.valueOf(
                moderationLevel != null ? moderationLevel : "STRICT"
            );
        }
    }
}

// =============================================================================
// FICHIER 3: FeignClientConfig.java
// =============================================================================
package com.nexusai.moderation.service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Configuration pour les clients Feign.
 * 
 * Ajoute automatiquement le token JWT dans les headers des requêtes.
 */
public class FeignClientConfig {
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Authentication authentication = SecurityContextHolder
                    .getContext()
                    .getAuthentication();
                
                if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                    template.header("Authorization", "Bearer " + jwt.getTokenValue());
                }
            }
        };
    }
}

// =============================================================================
// FICHIER 4: BlacklistService.java
// =============================================================================
package com.nexusai.moderation.service.moderation;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service de gestion de la blacklist de termes interdits.
 * 
 * Charge une liste de mots/expressions interdits depuis un fichier
 * et permet de vérifier rapidement leur présence dans un texte.
 * 
 * @author NexusAI Team
 */
@Service
@Slf4j
public class BlacklistService {
    
    @Value("${moderation.blacklist.file-path}")
    private Resource blacklistFile;
    
    @Value("${moderation.blacklist.enabled:true}")
    private boolean enabled;
    
    private Set<String> blacklistedTerms = new HashSet<>();
    private Set<Pattern> blacklistedPatterns = new HashSet<>();
    
    /**
     * Charge la blacklist au démarrage.
     */
    @PostConstruct
    public void loadBlacklist() {
        if (!enabled) {
            log.info("Blacklist disabled");
            return;
        }
        
        try {
            log.info("Loading blacklist from: {}", blacklistFile.getFilename());
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(blacklistFile.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                int count = 0;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Ignorer lignes vides et commentaires
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    
                    // Pattern regex (commence par "regex:")
                    if (line.startsWith("regex:")) {
                        String pattern = line.substring(6).trim();
                        blacklistedPatterns.add(
                            Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                        );
                    } else {
                        // Terme simple
                        blacklistedTerms.add(line.toLowerCase());
                    }
                    
                    count++;
                }
                
                log.info("Loaded {} blacklisted terms and {} patterns", 
                    blacklistedTerms.size(), blacklistedPatterns.size());
            }
            
        } catch (Exception e) {
            log.error("Error loading blacklist", e);
        }
    }
    
    /**
     * Recharge la blacklist toutes les heures.
     */
    @Scheduled(fixedDelayString = "${moderation.blacklist.reload-interval:3600000}")
    public void reloadBlacklist() {
        log.info("Reloading blacklist...");
        blacklistedTerms.clear();
        blacklistedPatterns.clear();
        loadBlacklist();
    }
    
    /**
     * Vérifie si le texte contient des termes blacklistés.
     * 
     * @param content Texte à vérifier
     * @return true si un terme blacklisté est trouvé
     */
    public boolean containsBlacklistedTerms(String content) {
        if (!enabled || content == null) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        
        // 1. Vérifier termes exacts
        for (String term : blacklistedTerms) {
            if (lowerContent.contains(term)) {
                log.debug("Blacklisted term found: {}", term);
                return true;
            }
        }
        
        // 2. Vérifier patterns regex
        for (Pattern pattern : blacklistedPatterns) {
            if (pattern.matcher(lowerContent).find()) {
                log.debug("Blacklisted pattern matched: {}", pattern.pattern());
                return true;
            }
        }
        
        return false;
    }
}

// =============================================================================
// FICHIER 5: ModerationRulesService.java
// =============================================================================
package com.nexusai.moderation.service.moderation;

import com.nexusai.moderation.model.entity.ModerationRule;
import com.nexusai.moderation.model.enums.ModerationLevel;
import com.nexusai.moderation.repository.ModerationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service de gestion des règles de modération.
 * 
 * Récupère les règles applicables selon le niveau de modération.
 * Les règles sont cachées pour optimiser les performances.
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationRulesService {
    
    private final ModerationRuleRepository ruleRepository;
    
    /**
     * Récupère les règles pour un niveau de modération donné.
     * 
     * Résultat mis en cache (invalidé toutes les 5 minutes).
     * 
     * @param level Niveau de modération
     * @return Liste des règles actives
     */
    @Cacheable(value = "moderation-rules", key = "#level")
    public List<ModerationRule> getRulesForLevel(ModerationLevel level) {
        log.debug("Fetching rules for level: {}", level);
        return ruleRepository.findByModerationLevelAndActiveTrue(level);
    }
    
    /**
     * Récupère toutes les règles actives.
     */
    public List<ModerationRule> getAllActiveRules() {
        return ruleRepository.findByActiveTrue();
    }
}

// =============================================================================
// FICHIER 6: ModerationIncidentRepository.java
// =============================================================================
package com.nexusai.moderation.repository;

import com.nexusai.moderation.model.entity.ModerationIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository pour les incidents de modération.
 * 
 * @author NexusAI Team
 */
@Repository
public interface ModerationIncidentRepository extends JpaRepository<ModerationIncident, UUID> {
    
    /**
     * Trouve tous les incidents d'un utilisateur.
     */
    List<ModerationIncident> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Trouve les incidents par statut.
     */
    List<ModerationIncident> findByStatusOrderByCreatedAtDesc(String status);
    
    /**
     * Compte les incidents CRITICAL non reviewés.
     */
    @Query("SELECT COUNT(i) FROM ModerationIncident i " +
           "WHERE i.severity = 'CRITICAL' AND i.status = 'PENDING'")
    long countCriticalPendingIncidents();
    
    /**
     * Trouve les incidents d'un utilisateur dans une période.
     */
    @Query("SELECT i FROM ModerationIncident i " +
           "WHERE i.userId = :userId " +
           "AND i.createdAt BETWEEN :startDate AND :endDate")
    List<ModerationIncident> findUserIncidentsInPeriod(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Statistiques par type d'incident.
     */
    @Query("SELECT i.incidentType, COUNT(i) " +
           "FROM ModerationIncident i " +
           "WHERE i.createdAt >= :since " +
           "GROUP BY i.incidentType")
    List<Object[]> getIncidentStatistics(@Param("since") LocalDateTime since);
}

// =============================================================================
// FICHIER 7: ModerationRuleRepository.java
// =============================================================================
package com.nexusai.moderation.repository;

import com.nexusai.moderation.model.entity.ModerationRule;
import com.nexusai.moderation.model.enums.ModerationLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour les règles de modération.
 * 
 * @author NexusAI Team
 */
@Repository
public interface ModerationRuleRepository extends JpaRepository<ModerationRule, UUID> {
    
    /**
     * Trouve les règles actives pour un niveau de modération.
     */
    List<ModerationRule> findByModerationLevelAndActiveTrue(ModerationLevel level);
    
    /**
     * Trouve toutes les règles actives.
     */
    List<ModerationRule> findByActiveTrue();
    
    /**
     * Trouve une règle par niveau et catégorie.
     */
    ModerationRule findByModerationLevelAndContentCategory(
        ModerationLevel level, 
        String category
    );
}

// =============================================================================
// FICHIER 8: ContentHashUtil.java
// =============================================================================
package com.nexusai.moderation.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire pour créer des hash SHA-256 de contenu.
 * 
 * Utilisé pour stocker une empreinte du contenu sans stocker
 * le contenu lui-même (privacy).
 * 
 * @author NexusAI Team
 */
public class ContentHashUtil {
    
    /**
     * Calcule le hash SHA-256 d'une chaîne.
     * 
     * @param content Contenu à hasher
     * @return Hash hexadécimal
     */
    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Convertit un tableau de bytes en chaîne hexadécimale.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

// =============================================================================
// FICHIER 9: NotificationService.java (Interface)
// =============================================================================
package com.nexusai.moderation.service.notification;

/**
 * Service de notification (interface).
 * 
 * À implémenter selon la stratégie de notification choisie
 * (WebSocket, Push, Email, etc.).
 * 
 * @author NexusAI Team
 */
public interface NotificationService {
    
    /**
     * Envoie un message système dans une conversation.
     */
    void sendSystemMessage(String userId, String conversationId, String message);
    
    /**
     * Alerte l'équipe support.
     */
    void alertSupportTeam(String userId, String alertType);
}

// =============================================================================
// FIN DU CODE
// =============================================================================
