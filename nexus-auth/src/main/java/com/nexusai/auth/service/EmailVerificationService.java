package com.nexusai.auth.service;

import com.nexusai.commons.exception.BusinessException;
import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String VERIFICATION_TOKEN_PREFIX = "email_verification:";
    private static final Duration TOKEN_EXPIRATION = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${nexusai.app.url}")
    private String appUrl;

    @Value("${spring.mail.username:noreply@nexusai.com}")
    private String fromEmail;

    public void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();

        // Store token in Redis
        String key = VERIFICATION_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, user.getId().toString(), TOKEN_EXPIRATION);

        // Send email
        String verificationLink = appUrl + "/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("NexusAI - Vérifiez votre email");
        message.setText(String.format(
                "Bonjour %s,\n\n" +
                "Bienvenue sur NexusAI !\n\n" +
                "Cliquez sur le lien suivant pour vérifier votre email:\n%s\n\n" +
                "Ce lien expire dans 24 heures.\n\n" +
                "L'équipe NexusAI",
                user.getUsername(), verificationLink
        ));

        try {
            mailSender.send(message);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        String key = VERIFICATION_TOKEN_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            throw new BusinessException("INVALID_TOKEN", "Invalid or expired verification token");
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        user.setEmailVerified(true);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // Delete used token
        redisTemplate.delete(key);

        log.info("Email verified for user: {}", user.getEmail());
    }

    public void resendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        if (user.getEmailVerified()) {
            throw new BusinessException("ALREADY_VERIFIED", "Email is already verified");
        }

        sendVerificationEmail(user);
    }
}
