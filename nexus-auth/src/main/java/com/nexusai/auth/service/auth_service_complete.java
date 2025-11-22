package com.nexusai.auth.service.impl;

import com.nexusai.auth.dto.request.*;
import com.nexusai.auth.dto.response.MessageResponse;
import com.nexusai.auth.repository.*;
import com.nexusai.auth.security.JwtTokenProvider;
import com.nexusai.auth.service.EmailService;
import com.nexusai.core.domain.*;
import com.nexusai.core.enums.EmailVerificationStatus;
import com.nexusai.core.exception.ResourceNotFoundException;
import com.nexusai.core.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Extension du service d'authentification avec toutes les fonctionnalités.
 * 
 * Ajoute les méthodes manquantes :
 * - Vérification d'email
 * - Réinitialisation de mot de passe
 * - Changement de mot de passe
 * - Gestion des refresh tokens
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceComplete {
    
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;
    private static final int MAX_RESET_REQUESTS_PER_HOUR = 3;
    
    /**
     * Envoie un email de vérification.
     * 
     * @param userId ID de l'utilisateur
     * @return MessageResponse
     */
    @Transactional
    public MessageResponse sendVerificationEmail(UUID userId) {
        log.info("Envoi email de vérification pour l'utilisateur: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        if (user.getEmailVerified()) {
            return MessageResponse.builder()
                    .message("Email déjà vérifié")
                    .success(true)
                    .build();
        }
        
        // Invalider les anciennes vérifications
        List<EmailVerification> oldVerifications = 
            emailVerificationRepository.findByUserIdAndStatus(userId, EmailVerificationStatus.PENDING);
        oldVerifications.forEach(v -> v.markAsInvalid());
        emailVerificationRepository.saveAll(oldVerifications);
        
        // Créer nouvelle vérification
        String token = generateSecureToken();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .email(user.getEmail())
                .token(token)
                .status(EmailVerificationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        
        emailVerificationRepository.save(verification);
        
        // Envoyer l'email
        emailService.sendVerificationEmail(user, token);
        
        log.info("Email de vérification envoyé avec succès");
        
        return MessageResponse.builder()
                .message("Email de vérification envoyé")
                .success(true)
                .build();
    }
    
    /**
     * Vérifie un email avec le token.
     * 
     * @param token Token de vérification
     * @param ipAddress Adresse IP
     * @return MessageResponse
     */
    @Transactional
    public MessageResponse verifyEmail(String token, String ipAddress) {
        log.info("Vérification d'email avec token");
        
        EmailVerification verification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));
        
        if (!verification.isUsable()) {
            if (verification.isExpired()) {
                throw new IllegalArgumentException("Token expiré");
            }
            throw new IllegalArgumentException("Token déjà utilisé ou invalide");
        }
        
        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        
        verification.markAsVerified(ipAddress);
        emailVerificationRepository.save(verification);
        
        // Log audit
        auditLogRepository.save(
            AuditLog.builder()
                .user(user)
                .action(com.nexusai.core.enums.AuditAction.USER_EMAIL_VERIFIED)
                .entityType("User")
                .entityId(user.getId())
                .description("Email vérifié")
                .ipAddress(ipAddress)
                .result("SUCCESS")
                .build()
        );
        
        // Envoyer email de bienvenue
        emailService.sendWelcomeEmail(user);
        
        log.info("Email vérifié avec succès pour l'utilisateur: {}", user.getId());
        
        return MessageResponse.builder()
                .message("Email vérifié avec succès")
                .success(true)
                .build();
    }
    
    /**
     * Demande de réinitialisation de mot de passe.
     * 
     * @param request Requête de réinitialisation
     * @param ipAddress Adresse IP
     * @param userAgent User agent
     * @return MessageResponse
     */
    @Transactional
    public MessageResponse forgotPassword(
            ForgotPasswordRequest request,
            String ipAddress,
            String userAgent) {
        
        log.info("Demande de réinitialisation pour: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Aucun compte associé à cet email"
                ));
        
        // Vérifier le rate limiting
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentRequests = passwordResetRepository
                .countRecentResetsByUserId(user.getId(), oneHourAgo);
        
        if (recentRequests >= MAX_RESET_REQUESTS_PER_HOUR) {
            throw new IllegalStateException(
                "Trop de demandes. Veuillez réessayer plus tard."
            );
        }
        
        // Invalider les anciennes demandes
        passwordResetRepository.invalidateAllUserResets(user.getId());
        
        // Créer nouvelle demande
        String token = generateSecureToken();
        PasswordReset reset = PasswordReset.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .requestIp(ipAddress)
                .requestUserAgent(userAgent)
                .build();
        
        passwordResetRepository.save(reset);
        
        // Envoyer l'email
        emailService.sendPasswordResetEmail(user, token);
        
        // Log audit
        auditLogRepository.save(
            AuditLog.builder()
                .user(user)
                .action(com.nexusai.core.enums.AuditAction.USER_PASSWORD_RESET)
                .entityType("User")
                .entityId(user.getId())
                .description("Demande de réinitialisation de mot de passe")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result("SUCCESS")
                .build()
        );
        
        log.info("Email de réinitialisation envoyé");
        
        return MessageResponse.builder()
                .message("Email de réinitialisation envoyé")
                .success(true)
                .build();
    }
    
    /**
     * Réinitialise le mot de passe.
     * 
     * @param request Requête de réinitialisation
     * @param ipAddress Adresse IP
     * @param userAgent User agent
     * @return MessageResponse
     */
    @Transactional
    public MessageResponse resetPassword(
            ResetPasswordRequest request,
            String ipAddress,
            String userAgent) {
        
        log.info("Réinitialisation de mot de passe avec token");
        
        PasswordReset reset = passwordResetRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));
        
        if (!reset.isValid()) {
            if (reset.isExpired()) {
                throw new IllegalArgumentException("Token expiré");
            }
            throw new IllegalArgumentException("Token déjà utilisé");
        }
        
        User user = reset.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        reset.markAsUsed(ipAddress, userAgent);
        passwordResetRepository.save(reset);
        
        // Révoquer tous les tokens de l'utilisateur
        refreshTokenRepository.revokeAllUserTokens(user.getId(), LocalDateTime.now());
        
        // Envoyer confirmation
        emailService.sendPasswordChangeConfirmation(user);
        
        // Log audit
        auditLogRepository.save(
            AuditLog.builder()
                .user(user)
                .action(com.nexusai.core.enums.AuditAction.USER_PASSWORD_CHANGE)
                .entityType("User")
                .entityId(user.getId())
                .description("Mot de passe réinitialisé")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result("SUCCESS")
                .build()
        );
        
        log.info("Mot de passe réinitialisé avec succès");
        
        return MessageResponse.builder()
                .message("Mot de passe réinitialisé avec succès")
                .success(true)
                .build();
    }
    
    /**
     * Change le mot de passe de l'utilisateur connecté.
     * 
     * @param userId ID de l'utilisateur
     * @param request Requête de changement
     * @return MessageResponse
     */
    @Transactional
    public MessageResponse changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changement de mot de passe pour: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Mot de passe actuel incorrect");
        }
        
        // Vérifier que le nouveau est différent
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException(
                "Le nouveau mot de passe doit être différent de l'ancien"
            );
        }
        
        // Mettre à jour le mot de passe
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Révoquer tous les tokens
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        
        // Envoyer confirmation
        emailService.sendPasswordChangeConfirmation(user);
        
        // Log audit
        auditLogRepository.save(
            AuditLog.builder()
                .user(user)
                .action(com.nexusai.core.enums.AuditAction.USER_PASSWORD_CHANGE)
                .entityType("User")
                .entityId(userId)
                .description("Mot de passe changé")
                .result("SUCCESS")
                .build()
        );
        
        log.info("Mot de passe changé avec succès");
        
        return MessageResponse.builder()
                .message("Mot de passe changé avec succès")
                .success(true)
                .build();
    }
    
    /**
     * Génère un token sécurisé.
     * 
     * @return Token généré
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * DTO pour la requête de mot de passe oublié.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ForgotPasswordRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
        private String email;
    }
    
    /**
     * DTO pour la réinitialisation de mot de passe.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ResetPasswordRequest {
        @jakarta.validation.constraints.NotBlank
        private String token;
        
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(min = 8)
        @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial"
        )
        private String newPassword;
    }
    
    /**
     * DTO pour le changement de mot de passe.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChangePasswordRequest {
        @jakarta.validation.constraints.NotBlank
        private String currentPassword;
        
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(min = 8)
        @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$"
        )
        private String newPassword;
    }
    
    /**
     * DTO pour les réponses de messages.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String message;
        private boolean success;
    }
}
