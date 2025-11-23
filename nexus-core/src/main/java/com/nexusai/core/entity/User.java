package com.nexusai.core.entity;

import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.core.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User entity representing a platform user.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_status", columnList = "account_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank
    @Email
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    @Builder.Default
    private SubscriptionType subscriptionType = SubscriptionType.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "tokens_remaining", nullable = false)
    @Builder.Default
    private Integer tokensRemaining = 100;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "fr_FR";

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Europe/Paris";

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Checks if the user has an active subscription.
     */
    public boolean hasActiveSubscription() {
        return subscriptionType != SubscriptionType.FREE;
    }

    /**
     * Checks if the user can consume tokens.
     */
    public boolean canConsumeTokens(int amount) {
        return tokensRemaining >= amount || subscriptionType == SubscriptionType.VIP_PLUS;
    }

    /**
     * Consumes tokens from the user's balance.
     */
    public boolean consumeTokens(int amount) {
        if (subscriptionType == SubscriptionType.VIP_PLUS) {
            return true; // Unlimited
        }
        if (tokensRemaining >= amount) {
            tokensRemaining -= amount;
            return true;
        }
        return false;
    }

    /**
     * Adds tokens to the user's balance.
     */
    public void addTokens(int amount) {
        tokensRemaining += amount;
    }

    /**
     * Checks if the user is active.
     */
    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE && deletedAt == null;
    }

    /**
     * Checks if the user is an admin.
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Checks if the user is a moderator or admin.
     */
    public boolean isModerator() {
        return role == UserRole.MODERATOR || role == UserRole.ADMIN;
    }
}
