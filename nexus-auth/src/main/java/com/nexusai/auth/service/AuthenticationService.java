package com.nexusai.auth.service;

import com.nexusai.auth.dto.AuthRequest;
import com.nexusai.auth.dto.AuthResponse;
import com.nexusai.auth.dto.RegisterRequest;
import com.nexusai.auth.security.JwtTokenProvider;
import com.nexusai.commons.exception.BusinessException;
import com.nexusai.commons.exception.ValidationException;
import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate unique constraints
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("email", "Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("username", "Username already taken");
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .accountStatus(AccountStatus.PENDING)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Send verification email
        emailVerificationService.sendVerificationEmail(user);

        // Generate tokens
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("AUTH_FAILED", "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("AUTH_FAILED", "Invalid email or password");
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new BusinessException("ACCOUNT_SUSPENDED", "Your account has been suspended");
        }

        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new BusinessException("ACCOUNT_DELETED", "This account no longer exists");
        }

        // Update last login
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
        log.info("User authenticated: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException("INVALID_TOKEN", "Invalid refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // Verify token is not revoked
        if (!refreshTokenService.isValidRefreshToken(userId, refreshToken)) {
            throw new BusinessException("TOKEN_REVOKED", "Refresh token has been revoked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        // Revoke old refresh token and generate new one
        refreshTokenService.revokeRefreshToken(userId, refreshToken);

        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(UUID userId, String refreshToken) {
        refreshTokenService.revokeRefreshToken(userId, refreshToken);
        log.info("User logged out: {}", userId);
    }

    @Transactional
    public void logoutAllDevices(UUID userId) {
        refreshTokenService.revokeAllTokens(userId);
        log.info("User logged out from all devices: {}", userId);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Store refresh token
        refreshTokenService.storeRefreshToken(user.getId(), refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole().name())
                .emailVerified(user.getEmailVerified())
                .build();
    }
}
