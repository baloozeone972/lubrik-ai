package com.nexusai.core.domain;

import com.nexusai.core.enums.EmailVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une demande de vérification d'email.
 * 
 * Gère le processus de vérification des emails lors de l'inscription
 * ou du changement d'adresse email.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "email_verifications", indexes = {
    @Index(name = "idx_email_verif_token", columnList = "token", unique = true),
    @Index(name = "idx_email_verif_user", columnList = "user_id"),
    @Index(name = "idx_email_verif_expires", columnList = "expires_at"),
    @Index(name = "idx_email_verif_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Utilisateur concerné par la vérification.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Email à vérifier.
     */
    @Column(nullable = false, length = 255)
    private String email;
    
    /**
     * Token de vérification unique.
     */
    @Column(nullable = false, unique = true, length = 256)
    private String token;
    
    /**
     * Statut de la vérification.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EmailVerificationStatus status = EmailVerificationStatus.PENDING;
    
    /**
     * Date d'expiration du token.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Date de vérification.
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    /**
     * Adresse IP ayant demandé la vérification.
     */
    @Column(name = "request_ip", length = 45)
    private String requestIp;
    
    /**
     * Adresse IP ayant vérifié l'email.
     */
    @Column(name = "verified_ip", length = 45)
    private String verifiedIp;
    
    /**
     * Nombre de tentatives d'envoi.
     */
    @Column(name = "send_attempts")
    @Builder.Default
    private Integer sendAttempts = 0;
    
    /**
     * Date du dernier envoi.
     */
    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;
    
    /**
     * Date de création.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Vérifie si le token est expiré.
     * 
     * @return true si expiré, false sinon
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Vérifie si la vérification peut encore être utilisée.
     * 
     * @return true si utilisable, false sinon
     */
    public boolean isUsable() {
        return status == EmailVerificationStatus.PENDING && !isExpired();
    }
    
    /**
     * Marque l'email comme vérifié.
     * 
     * @param verifiedFromIp IP ayant vérifié
     */
    public void markAsVerified(String verifiedFromIp) {
        this.status = EmailVerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedIp = verifiedFromIp;
    }
    
    /**
     * Marque comme expiré.
     */
    public void markAsExpired() {
        this.status = EmailVerificationStatus.EXPIRED;
    }
    
    /**
     * Marque comme invalide.
     */
    public void markAsInvalid() {
        this.status = EmailVerificationStatus.INVALID;
    }
    
    /**
     * Incrémente le compteur d'envois.
     */
    public void incrementSendAttempts() {
        this.sendAttempts++;
        this.lastSentAt = LocalDateTime.now();
    }
}
