// =============================================================================
// FICHIER 1: UserWarning.java
// =============================================================================
package com.nexusai.moderation.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un avertissement émis à un utilisateur.
 * 
 * Les avertissements expirent après 30 jours et contribuent
 * au système de points pouvant mener à un bannissement.
 * 
 * @author NexusAI Team
 */
@Entity
@Table(name = "user_warnings", indexes = {
    @Index(name = "idx_warnings_user_id", columnList = "user_id"),
    @Index(name = "idx_warnings_incident_id", columnList = "incident_id"),
    @Index(name = "idx_warnings_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWarning {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * ID de l'utilisateur ayant reçu l'avertissement.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    /**
     * ID de l'incident ayant causé l'avertissement.
     */
    @Column(name = "incident_id")
    private UUID incidentId;
    
    /**
     * Type d'avertissement (LOW, MEDIUM, HIGH, CRITICAL).
     */
    @Column(name = "warning_type", nullable = false, length = 50)
    private String warningType;
    
    /**
     * Description de l'avertissement.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * L'utilisateur a-t-il pris connaissance de l'avertissement ?
     */
    @Column(name = "acknowledged")
    private Boolean acknowledged = false;
    
    /**
     * Date de prise de connaissance.
     */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    /**
     * Date d'expiration de l'avertissement (30 jours par défaut).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * Date de création.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusDays(30);
        }
    }
    
    /**
     * Vérifie si l'avertissement a expiré.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Retourne le nombre de points associé à cet avertissement.
     */
    public int getPoints() {
        return switch (warningType.toUpperCase()) {
            case "CRITICAL" -> 5;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 1;
        };
    }
}

// =============================================================================
// FICHIER 2: AdultContentConsent.java
// =============================================================================
package com.nexusai.moderation.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un consentement explicite pour accéder à du contenu adulte.
 * 
 * Requis pour utilisateurs VIP+ avec KYC Level 3.
 * - Signature numérique pour authenticité
 * - Traçabilité complète (IP, User-Agent)
 * - Expiration après 90 jours
 * 
 * @author NexusAI Team
 */
@Entity
@Table(name = "adult_content_consents", indexes = {
    @Index(name = "idx_consents_user_id", columnList = "user_id"),
    @Index(name = "idx_consents_user_type", columnList = "user_id, consent_type"),
    @Index(name = "idx_consents_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdultContentConsent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * ID de l'utilisateur.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    /**
     * Type de consentement (ex: "UNMODERATED_MODE", "ADULT_CONTENT").
     */
    @Column(name = "consent_type", nullable = false, length = 50)
    private String consentType;
    
    /**
     * Version du consentement (pour tracking des changements de CGU).
     */
    @Column(name = "version", nullable = false, length = 10)
    private String version;
    
    /**
     * Adresse IP lors de la signature.
     */
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;
    
    /**
     * User-Agent du navigateur.
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    /**
     * Signature numérique RSA-2048 du consentement.
     */
    @Column(name = "digital_signature", nullable = false, columnDefinition = "TEXT")
    private String digitalSignature;
    
    /**
     * Date de signature du consentement.
     */
    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;
    
    /**
     * Date d'expiration (90 jours après signature).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * Le consentement a-t-il été révoqué ?
     */
    @Column(name = "revoked")
    private Boolean revoked = false;
    
    /**
     * Date de révocation.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    /**
     * Date de création.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (signedAt == null) {
            signedAt = createdAt;
        }
        if (expiresAt == null) {
            expiresAt = signedAt.plusDays(90);
        }
    }
    
    /**
     * Vérifie si le consentement est encore valide.
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
    
    /**
     * Vérifie si le consentement a expiré.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

// =============================================================================
// FICHIER 3: ContentType.java (Enum)
// =============================================================================
package com.nexusai.moderation.model.enums;

/**
 * Types de contenu pouvant être modérés.
 * 
 * @author NexusAI Team
 */
public enum ContentType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO
}

// =============================================================================
// FICHIER 4: IncidentCreatedEvent.java
// =============================================================================
package com.nexusai.moderation.event.events;

import java.io.Serializable;

/**
 * Événement émis lors de la création d'un incident.
 * 
 * @author NexusAI Team
 */
public record IncidentCreatedEvent(
    String incidentId,
    String userId,
    String incidentType,
    String severity,
    String contentType
) implements Serializable {}

// =============================================================================
// FICHIER 5: IncidentReviewedEvent.java
// =============================================================================
package com.nexusai.moderation.event.events;

import java.io.Serializable;

/**
 * Événement émis lors de la review d'un incident.
 * 
 * @author NexusAI Team
 */
public record IncidentReviewedEvent(
    String incidentId,
    String newStatus,
    String reviewedBy
) implements Serializable {}

// =============================================================================
// FICHIER 6: UserWarnedEvent.java
// =============================================================================
package com.nexusai.moderation.event.events;

import java.io.Serializable;

/**
 * Événement émis lors de l'émission d'un avertissement à un utilisateur.
 * 
 * @author NexusAI Team
 */
public record UserWarnedEvent(
    String userId,
    String warningType,
    String description
) implements Serializable {}

// =============================================================================
// FICHIER 7: UserBannedEvent.java
// =============================================================================
package com.nexusai.moderation.event.events;

import java.io.Serializable;

/**
 * Événement émis lors du bannissement d'un utilisateur.
 * 
 * @author NexusAI Team
 */
public record UserBannedEvent(
    String userId,
    String banType,      // TEMPORARY_24H, TEMPORARY_7D, PERMANENT
    String reason,
    long timestamp
) implements Serializable {}

// =============================================================================
// FICHIER 8: DistressDetectedEvent.java
// =============================================================================
package com.nexusai.moderation.event.events;

import java.io.Serializable;

/**
 * Événement émis lors de la détection de détresse psychologique.
 * 
 * @author NexusAI Team
 */
public record DistressDetectedEvent(
    String userId,
    String conversationId
) implements Serializable {}

// =============================================================================
// FICHIER 9: NotificationService.java (Interface complète)
// =============================================================================
package com.nexusai.moderation.service.notification;

/**
 * Interface du service de notification.
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
    
    /**
     * Alerte les modérateurs.
     */
    void alertModerators(String message);
    
    /**
     * Alerte l'équipe légale (CSAM, terrorisme).
     */
    void alertLegalTeam(String message);
    
    /**
     * Alerte le management.
     */
    void alertManagement(String message);
}

// =============================================================================
// FICHIER 10: EmailService.java
// =============================================================================
package com.nexusai.moderation.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi d'emails.
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${moderation.notifications.email.support:support@nexusai.com}")
    private String supportEmail;
    
    @Value("${moderation.notifications.email.moderators:moderators@nexusai.com}")
    private String moderatorsEmail;
    
    @Value("${moderation.notifications.email.legal:legal@nexusai.com}")
    private String legalEmail;
    
    @Value("${moderation.notifications.email.management:management@nexusai.com}")
    private String managementEmail;
    
    @Value("${moderation.notifications.email.from:noreply@nexusai.com}")
    private String fromEmail;
    
    /**
     * Envoie un email à l'équipe support.
     */
    public void sendToSupport(String subject, String body) {
        sendEmail(supportEmail, subject, body);
    }
    
    /**
     * Envoie un email aux modérateurs.
     */
    public void sendToModerators(String subject, String body) {
        sendEmail(moderatorsEmail, subject, body);
    }
    
    /**
     * Envoie un email à l'équipe légale.
     */
    public void sendToLegal(String subject, String body) {
        sendEmail(legalEmail, subject, body);
    }
    
    /**
     * Envoie un email au management.
     */
    public void sendToManagement(String subject, String body) {
        sendEmail(managementEmail, subject, body);
    }
    
    /**
     * Envoie un email générique.
     */
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            
            log.info("Email sent to: {}, subject: {}", to, subject);
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }
}

// =============================================================================
// FICHIER 11: SlackNotificationService.java
// =============================================================================
package com.nexusai.moderation.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service de notifications Slack.
 * 
 * @author NexusAI Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlackNotificationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${moderation.notifications.slack.webhook-url:}")
    private String webhookUrl;
    
    @Value("${moderation.notifications.slack.enabled:true}")
    private boolean enabled;
    
    /**
     * Envoie un message sur un canal Slack.
     */
    public void sendToChannel(String channel, String message) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Slack notifications disabled or not configured");
            return;
        }
        
        try {
            Map<String, Object> payload = Map.of(
                "channel", channel,
                "text", message,
                "username", "NexusAI Moderation Bot",
                "icon_emoji", ":robot_face:"
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Slack notification sent to channel: {}", channel);
            } else {
                log.error("Failed to send Slack notification: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error sending Slack notification to channel: {}", channel, e);
        }
    }
}

