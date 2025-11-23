package com.nexusai.api.controller;

import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.core.dto.UserProfileDTO;
import com.nexusai.core.dto.UpdateProfileRequest;
import com.nexusai.core.dto.UserPreferencesDTO;
import com.nexusai.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and preferences endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<UserProfileDTO> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileDTO profile = userService.getProfile(principal.getUserId());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's profile")
    public ResponseEntity<UserProfileDTO> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileDTO profile = userService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me/preferences")
    @Operation(summary = "Get user preferences")
    public ResponseEntity<UserPreferencesDTO> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserPreferencesDTO preferences = userService.getPreferences(principal.getUserId());
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/me/preferences")
    @Operation(summary = "Update user preferences")
    public ResponseEntity<UserPreferencesDTO> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserPreferencesDTO preferences) {
        UserPreferencesDTO updated = userService.updatePreferences(principal.getUserId(), preferences);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        userService.changePassword(
                principal.getUserId(),
                request.get("currentPassword"),
                request.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/me/deactivate")
    @Operation(summary = "Deactivate account")
    public ResponseEntity<Map<String, String>> deactivateAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        userService.deactivateAccount(principal.getUserId(), request.get("password"));
        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully"));
    }

    @GetMapping("/me/usage")
    @Operation(summary = "Get usage statistics")
    public ResponseEntity<Map<String, Object>> getUsageStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> usage = userService.getUsageStats(principal.getUserId());
        return ResponseEntity.ok(usage);
    }

    @GetMapping("/me/subscription")
    @Operation(summary = "Get subscription info")
    public ResponseEntity<Map<String, Object>> getSubscriptionInfo(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> subscription = userService.getSubscriptionInfo(principal.getUserId());
        return ResponseEntity.ok(subscription);
    }
}
