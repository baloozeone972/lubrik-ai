package com.nexusai.auth.service;

import com.nexusai.auth.dto.AuthRequest;
import com.nexusai.auth.dto.AuthResponse;
import com.nexusai.auth.dto.RegisterRequest;
import com.nexusai.auth.security.JwtTokenProvider;
import com.nexusai.commons.exception.BusinessException;
import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.core.enums.UserRole;
import com.nexusai.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
        //.id(userId)
                .email("test@example.com")
                .username("testuser")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.USER)
                .subscriptionType(SubscriptionType.FREE)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setUsername("newuser");
            request.setPassword("Password123!");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refreshToken");

            AuthResponse result = authenticationService.register(request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setUsername("newuser");
            request.setPassword("Password123!");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setUsername("existinguser");
            request.setPassword("Password123!");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("username");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfullyWithValidCredentials() {
            AuthRequest request = new AuthRequest();
            request.setEmail("test@example.com");
            request.setPassword("correctPassword");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("correctPassword", testUser.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refreshToken");

            AuthResponse result = authenticationService.authenticate(request);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
        }

        @Test
        @DisplayName("Should throw exception with invalid email")
        void shouldThrowExceptionWithInvalidEmail() {
            AuthRequest request = new AuthRequest();
            request.setEmail("nonexistent@example.com");
            request.setPassword("anyPassword");

            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("Should throw exception with wrong password")
        void shouldThrowExceptionWithWrongPassword() {
            AuthRequest request = new AuthRequest();
            request.setEmail("test@example.com");
            request.setPassword("wrongPassword");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", testUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("Should throw exception when account is suspended")
        void shouldThrowExceptionWhenAccountSuspended() {
            testUser.setAccountStatus(AccountStatus.SUSPENDED);
            AuthRequest request = new AuthRequest();
            request.setEmail("test@example.com");
            request.setPassword("correctPassword");

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("suspended");
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            String refreshToken = "validRefreshToken";

            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("newAccessToken");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("newRefreshToken");

            AuthResponse result = authenticationService.refreshToken(refreshToken);

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("newAccessToken");
        }

        @Test
        @DisplayName("Should throw exception with invalid refresh token")
        void shouldThrowExceptionWithInvalidRefreshToken() {
            String invalidToken = "invalidToken";

            when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

            assertThatThrownBy(() -> authenticationService.refreshToken(invalidToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid");
        }
    }
}
