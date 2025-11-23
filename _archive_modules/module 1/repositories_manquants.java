package com.nexusai.auth.repository;

import com.nexusai.core.domain.*;
import com.nexusai.core.enums.AuditAction;
import com.nexusai.core.enums.EmailVerificationStatus;
import com.nexusai.core.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité TokenTransaction.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Repository
public interface TokenTransactionRepository extends JpaRepository<TokenTransaction, UUID> {
    
    /**
     * Trouve toutes les transactions d'un portefeuille.
     */
    Page<TokenTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
    
    /**
     * Trouve les transactions par type.
     */
    List<TokenTransaction> findByWalletIdAndType(UUID walletId, TransactionType type);
    
    /**
     * Calcule le total dépensé dans une période.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TokenTransaction t " +
           "WHERE t.wallet.id = :walletId " +
           "AND t.type = 'SPEND' " +
           "AND t.createdAt BETWEEN :startDate AND :endDate")
    Integer calculateTotalSpent(
        @Param("walletId") UUID walletId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Trouve les transactions liées à une référence.
     */
    List<TokenTransaction> findByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}

// =========================================================================

/**
 * Repository pour l'entité RefreshToken.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    /**
     * Trouve un token par sa valeur.
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Trouve tous les tokens d'un utilisateur.
     */
    List<RefreshToken> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Trouve les tokens valides d'un utilisateur.
     */
    @Query("SELECT rt FROM RefreshToken rt " +
           "WHERE rt.user.id = :userId " +
           "AND rt.revoked = false " +
           "AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUserId(
        @Param("userId") UUID userId,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Révoque tous les tokens d'un utilisateur.
     */
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now " +
           "WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllUserTokens(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    /**
     * Supprime les tokens expirés.
     */
    void deleteByExpiresAtBefore(LocalDateTime date);
}

// =========================================================================

/**
 * Repository pour l'entité EmailVerification.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    
    /**
     * Trouve une vérification par token.
     */
    Optional<EmailVerification> findByToken(String token);
    
    /**
     * Trouve les vérifications actives d'un utilisateur.
     */
    List<EmailVerification> findByUserIdAndStatus(UUID userId, EmailVerificationStatus status);
    
    /**
     * Trouve une vérification active par email.
     */
    @Query("SELECT ev FROM EmailVerification ev " +
           "WHERE ev.email = :email " +
           "AND ev.status = 'PENDING' " +
           "AND ev.expiresAt > :now " +
           "ORDER BY ev.createdAt DESC")
    Optional<EmailVerification> findActiveByEmail(
        @Param("email") String email,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Marque comme expirées les vérifications périmées.
     */
    @Query("UPDATE EmailVerification ev SET ev.status = 'EXPIRED' " +
           "WHERE ev.status = 'PENDING' AND ev.expiresAt < :now")
    void markExpiredVerifications(@Param("now") LocalDateTime now);
    
    /**
     * Supprime les anciennes vérifications.
     */
    void deleteByCreatedAtBefore(LocalDateTime date);
}

// =========================================================================

/**
 * Repository pour l'entité PasswordReset.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> {
    
    /**
     * Trouve une réinitialisation par token.
     */
    Optional<PasswordReset> findByToken(String token);
    
    /**
     * Trouve les réinitialisations actives d'un utilisateur.
     */
    @Query("SELECT pr FROM PasswordReset pr " +
           "WHERE pr.user.id = :userId " +
           "AND pr.used = false " +
           "AND pr.expiresAt > :now " +
           "ORDER BY pr.createdAt DESC")
    List<PasswordReset> findActiveByUserId(
        @Param("userId") UUID userId,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Compte les réinitialisations demandées récemment.
     */
    @Query("SELECT COUNT(pr) FROM PasswordReset pr " +
           "WHERE pr.user.id = :userId " +
           "AND pr.createdAt > :since")
    long countRecentResetsByUserId(
        @Param("userId") UUID userId,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Marque comme utilisées les anciennes réinitialisations d'un utilisateur.
     */
    @Query("UPDATE PasswordReset pr SET pr.used = true " +
           "WHERE pr.user.id = :userId AND pr.used = false")
    void invalidateAllUserResets(@Param("userId") UUID userId);
    
    /**
     * Supprime les anciennes réinitialisations.
     */
    void deleteByCreatedAtBefore(LocalDateTime date);
}

// =========================================================================

/**
 * Repository pour l'entité AuditLog.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    /**
     * Trouve les logs d'un utilisateur.
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    /**
     * Trouve les logs par action.
     */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);
    
    /**
     * Trouve les logs par type d'entité.
     */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        String entityType,
        UUID entityId,
        Pageable pageable
    );
    
    /**
     * Trouve les logs par sévérité.
     */
    Page<AuditLog> findBySeverityOrderByCreatedAtDesc(String severity, Pageable pageable);
    
    /**
     * Trouve les logs dans une période.
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Compte les tentatives de connexion échouées récentes.
     */
    @Query("SELECT COUNT(al) FROM AuditLog al " +
           "WHERE al.action = 'USER_LOGIN_FAILED' " +
           "AND al.ipAddress = :ipAddress " +
           "AND al.createdAt > :since")
    long countRecentFailedLogins(
        @Param("ipAddress") String ipAddress,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Trouve les activités suspectes.
     */
    @Query("SELECT al FROM AuditLog al " +
           "WHERE al.severity IN ('ERROR', 'CRITICAL') " +
           "AND al.createdAt > :since " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findSuspiciousActivities(
        @Param("since") LocalDateTime since
    );
}
