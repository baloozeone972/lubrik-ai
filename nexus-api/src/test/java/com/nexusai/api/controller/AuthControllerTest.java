package com.nexusai.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.auth.dto.AuthRequest;
import com.nexusai.auth.dto.AuthResponse;
import com.nexusai.auth.dto.RegisterRequest;
import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.auth.service.AuthenticationService;
import com.nexusai.auth.service.EmailVerificationService;
import com.nexusai.commons.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    private UUID userId;
    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockAuthResponse = AuthResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        @WithMockUser
        void shouldRegisterUserSuccessfully() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setUsername("newuser");
            request.setPassword("Password123!");

            when(authenticationService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));

            verify(authenticationService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid email format")
        @WithMockUser
        void shouldReturn400ForInvalidEmail() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("invalid-email");
            request.setUsername("newuser");
            request.setPassword("Password123!");

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authenticationService, never()).register(any());
        }

        @Test
        @DisplayName("Should return 400 for empty password")
        @WithMockUser
        void shouldReturn400ForEmptyPassword() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");
            request.setUsername("newuser");
            request.setPassword("");

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when email already exists")
        @WithMockUser
        void shouldReturn409WhenEmailExists() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setUsername("newuser");
            request.setPassword("Password123!");

            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenThrow(new BusinessException("EMAIL_EXISTS", "Email already registered"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        @WithMockUser
        void shouldLoginSuccessfully() throws Exception {
            AuthRequest request = new AuthRequest();
            request.setEmail("test@example.com");
            request.setPassword("Password123!");

            when(authenticationService.authenticate(any(AuthRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"));

            verify(authenticationService).authenticate(any(AuthRequest.class));
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials")
        @WithMockUser
        void shouldReturn401ForInvalidCredentials() throws Exception {
            AuthRequest request = new AuthRequest();
            request.setEmail("test@example.com");
            request.setPassword("wrongpassword");

            when(authenticationService.authenticate(any(AuthRequest.class)))
                    .thenThrow(new BusinessException("INVALID_CREDENTIALS", "Invalid email or password"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        @WithMockUser
        void shouldRefreshTokenSuccessfully() throws Exception {
            when(authenticationService.refreshToken("valid-refresh-token")).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("mock-access-token"));

            verify(authenticationService).refreshToken("valid-refresh-token");
        }

        @Test
        @DisplayName("Should return 401 for invalid refresh token")
        @WithMockUser
        void shouldReturn401ForInvalidRefreshToken() throws Exception {
            when(authenticationService.refreshToken("invalid-token"))
                    .thenThrow(new BusinessException("INVALID_TOKEN", "Invalid refresh token"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"invalid-token\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            UserPrincipal principal = new UserPrincipal(userId, "test@example.com", "USER");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .with(csrf())
                            .with(user(principal.getEmail()).roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"token-to-invalidate\"}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"some-token\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/verify-email")
    class VerifyEmailTests {

        @Test
        @DisplayName("Should verify email successfully")
        @WithMockUser
        void shouldVerifyEmailSuccessfully() throws Exception {
            doNothing().when(emailVerificationService).verifyEmail("valid-token");

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .with(csrf())
                            .param("token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email verified successfully"));

            verify(emailVerificationService).verifyEmail("valid-token");
        }

        @Test
        @DisplayName("Should return 400 for invalid verification token")
        @WithMockUser
        void shouldReturn400ForInvalidToken() throws Exception {
            doThrow(new BusinessException("INVALID_TOKEN", "Invalid or expired token"))
                    .when(emailVerificationService).verifyEmail("invalid-token");

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .with(csrf())
                            .param("token", "invalid-token"))
                    .andExpect(status().isBadRequest());
        }
    }
}