// =============================================================================
// FICHIER 12: ModerationException.java
// =============================================================================
package com.nexusai.moderation.exception;

/**
 * Exception générique du système de modération.
 * 
 * @author NexusAI Team
 */
public class ModerationException extends RuntimeException {
    
    public ModerationException(String message) {
        super(message);
    }
    
    public ModerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

// =============================================================================
// FICHIER 13: ContentBlockedException.java
// =============================================================================
package com.nexusai.moderation.exception;

import com.nexusai.moderation.model.enums.IncidentType;
import com.nexusai.moderation.model.enums.Severity;
import lombok.Getter;

/**
 * Exception levée quand du contenu est bloqué par la modération.
 * 
 * @author NexusAI Team
 */
@Getter
public class ContentBlockedException extends ModerationException {
    
    private final IncidentType incidentType;
    private final Severity severity;
    private final String incidentId;
    
    public ContentBlockedException(
            String message,
            IncidentType incidentType,
            Severity severity,
            String incidentId) {
        super(message);
        this.incidentType = incidentType;
        this.severity = severity;
        this.incidentId = incidentId;
    }
}

// =============================================================================
// FICHIER 14: GlobalExceptionHandler.java
// =============================================================================
package com.nexusai.moderation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Gestionnaire global des exceptions.
 * 
 * @author NexusAI Team
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ContentBlockedException.class)
    public ResponseEntity<Map<String, Object>> handleContentBlocked(ContentBlockedException ex) {
        log.warn("Content blocked: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "CONTENT_BLOCKED",
            "message", ex.getMessage(),
            "incidentType", ex.getIncidentType().name(),
            "severity", ex.getSeverity().name(),
            "incidentId", ex.getIncidentId(),
            "timestamp", LocalDateTime.now()
        ));
    }
    
    @ExceptionHandler(ModerationException.class)
    public ResponseEntity<Map<String, Object>> handleModeration(ModerationException ex) {
        log.error("Moderation error: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error", "MODERATION_ERROR",
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now()
        ));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "ACCESS_DENIED",
            "message", "Vous n'avez pas les permissions nécessaires",
            "timestamp", LocalDateTime.now()
        ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error", "INTERNAL_ERROR",
            "message", "Une erreur inattendue s'est produite",
            "timestamp", LocalDateTime.now()
        ));
    }
}

// =============================================================================
// FIN - Entités & Événements
// =============================================================================
