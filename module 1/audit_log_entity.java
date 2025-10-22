package com.nexusai.core.domain;

import com.nexusai.core.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une entrée du journal d'audit.
 * 
 * Trace toutes les actions importantes effectuées dans l'application :
 * - Actions utilisateur (connexion, modification profil, etc.)
 * - Actions administratives
 * - Événements de sécurité
 * - Modifications de données sensibles
 * 
 * Conforme aux exigences RGPD et de compliance.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_created", columnList = "created_at"),
    @Index(name = "idx_audit_ip", columnList = "ip_address")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Utilisateur ayant effectué l'action (null si système).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * Type d'action effectuée.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;
    
    /**
     * Type d'entité concernée (User, Subscription, etc.).
     */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    
    /**
     * ID de l'entité concernée.
     */
    @Column(name = "entity_id")
    private UUID entityId;
    
    /**
     * Description détaillée de l'action.
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Anciennes valeurs (JSON) - pour les modifications.
     */
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;
    
    /**
     * Nouvelles valeurs (JSON) - pour les modifications.
     */
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;
    
    /**
     * Adresse IP de l'utilisateur.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent du client.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Résultat de l'action (SUCCESS, FAILURE, PARTIAL).
     */
    @Column(length = 20)
    private String result;
    
    /**
     * Message d'erreur en cas d'échec.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Niveau de sévérité (INFO, WARNING, ERROR, CRITICAL).
     */
    @Column(length = 20)
    @Builder.Default
    private String severity = "INFO";
    
    /**
     * Métadonnées additionnelles (JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * Date de création de l'entrée.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Crée une entrée d'audit pour une connexion réussie.
     * 
     * @param user Utilisateur connecté
     * @param ipAddress Adresse IP
     * @param userAgent User agent
     * @return AuditLog
     */
    public static AuditLog createLoginSuccess(User user, String ipAddress, String userAgent) {
        return AuditLog.builder()
                .user(user)
                .action(AuditAction.USER_LOGIN)
                .entityType("User")
                .entityId(user.getId())
                .description("Connexion réussie")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result("SUCCESS")
                .severity("INFO")
                .build();
    }
    
    /**
     * Crée une entrée d'audit pour une tentative de connexion échouée.
     * 
     * @param email Email utilisé
     * @param ipAddress Adresse IP
     * @param userAgent User agent
     * @param reason Raison de l'échec
     * @return AuditLog
     */
    public static AuditLog createLoginFailure(
            String email, 
            String ipAddress, 
            String userAgent,
            String reason) {
        return AuditLog.builder()
                .action(AuditAction.USER_LOGIN_FAILED)
                .entityType("User")
                .description("Tentative de connexion échouée pour: " + email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result("FAILURE")
                .errorMessage(reason)
                .severity("WARNING")
                .build();
    }
    
    /**
     * Crée une entrée d'audit pour une modification.
     * 
     * @param user Utilisateur ayant effectué la modification
     * @param entityType Type d'entité
     * @param entityId ID de l'entité
     * @param oldValues Anciennes valeurs (JSON)
     * @param newValues Nouvelles valeurs (JSON)
     * @param ipAddress Adresse IP
     * @return AuditLog
     */
    public static AuditLog createUpdate(
            User user,
            String entityType,
            UUID entityId,
            String oldValues,
            String newValues,
            String ipAddress) {
        return AuditLog.builder()
                .user(user)
                .action(AuditAction.DATA_UPDATE)
                .entityType(entityType)
                .entityId(entityId)
                .description("Modification de " + entityType)
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .result("SUCCESS")
                .severity("INFO")
                .build();
    }
}
