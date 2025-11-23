package com.nexusai.api.controller;

import com.nexusai.auth.dto.AuthRequest;
import com.nexusai.auth.dto.AuthResponse;
import com.nexusai.auth.dto.RegisterRequest;
import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.auth.service.AuthenticationService;
import com.nexusai.auth.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user registration endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        AuthResponse response = authenticationService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current session")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        authenticationService.logout(principal.getUserId(), refreshToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        authenticationService.logoutAllDevices(principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<Map<String, String>> resendVerification(
            @AuthenticationPrincipal UserPrincipal principal) {
        emailVerificationService.resendVerificationEmail(principal.getUserId());
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }
}
