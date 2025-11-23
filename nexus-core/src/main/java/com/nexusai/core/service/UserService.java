package com.nexusai.core.service;

import com.nexusai.commons.exception.BusinessException;
import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.core.dto.UpdateProfileRequest;
import com.nexusai.core.dto.UserPreferencesDTO;
import com.nexusai.core.dto.UserProfileDTO;
import com.nexusai.core.entity.User;
import com.nexusai.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        return mapToProfile(user);
    }

    @Transactional
    public UserProfileDTO updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (request.getUsername() != null) {
            // Check if username is taken
            userRepository.findByUsername(request.getUsername())
                    .filter(u -> !u.getId().equals(userId))
                    .ifPresent(u -> {
                        throw new BusinessException("USERNAME_TAKEN", "Username is already taken");
                    });
            user.setUsername(request.getUsername());
        }

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user {}", userId);

        return mapToProfile(user);
    }

    @Transactional(readOnly = true)
    public UserPreferencesDTO getPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // Would fetch from user_preferences table
        return UserPreferencesDTO.builder()
                .language("en")
                .theme("system")
                .notificationsEnabled(true)
                .emailNotifications(true)
                .marketingEmails(false)
                .defaultModel("llama3.2")
                .contextMessageLimit(20)
                .streamResponses(true)
                .showTypingIndicator(true)
                .build();
    }

    @Transactional
    public UserPreferencesDTO updatePreferences(UUID userId, UserPreferencesDTO preferences) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        // Would save to user_preferences table
        log.info("Preferences updated for user {}", userId);

        return preferences;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("INVALID_PASSWORD", "Current password is incorrect");
        }

        if (newPassword.length() < 8) {
            throw new BusinessException("WEAK_PASSWORD", "Password must be at least 8 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user {}", userId);
    }

    @Transactional
    public void deactivateAccount(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("INVALID_PASSWORD", "Password is incorrect");
        }

        user.setStatus(com.nexusai.core.enums.UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Account deactivated for user {}", userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUsageStats(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("tier", user.getTier().name());
        stats.put("companionsCount", 0); // Would fetch from DB
        stats.put("conversationsCount", 0);
        stats.put("messagesThisMonth", 0);
        stats.put("tokensUsedThisMonth", 0);
        stats.put("storageUsedMB", 0);

        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSubscriptionInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Map<String, Object> subscription = new HashMap<>();
        subscription.put("tier", user.getTier().name());
        subscription.put("status", "active");
        subscription.put("currentPeriodEnd", null);
        subscription.put("cancelAtPeriodEnd", false);

        // Would fetch from subscriptions table
        Map<String, Object> limits = new HashMap<>();
        limits.put("companions", getLimitForTier(user.getTier().name(), "companions"));
        limits.put("messagesPerDay", getLimitForTier(user.getTier().name(), "messages_per_day"));
        limits.put("tokensPerMonth", getLimitForTier(user.getTier().name(), "tokens_per_month"));

        subscription.put("limits", limits);
        return subscription;
    }

    private int getLimitForTier(String tier, String limitType) {
        return switch (tier.toUpperCase()) {
            case "FREE" -> switch (limitType) {
                case "companions" -> 1;
                case "messages_per_day" -> 100;
                case "tokens_per_month" -> 10000;
                default -> 0;
            };
            case "STARTER" -> switch (limitType) {
                case "companions" -> 3;
                case "messages_per_day" -> 1000;
                case "tokens_per_month" -> 100000;
                default -> 0;
            };
            case "PRO" -> switch (limitType) {
                case "companions" -> 10;
                case "messages_per_day" -> -1;
                case "tokens_per_month" -> 500000;
                default -> 0;
            };
            default -> switch (limitType) {
                case "companions" -> -1;
                case "messages_per_day" -> -1;
                case "tokens_per_month" -> -1;
                default -> 0;
            };
        };
    }

    private UserProfileDTO mapToProfile(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .tier(user.getTier().name())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
