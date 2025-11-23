package com.nexusai.api.controller;

import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.moderation.dto.ContentFlagDTO;
import com.nexusai.moderation.dto.CreateFlagRequest;
import com.nexusai.moderation.dto.ModerationActionDTO;
import com.nexusai.moderation.service.GDPRService;
import com.nexusai.moderation.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderation", description = "Content moderation and reporting endpoints")
public class ModerationController {

    private final ModerationService moderationService;
    private final GDPRService gdprService;

    // User-facing endpoints

    @PostMapping("/report")
    @Operation(summary = "Report content")
    public ResponseEntity<ContentFlagDTO> reportContent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateFlagRequest request) {
        ContentFlagDTO flag = moderationService.createFlag(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(flag);
    }

    @GetMapping("/my-reports")
    @Operation(summary = "Get my submitted reports")
    public ResponseEntity<Page<ContentFlagDTO>> getMyReports(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Page<ContentFlagDTO> reports = moderationService.getFlagsByStatus("all", pageable);
        return ResponseEntity.ok(reports);
    }

    // Admin/Moderator endpoints

    @GetMapping("/flags")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get pending flags (moderator only)")
    public ResponseEntity<Page<ContentFlagDTO>> getPendingFlags(Pageable pageable) {
        Page<ContentFlagDTO> flags = moderationService.getPendingFlags(pageable);
        return ResponseEntity.ok(flags);
    }

    @GetMapping("/flags/{flagId}")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get flag details (moderator only)")
    public ResponseEntity<ContentFlagDTO> getFlag(@PathVariable UUID flagId) {
        ContentFlagDTO flag = moderationService.getFlag(flagId);
        return ResponseEntity.ok(flag);
    }

    @PostMapping("/flags/{flagId}/review")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Review a flag (moderator only)")
    public ResponseEntity<ContentFlagDTO> reviewFlag(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID flagId,
            @RequestBody Map<String, String> request) {
        ContentFlagDTO flag = moderationService.reviewFlag(
                flagId,
                principal.getUserId(),
                request.get("resolution"),
                request.get("notes"));
        return ResponseEntity.ok(flag);
    }

    @PostMapping("/actions")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Take moderation action (moderator only)")
    public ResponseEntity<ModerationActionDTO> takeAction(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> request) {
        ModerationActionDTO action = moderationService.takeAction(
                principal.getUserId(),
                (String) request.get("targetType"),
                UUID.fromString((String) request.get("targetId")),
                (String) request.get("actionType"),
                (String) request.get("reason"),
                request.get("durationHours") != null ?
                        ((Number) request.get("durationHours")).intValue() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(action);
    }

    @DeleteMapping("/actions/{actionId}")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Revoke moderation action (moderator only)")
    public ResponseEntity<Void> revokeAction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID actionId,
            @RequestBody Map<String, String> request) {
        moderationService.revokeAction(actionId, principal.getUserId(), request.get("reason"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/actions")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get active moderation actions (moderator only)")
    public ResponseEntity<Page<ModerationActionDTO>> getActiveActions(
            @RequestParam(defaultValue = "user") String targetType,
            Pageable pageable) {
        Page<ModerationActionDTO> actions = moderationService.getActiveActions(targetType, pageable);
        return ResponseEntity.ok(actions);
    }

    @GetMapping("/actions/history/{targetId}")
    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get moderation history for target (moderator only)")
    public ResponseEntity<Page<ModerationActionDTO>> getActionHistory(
            @PathVariable UUID targetId,
            Pageable pageable) {
        Page<ModerationActionDTO> history = moderationService.getActionHistory(targetId, pageable);
        return ResponseEntity.ok(history);
    }

    // GDPR endpoints

    @PostMapping("/gdpr/export")
    @Operation(summary = "Request data export (GDPR)")
    public ResponseEntity<Map<String, Object>> requestDataExport(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID requestId = gdprService.requestDataExport(principal.getUserId());
        return ResponseEntity.ok(Map.of(
                "requestId", requestId,
                "message", "Your data export request has been submitted. You will receive an email when it's ready."
        ));
    }

    @PostMapping("/gdpr/delete")
    @Operation(summary = "Request account deletion (GDPR)")
    public ResponseEntity<Map<String, Object>> requestDataDeletion(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID requestId = gdprService.requestDataDeletion(principal.getUserId());
        return ResponseEntity.ok(Map.of(
                "requestId", requestId,
                "message", "Your account deletion request has been submitted. Your data will be deleted within 30 days."
        ));
    }

    @GetMapping("/gdpr/request/{requestId}")
    @Operation(summary = "Check GDPR request status")
    public ResponseEntity<Map<String, Object>> getGDPRRequestStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID requestId) {
        Map<String, Object> status = gdprService.getRequestStatus(requestId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/gdpr/data-summary")
    @Operation(summary = "Get summary of stored data")
    public ResponseEntity<Map<String, Object>> getDataSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> summary = gdprService.getUserDataSummary(principal.getUserId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/gdpr/consents")
    @Operation(summary = "Get user consent preferences")
    public ResponseEntity<Map<String, Boolean>> getConsents(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Boolean> consents = gdprService.getUserConsents(principal.getUserId());
        return ResponseEntity.ok(consents);
    }

    @PutMapping("/gdpr/consents")
    @Operation(summary = "Update consent preferences")
    public ResponseEntity<Void> updateConsents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Boolean> consents) {
        consents.forEach((type, granted) ->
                gdprService.recordConsent(principal.getUserId(), type, granted));
        return ResponseEntity.ok().build();
    }
}
