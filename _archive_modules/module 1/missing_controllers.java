package com.nexusai.auth.controller;

import com.nexusai.auth.dto.request.SubscriptionRequest;
import com.nexusai.auth.dto.response.SubscriptionResponse;
import com.nexusai.auth.security.CustomUserDetails;
import com.nexusai.auth.service.SubscriptionService;
import com.nexusai.auth.service.TokenService;
import com.nexusai.core.domain.Subscription;
import com.nexusai.core.domain.TokenTransaction;
import com.nexusai.core.enums.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * ══════════════════════════════════════════════════════════════
 * SUBSCRIPTION CONTROLLER
 * ══════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Gestion des abonnements")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    /**
     * Récupère tous les plans disponibles.
     */
    @GetMapping("/plans")
    @Operation(summary = "Plans disponibles", description = "Liste tous les plans d'abonnement")
    public ResponseEntity<List<SubscriptionService.SubscriptionPlanInfo>> getAvailablePlans() {
        return ResponseEntity.ok(subscriptionService.getAvailablePlans());
    }
    
    /**
     * Récupère l'abonnement actuel de l'utilisateur.
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Abonnement actuel", description = "Récupère l'abonnement de l'utilisateur connecté")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Subscription subscription = subscriptionService.getUserSubscription(
            userDetails.getUser().getId()
        );
        
        return ResponseEntity.ok(subscriptionService.toResponse(subscription));
    }
    
    /**
     * Crée un nouvel abonnement.
     */
    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "S'abonner", description = "Crée un nouvel abonnement")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SubscriptionRequest request) {
        
        Subscription subscription = subscriptionService.createSubscription(
            userDetails.getUser().getId(),
            request
        );
        
        return ResponseEntity.ok(subscriptionService.toResponse(subscription));
    }
    
    /**
     * Upgrade l'abonnement.
     */
    @PostMapping("/upgrade")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upgrade", description = "Upgrade vers un plan supérieur")
    public ResponseEntity<SubscriptionResponse> upgrade(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String plan) {
        
        Subscription subscription = subscriptionService.upgradeSubscription(
            userDetails.getUser().getId(),
            plan
        );
        
        return ResponseEntity.ok(subscriptionService.toResponse(subscription));
    }
    
    /**
     * Downgrade l'abonnement.
     */
    @PostMapping("/downgrade")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Downgrade", description = "Downgrade vers un plan inférieur")
    public ResponseEntity<SubscriptionResponse> downgrade(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String plan) {
        
        Subscription subscription = subscriptionService.downgradeSubscription(
            userDetails.getUser().getId(),
            plan
        );
        
        return ResponseEntity.ok(subscriptionService.toResponse(subscription));
    }
    
    /**
     * Annule l'abonnement.
     */
    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Annuler", description = "Annule l'abonnement")
    public ResponseEntity<SubscriptionResponse> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "false") boolean immediately) {
        
        Subscription subscription = subscriptionService.cancelSubscription(
            userDetails.getUser().getId(),
            immediately
        );
        
        return ResponseEntity.ok(subscriptionService.toResponse(subscription));
    }
    
    /**
     * Réactive l'abonnement.
     */
    @PostMapping("/reactivate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Réactiver", description = "Réactive un abonnement annulé")
    public ResponseEntity<SubscriptionResponse> reactivate(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Subscription subscription = subscriptionService.reactivateSubscription(
            userDetails.getUser().getId()
        );
        
        return ResponseEntity.ok(subscriptionService.toResponse(subscription));
    }
}

