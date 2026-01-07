package com.nexusai.core.repository;

import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.enums.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ========== BASIC QUERIES ==========

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailAndAccountStatus(String email, AccountStatus status);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // ========== STRIPE QUERIES ==========
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("loginTime") LocalDateTime loginTime);

    @Modifying
    @Query("UPDATE User u SET u.tokensRemaining = u.tokensRemaining - :amount WHERE u.id = :userId AND u.tokensRemaining >= :amount")
    int consumeTokens(@Param("userId") UUID userId, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE User u SET u.tokensRemaining = u.tokensRemaining + :amount WHERE u.id = :userId")
    void addTokens(@Param("userId") UUID userId, @Param("amount") int amount);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    Optional<User> findByStripeSubscriptionId(String stripeSubscriptionId);

    // ========== ACCOUNT STATUS QUERIES ==========

    List<User> findByAccountStatus(AccountStatus accountStatus);

    @Query("SELECT u FROM User u WHERE u.accountStatus = :status AND u.createdAt < :beforeDate")
    List<User> findByAccountStatusAndCreatedBefore(
            @Param("status") AccountStatus status,
            @Param("beforeDate") java.time.LocalDateTime beforeDate);

    // ========== SUBSCRIPTION QUERIES ==========

    List<User> findBySubscriptionType(SubscriptionType subscriptionType);

    @Query("SELECT COUNT(u) FROM User u WHERE u.subscriptionType = :subscriptionType")
    long countBySubscriptionType(@Param("subscriptionType") SubscriptionType subscriptionType);

    @Query("SELECT u FROM User u WHERE u.subscriptionType <> 'FREE' AND u.accountStatus = 'ACTIVE'")
    List<User> findActivePayingUsers();

    // ========== EMAIL VERIFICATION QUERIES ==========

    List<User> findByEmailVerifiedFalseAndAccountStatus(AccountStatus accountStatus);

    // ========== TOKEN QUERIES ==========

    @Query("SELECT u FROM User u WHERE u.tokensRemaining < :threshold AND u.subscriptionType = 'FREE'")
    List<User> findLowTokenUsers(@Param("threshold") int threshold);

    // ========== ACTIVITY QUERIES ==========

    @Query("SELECT u FROM User u WHERE u.lastLoginAt > :since")
    List<User> findActiveUsersSince(@Param("since") java.time.LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt BETWEEN :start AND :end")
    long countActiveUsersBetween(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);
}
