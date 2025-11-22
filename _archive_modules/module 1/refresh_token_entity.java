package com.nexusai.core.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un refresh token pour l'authentification.
 * 
 * Les refresh tokens permettent de renouveler les access tokens sans
 * redemander les identifiants à l'utilisateur.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token", columnList = "token", unique = true),
    @Index(name = "idx_refresh_user", columnList = "user_id"),
    @Index(name = "idx_refresh_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Utilisateur propriétaire du token.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Valeur du refresh token (unique).
     */
    @Column(nullable = false, unique = true, length = 512)
    private String token;
    
    /**
     * Date d'expiration du token.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Indique si le token a été révoqué.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;
    
    /**
     * Date de révocation du token.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    /**
     * Raison de la révocation.
     */
    @Column(name = "revoke_reason")
    private String revokeReason;
    
    /**
     * Adresse IP ayant créé le token.
     */
    @Column(name = "created_from_ip", length = 45)
    private String createdFromIp;
    
    /**
     * User agent du client.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Date de dernière utilisation.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
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
     * Vérifie si le token est valide (non révoqué et non expiré).
     * 
     * @return true si valide, false sinon
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
    
    /**
     * Révoque le token.
     * 
     * @param reason Raison de la révocation
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
        this.revokeReason = reason;
    }
    
    /**
     * Met à jour la dernière utilisation.
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