/**
 * ══════════════════════════════════════════════════════════════
 * TOKEN CONTROLLER
 * ══════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
@Tag(name = "Tokens", description = "Gestion des jetons")
@SecurityRequirement(name = "bearerAuth")
public class TokenController {
    
    private final TokenService tokenService;
    
    /**
     * Récupère le solde de jetons.
     */
    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Solde", description = "Récupère le solde de jetons")
    public ResponseEntity<TokenBalanceResponse> getBalance(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        int balance = tokenService.getBalance(userDetails.getUser().getId());
        
        return ResponseEntity.ok(TokenBalanceResponse.builder()
                .balance(balance)
                .build());
    }
    
    /**
     * Récupère les statistiques de jetons.
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Statistiques", description = "Statistiques détaillées sur les jetons")
    public ResponseEntity<TokenService.TokenStatistics> getStatistics(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return ResponseEntity.ok(tokenService.getTokenStatistics(
            userDetails.getUser().getId()
        ));
    }
    
    /**
     * Achète des jetons.
     */
    @PostMapping("/purchase")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Acheter", description = "Achète des jetons")
    public ResponseEntity<TokenTransactionResponse> purchaseTokens(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int amount,
            @RequestParam String paymentReference) {
        
        TokenTransaction transaction = tokenService.purchaseTokens(
            userDetails.getUser().getId(),
            amount,
            paymentReference
        );
        
        return ResponseEntity.ok(toTransactionResponse(transaction));
    }
    
    /**
     * Récupère le bonus quotidien.
     */
    @PostMapping("/daily-bonus")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bonus quotidien", description = "Réclame le bonus quotidien")
    public ResponseEntity<TokenTransactionResponse> getDailyBonus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        TokenTransaction transaction = tokenService.getDailyBonus(
            userDetails.getUser().getId()
        );
        
        return ResponseEntity.ok(toTransactionResponse(transaction));
    }
    
    /**
     * Vérifie la disponibilité du bonus quotidien.
     */
    @GetMapping("/daily-bonus/available")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bonus disponible", description = "Vérifie si le bonus quotidien est disponible")
    public ResponseEntity<DailyBonusAvailability> checkDailyBonus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Duration timeUntilNext = tokenService.getTimeUntilNextBonus(
            userDetails.getUser().getId()
        );
        
        return ResponseEntity.ok(DailyBonusAvailability.builder()
                .available(timeUntilNext.isZero())
                .timeUntilNext(timeUntilNext.getSeconds())
                .build());
    }
    
    /**
     * Récupère l'historique des transactions.
     */
    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Historique", description = "Historique des transactions de jetons")
    public ResponseEntity<Page<TokenTransactionResponse>> getTransactionHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        
        Page<TokenTransaction> transactions = tokenService.getTransactionHistory(
            userDetails.getUser().getId(),
            pageable
        );
        
        return ResponseEntity.ok(transactions.map(this::toTransactionResponse));
    }
    
    /**
     * Récupère les transactions par type.
     */
    @GetMapping("/transactions/by-type")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Par type", description = "Transactions filtrées par type")
    public ResponseEntity<List<TokenTransactionResponse>> getTransactionsByType(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam TransactionType type) {
        
        List<TokenTransaction> transactions = tokenService.getTransactionsByType(
            userDetails.getUser().getId(),
            type
        );
        
        return ResponseEntity.ok(transactions.stream()
                .map(this::toTransactionResponse)
                .toList());
    }
    
    private TokenTransactionResponse toTransactionResponse(TokenTransaction tx) {
        return TokenTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
    
    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class TokenBalanceResponse {
        private int balance;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class TokenTransactionResponse {
        private UUID id;
        private String type;
        private int amount;
        private int balanceAfter;
        private String description;
        private java.time.LocalDateTime createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DailyBonusAvailability {
        private boolean available;
        private long timeUntilNext; // secondes
    }
}

/**
 * ══════════════════════════════════════════════════════════════
 * ADMIN CONTROLLER
 * ══════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administration")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final com.nexusai.auth.repository.UserRepository userRepository;
    private final com.nexusai.auth.repository.AuditLogRepository auditLogRepository;
    private final TokenService tokenService;
    
    /**
     * Liste tous les utilisateurs.
     */
    @GetMapping("/users")
    @Operation(summary = "Liste utilisateurs", description = "Liste paginée de tous les utilisateurs")
    public ResponseEntity<Page<com.nexusai.auth.dto.response.UserResponse>> getAllUsers(
            Pageable pageable) {
        
        Page<com.nexusai.core.domain.User> users = userRepository.findAll(pageable);
        
        // Mapper vers UserResponse
        return ResponseEntity.ok(users.map(user -> 
            com.nexusai.auth.dto.response.UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build()
        ));
    }
    
    /**
     * Récupère un utilisateur par ID.
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Détails utilisateur", description = "Détails complets d'un utilisateur")
    public ResponseEntity<com.nexusai.auth.dto.response.UserResponse> getUserById(
            @PathVariable UUID id) {
        
        com.nexusai.core.domain.User user = userRepository.findById(id)
                .orElseThrow(() -> new com.nexusai.core.exception.ResourceNotFoundException(
                    "Utilisateur non trouvé"
                ));
        
        return ResponseEntity.ok(
            com.nexusai.auth.dto.response.UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .birthDate(user.getBirthDate())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build()
        );
    }
    
    /**
     * Verrouille un compte utilisateur.
     */
    @PutMapping("/users/{id}/lock")
    @Operation(summary = "Verrouiller", description = "Verrouille un compte utilisateur")
    public ResponseEntity<Void> lockUser(@PathVariable UUID id) {
        com.nexusai.core.domain.User user = userRepository.findById(id)
                .orElseThrow(() -> new com.nexusai.core.exception.ResourceNotFoundException(
                    "Utilisateur non trouvé"
                ));
        
        user.setAccountLocked(true);
        user.setLockTime(java.time.LocalDateTime.now());
        userRepository.save(user);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Déverrouille un compte utilisateur.
     */
    @PutMapping("/users/{id}/unlock")
    @Operation(summary = "Déverrouiller", description = "Déverrouille un compte utilisateur")
    public ResponseEntity<Void> unlockUser(@PathVariable UUID id) {
        com.nexusai.core.domain.User user = userRepository.findById(id)
                .orElseThrow(() -> new com.nexusai.core.exception.ResourceNotFoundException(
                    "Utilisateur non trouvé"
                ));
        
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Accorde des jetons à un utilisateur.
     */
    @PostMapping("/tokens/grant")
    @Operation(summary = "Accorder jetons", description = "Accorde des jetons administrativement")
    public ResponseEntity<TokenController.TokenTransactionResponse> grantTokens(
            @RequestParam UUID userId,
            @RequestParam int amount,
            @RequestParam String reason) {
        
        TokenTransaction transaction = tokenService.addTokens(
            userId,
            amount,
            TransactionType.ADMIN_ADJUSTMENT,
            reason
        );
        
        return ResponseEntity.ok(
            TokenController.TokenTransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build()
        );
    }
    
    /**
     * Récupère les logs d'audit.
     */
    @GetMapping("/audit-logs")
    @Operation(summary = "Logs d'audit", description = "Historique des actions auditées")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(Pageable pageable) {
        Page<com.nexusai.core.domain.AuditLog> logs = auditLogRepository.findAll(pageable);
        
        return ResponseEntity.ok(logs.map(log -> 
            AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction().name())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .severity(log.getSeverity())
                .createdAt(log.getCreatedAt())
                .build()
        ));
    }
    
    /**
     * Récupère les statistiques globales.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques", description = "Statistiques globales de la plateforme")
    public ResponseEntity<GlobalStatistics> getGlobalStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(com.nexusai.core.domain.User::getActive)
                .count();
        
        return ResponseEntity.ok(GlobalStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .build());
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AuditLogResponse {
        private UUID id;
        private String action;
        private String entityType;
        private UUID entityId;
        private String description;
        private String ipAddress;
        private String severity;
        private java.time.LocalDateTime createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class GlobalStatistics {
        private long totalUsers;
        private long activeUsers;
    }
}
