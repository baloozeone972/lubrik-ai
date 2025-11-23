// =============================================================================
// FICHIER 1: ConsentManagementService.java
// =============================================================================
package com.nexusai.moderation.service.consent;

import com.nexusai.moderation.model.entity.AdultContentConsent;
import com.nexusai.moderation.repository.AdultContentConsentRepository;
import com.nexusai.moderation.service.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de gestion des consentements pour contenu adulte.
 * 
 * Requis pour utilisateurs VIP+ souhaitant activer le mode OPTIONAL.
 * 
 * Workflow :
 * 1. Vérifier KYC Level 3
 * 2. Créer consentement avec signature numérique
 * 3. Enregistrer IP + User-Agent (traçabilité)
 * 4. Expiration après 90 jours
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentManagementService {
    
    private final AdultContentConsentRepository consentRepository;
    private final DigitalSignatureService signatureService;
    private final UserServiceClient userServiceClient;
    
    private static final String CONSENT_VERSION = "1.0";
    private static final int CONSENT_VALIDITY_DAYS = 90;
    
    /**
     * Crée un consentement pour contenu adulte.
     * 
     * @param userId ID utilisateur
     * @param consentType Type de consentement
     * @param ipAddress Adresse IP
     * @param userAgent User-Agent
     * @return Consentement créé
     */
    @Transactional
    public AdultContentConsent createConsent(
            UUID userId,
            String consentType,
            String ipAddress,
            String userAgent) {
        
        log.info("Creating consent for user: {}, type: {}", userId, consentType);
        
        // 1. Vérifier KYC Level 3
        if (!userServiceClient.hasValidKYC(userId.toString(), 3)) {
            throw new InsufficientKYCException(
                "KYC Level 3 required for adult content consent"
            );
        }
        
        // 2. Vérifier qu'il n'y a pas déjà un consentement actif
        Optional<AdultContentConsent> existing = consentRepository
            .findActiveConsentByUserIdAndType(userId, consentType);
        
        if (existing.isPresent()) {
            log.info("User {} already has active consent", userId);
            return existing.get();
        }
        
        // 3. Construire les données de consentement
        String consentData = buildConsentData(userId, consentType, ipAddress);
        
        // 4. Générer signature numérique
        String signature = signatureService.signData(consentData);
        
        // 5. Créer le consentement
        AdultContentConsent consent = AdultContentConsent.builder()
            .userId(userId)
            .consentType(consentType)
            .version(CONSENT_VERSION)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .digitalSignature(signature)
            .signedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(CONSENT_VALIDITY_DAYS))
            .revoked(false)
            .build();
        
        AdultContentConsent saved = consentRepository.save(consent);
        
        log.info("Consent created: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Révoque un consentement.
     */
    @Transactional
    public void revokeConsent(UUID consentId, UUID userId) {
        AdultContentConsent consent = consentRepository.findById(consentId)
            .orElseThrow(() -> new ConsentNotFoundException(consentId));
        
        // Vérifier propriété
        if (!consent.getUserId().equals(userId)) {
            throw new UnauthorizedConsentAccessException(
                "User does not own this consent"
            );
        }
        
        consent.setRevoked(true);
        consent.setRevokedAt(LocalDateTime.now());
        
        consentRepository.save(consent);
        
        log.info("Consent {} revoked by user {}", consentId, userId);
    }
    
    /**
     * Vérifie si un utilisateur a un consentement actif.
     */
    public boolean hasActiveConsent(UUID userId, String consentType) {
        Optional<AdultContentConsent> consent = consentRepository
            .findActiveConsentByUserIdAndType(userId, consentType);
        
        return consent.isPresent() && !consent.get().isExpired();
    }
    
    /**
     * Récupère les consentements actifs d'un utilisateur.
     */
    public List<AdultContentConsent> getActiveConsents(UUID userId) {
        return consentRepository.findByUserIdAndRevokedFalse(userId).stream()
            .filter(c -> !c.isExpired())
            .toList();
    }
    
    /**
     * Vérifie la signature d'un consentement.
     */
    public boolean verifyConsentSignature(UUID consentId) {
        AdultContentConsent consent = consentRepository.findById(consentId)
            .orElseThrow(() -> new ConsentNotFoundException(consentId));
        
        String consentData = buildConsentData(
            consent.getUserId(),
            consent.getConsentType(),
            consent.getIpAddress()
        );
        
        return signatureService.verifySignature(
            consentData,
            consent.getDigitalSignature()
        );
    }
    
    /**
     * Nettoie les consentements expirés.
     */
    @Transactional
    public int cleanupExpiredConsents() {
        List<AdultContentConsent> expired = consentRepository
            .findByExpiresAtBefore(LocalDateTime.now());
        
        // Marquer comme révoqués plutôt que de supprimer (audit)
        for (AdultContentConsent consent : expired) {
            consent.setRevoked(true);
            consent.setRevokedAt(LocalDateTime.now());
        }
        
        consentRepository.saveAll(expired);
        
        log.info("Marked {} expired consents as revoked", expired.size());
        
        return expired.size();
    }
    
    /**
     * Construit les données de consentement pour signature.
     */
    private String buildConsentData(UUID userId, String consentType, String ipAddress) {
        return String.format(
            "USER:%s|TYPE:%s|IP:%s|VERSION:%s|TIMESTAMP:%s",
            userId,
            consentType,
            ipAddress,
            CONSENT_VERSION,
            System.currentTimeMillis()
        );
    }
    
    // Exceptions
    
    public static class InsufficientKYCException extends RuntimeException {
        public InsufficientKYCException(String message) {
            super(message);
        }
    }
    
    public static class ConsentNotFoundException extends RuntimeException {
        public ConsentNotFoundException(UUID consentId) {
            super("Consent not found: " + consentId);
        }
    }
    
    public static class UnauthorizedConsentAccessException extends RuntimeException {
        public UnauthorizedConsentAccessException(String message) {
            super(message);
        }
    }
}

// =============================================================================
// FICHIER 2: DigitalSignatureService.java
// =============================================================================
package com.nexusai.moderation.service.consent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * Service de signature numérique pour les consentements.
 * 
 * Utilise RSA-2048 pour garantir l'authenticité et la non-répudiation.
 * 
 * @author NexusAI Team
 */
@Service
@Slf4j
public class DigitalSignatureService {
    
    @Value("${moderation.consent.private-key}")
    private String privateKeyBase64;
    
    @Value("${moderation.consent.public-key}")
    private String publicKeyBase64;
    
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    
    /**
     * Signe des données avec la clé privée.
     * 
     * @param data Données à signer
     * @return Signature en Base64
     */
    public String signData(String data) {
        try {
            // Charger la clé privée
            PrivateKey privateKey = loadPrivateKey(privateKeyBase64);
            
            // Créer la signature
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            
            byte[] signatureBytes = signature.sign();
            
            // Encoder en Base64
            return Base64.getEncoder().encodeToString(signatureBytes);
            
        } catch (Exception e) {
            log.error("Error signing data", e);
            throw new SignatureException("Failed to sign data", e);
        }
    }
    
    /**
     * Vérifie une signature avec la clé publique.
     * 
     * @param data Données originales
     * @param signatureBase64 Signature en Base64
     * @return true si signature valide
     */
    public boolean verifySignature(String data, String signatureBase64) {
        try {
            // Charger la clé publique
            PublicKey publicKey = loadPublicKey(publicKeyBase64);
            
            // Décoder la signature
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            
            // Vérifier
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            
            return signature.verify(signatureBytes);
            
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }
    
    /**
     * Génère une paire de clés RSA-2048 (à utiliser en setup).
     */
    public KeyPairInfo generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        
        String privateKey = Base64.getEncoder()
            .encodeToString(pair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder()
            .encodeToString(pair.getPublic().getEncoded());
        
        return new KeyPairInfo(privateKey, publicKey);
    }
    
    // Helper methods
    
    private PrivateKey loadPrivateKey(String keyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }
    
    private PublicKey loadPublicKey(String keyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(spec);
    }
    
    // DTOs
    
    public record KeyPairInfo(String privateKey, String publicKey) {}
    
    // Exceptions
    
    public static class SignatureException extends RuntimeException {
        public SignatureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// =============================================================================
// FICHIER 3: NotificationServiceImpl.java
// =============================================================================
package com.nexusai.moderation.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Implémentation du service de notification.
 * 
 * Supporte :
 * - Messages système dans conversations (via Kafka)
 * - Alertes équipes (Slack, Email)
 * - Notifications push
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EmailService emailService;
    private final SlackNotificationService slackService;
    
    @Value("${moderation.notifications.slack.enabled:true}")
    private boolean slackEnabled;
    
    @Value("${moderation.notifications.email.enabled:true}")
    private boolean emailEnabled;
    
    @Override
    public void sendSystemMessage(String userId, String conversationId, String message) {
        log.info("Sending system message to user: {}", userId);
        
        // Publier événement Kafka pour le service de conversation
        Map<String, Object> event = Map.of(
            "type", "SYSTEM_MESSAGE",
            "userId", userId,
            "conversationId", conversationId != null ? conversationId : "",
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        
        kafkaTemplate.send("conversation.system.messages", userId, event);
    }
    
    @Override
    public void alertSupportTeam(String userId, String alertType) {
        log.warn("Alerting support team: {} for user: {}", alertType, userId);
        
        String message = String.format(
            "⚠️ Support Alert: %s\nUser ID: %s\nTime: %s",
            alertType,
            userId,
            java.time.LocalDateTime.now()
        );
        
        if (slackEnabled) {
            slackService.sendToChannel("support-alerts", message);
        }
        
        if (emailEnabled) {
            emailService.sendToSupport("Support Alert: " + alertType, message);
        }
    }
    
    @Override
    public void alertModerators(String message) {
        log.warn("Alerting moderators: {}", message);
        
        if (slackEnabled) {
            slackService.sendToChannel("moderation-team", message);
        }
        
        if (emailEnabled) {
            emailService.sendToModerators("Moderation Alert", message);
        }
    }
    
    @Override
    public void alertLegalTeam(String message) {
        log.error("LEGAL ALERT: {}", message);
        
        // TOUJOURS envoyer, même si désactivé
        slackService.sendToChannel("legal-urgent", "🚨 " + message);
        emailService.sendToLegal("URGENT: Legal Escalation", message);
        
        // SMS si configuré
        // smsService.sendToLegalTeam(message);
    }
    
    @Override
    public void alertManagement(String message) {
        log.warn("Alerting management: {}", message);
        
        if (slackEnabled) {
            slackService.sendToChannel("management", message);
        }
        
        if (emailEnabled) {
            emailService.sendToManagement("Management Alert", message);
        }
    }
}

// =============================================================================
// FICHIER 4: ModerationEventPublisher.java (Complet)
// =============================================================================
package com.nexusai.moderation.event;

import com.nexusai.moderation.event.events.*;
import com.nexusai.moderation.model.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publisher d'événements de modération via Kafka.
 * 
 * @author NexusAI Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String TOPIC_INCIDENTS = "moderation.incidents";
    private static final String TOPIC_WARNINGS = "moderation.warnings";
    private static final String TOPIC_BANS = "moderation.bans";
    private static final String TOPIC_DISTRESS = "moderation.distress";
    
    /**
     * Publie la création d'un incident.
     */
    public void publishIncidentCreated(ModerationIncident incident) {
        log.debug("Publishing incident created: {}", incident.getId());
        
        IncidentCreatedEvent event = new IncidentCreatedEvent(
            incident.getId().toString(),
            incident.getUserId().toString(),
            incident.getIncidentType().name(),
            incident.getSeverity().name(),
            incident.getContentType().name()
        );
        
        kafkaTemplate.send(TOPIC_INCIDENTS, event.userId(), event);
    }
    
    /**
     * Publie la review d'un incident.
     */
    public void publishIncidentReviewed(ModerationIncident incident) {
        log.debug("Publishing incident reviewed: {}", incident.getId());
        
        IncidentReviewedEvent event = new IncidentReviewedEvent(
            incident.getId().toString(),
            incident.getStatus(),
            incident.getReviewedBy() != null ? incident.getReviewedBy().toString() : null
        );
        
        kafkaTemplate.send(TOPIC_INCIDENTS, incident.getUserId().toString(), event);
    }
    
    /**
     * Publie un avertissement utilisateur.
     */
    public void publishUserWarned(UserWarning warning) {
        log.debug("Publishing user warned: {}", warning.getUserId());
        
        UserWarnedEvent event = new UserWarnedEvent(
            warning.getUserId().toString(),
            warning.getWarningType(),
            warning.getDescription()
        );
        
        kafkaTemplate.send(TOPIC_WARNINGS, warning.getUserId().toString(), event);
    }
    
    /**
     * Publie un bannissement utilisateur.
     */
    public void publishUserBanned(UUID userId, String banType, String reason) {
        log.warn("Publishing user banned: {}", userId);
        
        UserBannedEvent event = new UserBannedEvent(
            userId.toString(),
            banType,
            reason,
            System.currentTimeMillis()
        );
        
        kafkaTemplate.send(TOPIC_BANS, userId.toString(), event);
    }
    
    /**
     * Publie la détection de détresse.
     */
    public void publishDistressDetected(DistressDetectedEvent event) {
        log.warn("Publishing distress detected: {}", event.userId());
        
        kafkaTemplate.send(TOPIC_DISTRESS, event.userId(), event);
    }
}

// =============================================================================
// FICHIER 5: ModerationEventListener.java
// =============================================================================
package com.nexusai.moderation.event;

import com.nexusai.moderation.repository.ModerationIncidentRepository;
import com.nexusai.moderation.repository.AdultContentConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Listener d'événements externes (User Service, etc.).
 * 
 * @author NexusAI Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationEventListener {
    
    private final ModerationIncidentRepository incidentRepository;
    private final AdultContentConsentRepository consentRepository;
    
    /**
     * Écoute les suppressions d'utilisateurs.
     */
    @KafkaListener(topics = "user.deleted", groupId = "moderation-group")
    public void onUserDeleted(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        log.info("User deleted event received: {}", userId);
        
        // Supprimer les incidents de l'utilisateur (RGPD)
        UUID userUuid = UUID.fromString(userId);
        var incidents = incidentRepository.findByUserId(userUuid);
        incidentRepository.deleteAll(incidents);
        
        // Supprimer les consentements
        var consents = consentRepository.findByUserId(userUuid);
        consentRepository.deleteAll(consents);
        
        log.info("Deleted {} incidents and {} consents for user {}",
            incidents.size(), consents.size(), userId);
    }
    
    /**
     * Écoute les changements d'abonnement.
     */
    @KafkaListener(topics = "subscription.changed", groupId = "moderation-group")
    public void onSubscriptionChanged(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        String newPlan = (String) event.get("newPlan");
        
        log.info("Subscription changed for user {}: {}", userId, newPlan);
        
        // Si downgrade depuis VIP+ → révoquer consentements
        if ("FREE".equals(newPlan) || "STANDARD".equals(newPlan)) {
            UUID userUuid = UUID.fromString(userId);
            var consents = consentRepository.findByUserIdAndRevokedFalse(userUuid);
            
            consents.forEach(consent -> {
                consent.setRevoked(true);
                consent.setRevokedAt(java.time.LocalDateTime.now());
            });
            
            consentRepository.saveAll(consents);
            
            log.info("Revoked {} consents for downgraded user {}", 
                consents.size(), userId);
        }
    }
}

// =============================================================================
// FIN - Consent, Notifications & Events
// =============================================================================
