package com.nexusai.core.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une demande de réinitialisation de mot de passe.
 * 
 * Gère le processus complet de récupération de mot de passe :
 * - Génération du token
 * - Vérification
 * - Expiration
 * - Suivi d'utilisation
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "password_resets", indexes = {
    @Index(name = "idx_pwd_reset_token", columnList = "token", unique = true),
    @Index(name = "idx_pwd_reset_user", columnList = "user_id"),
    @Index(name = "idx_pwd_reset_expires", columnList = "expires_at"),
    @Index(name = "idx_pwd_reset_used", columnList = "used")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordReset {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Utilisateur concerné par la réinitialisation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Token de réinitialisation unique.
     */
    @Column(nullable = false, unique = true, length = 256)
    private String token;
    
    /**
     * Date d'expiration du token.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Indique si le token a été utilisé.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;
    
    /**
     * Date d'utilisation du token.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    /**
     * Adresse IP ayant demandé la réinitialisation.
     */
    @Column(name = "request_ip", length = 45)
    private String requestIp;
    
    /**
     * Adresse IP ayant utilisé le token.
     */
    @Column(name = "used_ip", length = 45)
    private String usedIp;
    
    /**
     * User agent du client qui a demandé.
     */
    @Column(name = "request_user_agent", length = 500)
    private String requestUserAgent;
    
    /**
     * User agent du client qui a utilisé.
     */
    @Column(name = "used_user_agent", length = 500)
    private String usedUserAgent;
    
    /**
     * Nombre de tentatives d'utilisation invalides.
     */
    @Column(name = "invalid_attempts")
    @Builder.Default
    private Integer invalidAttempts = 0;
    
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
     * Vérifie si le token est valide (non utilisé et non expiré).
     * 
     * @return true si valide, false sinon
     */
    public boolean isValid() {
        return !used && !isExpired();
    }
    
    /**
     * Marque le token comme utilisé.
     * 
     * @param usedFromIp IP ayant utilisé le token
     * @param userAgent User agent du client
     */
    public void markAsUsed(String usedFromIp, String userAgent) {
        this.used = true;
        this.usedAt = LocalDateTime.now();
        this.usedIp = usedFromIp;
        this.usedUserAgent = userAgent;
    }
    
    /**
     * Incrémente le compteur de tentatives invalides.
     */
    public void incrementInvalidAttempts() {
        this.invalidAttempts++;
    }
    
    /**
     * Vérifie si le nombre maximum de tentatives est atteint.
     * 
     * @param maxAttempts Nombre maximum de tentatives autorisées
     * @return true si le maximum est atteint, false sinon
     */
    public boolean hasReachedMaxAttempts(int maxAttempts) {
        return this.invalidAttempts >= maxAttempts;
    }
}
