package com.nexusai.api.controller;

import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.companion.dto.CompanionCreateRequest;
import com.nexusai.companion.dto.CompanionResponse;
import com.nexusai.companion.dto.CompanionUpdateRequest;
import com.nexusai.companion.service.CompanionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companions")
@RequiredArgsConstructor
@Tag(name = "Companions", description = "AI Companion management endpoints")
public class CompanionController {

    private final CompanionService companionService;

    @PostMapping
    @Operation(summary = "Create a new companion")
    public ResponseEntity<CompanionResponse> createCompanion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompanionCreateRequest request) {
        CompanionResponse response = companionService.createCompanion(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all user's companions")
    public ResponseEntity<List<CompanionResponse>> getUserCompanions(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CompanionResponse> companions = companionService.getUserCompanions(principal.getUserId());
        return ResponseEntity.ok(companions);
    }

    @GetMapping("/{companionId}")
    @Operation(summary = "Get companion by ID")
    public ResponseEntity<CompanionResponse> getCompanion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companionId) {
        CompanionResponse response = companionService.getCompanion(companionId, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{companionId}")
    @Operation(summary = "Update companion")
    public ResponseEntity<CompanionResponse> updateCompanion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companionId,
            @Valid @RequestBody CompanionUpdateRequest request) {
        CompanionResponse response = companionService.updateCompanion(
                companionId, principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{companionId}")
    @Operation(summary = "Delete companion")
    public ResponseEntity<Void> deleteCompanion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companionId) {
        companionService.deleteCompanion(companionId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public")
    @Operation(summary = "Get public companions")
    public ResponseEntity<Page<CompanionResponse>> getPublicCompanions(Pageable pageable) {
        Page<CompanionResponse> companions = companionService.getPublicCompanions(pageable);
        return ResponseEntity.ok(companions);
    }

    @GetMapping("/public/search")
    @Operation(summary = "Search public companions")
    public ResponseEntity<Page<CompanionResponse>> searchPublicCompanions(
            @RequestParam String query,
            Pageable pageable) {
        Page<CompanionResponse> companions = companionService.searchPublicCompanions(query, pageable);
        return ResponseEntity.ok(companions);
    }
}
