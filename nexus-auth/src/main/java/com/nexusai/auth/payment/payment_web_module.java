/**
 * ============================================================================
 * MODULE: payment-web
 * ============================================================================
 * Ce module contient les REST Controllers et la configuration Spring Boot.
 * 
 * DÉVELOPPEUR ASSIGNÉ: Developer 5 (Équipe API)
 * DÉPENDANCES: Tous les modules précédents
 * ============================================================================
 */

package com.nexusai.payment.web.controller;

import com.nexusai.payment.api.dto.*;
import com.nexusai.payment.api.request.*;
import com.nexusai.payment.api.response.*;
import com.nexusai.payment.application.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.UUID;

/**
 * Controller REST pour la gestion des abonnements.
 * 
 * <p><b>Endpoints:</b></p>
 * <ul>
 *   <li>POST /api/v1/subscriptions/subscribe - Créer un abonnement</li>
 *   <li>GET /api/v1/subscriptions/current - Récupérer l'abonnement actuel</li>
 *   <li>POST /api/v1/subscriptions/cancel - Annuler l'abonnement</li>
 *   <li>PUT /api/v1/subscriptions/update - Changer de plan</li>
 * </ul>
 * 
 * @author Developer 5
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Gestion des abonnements")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    /**
     * Crée ou met à jour un abonnement.
     * 
     * <p><b>Exemple de requête:</b></p>
     * <pre>{@code
     * POST /api/v1/subscriptions/subscribe
     * {
     *   "userId": "550e8400-e29b-41d4-a716-446655440000",
     *   "plan": "PREMIUM",
     *   "paymentMethodId": "pm_card_visa"
     * }
     * }</pre>
     * 
     * @param request Données de l'abonnement
     * @return Réponse avec l'abonnement créé
     */
    @PostMapping("/subscribe")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Créer un abonnement",
        description = "Crée un nouvel abonnement pour l'utilisateur. " +
                     "Le paiement est effectué immédiatement via Stripe."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Abonnement créé avec succès",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Requête invalide (utilisateur a déjà un abonnement actif)"
        ),
        @ApiResponse(
            responseCode = "402",
            description = "Erreur de paiement"
        )
    })
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        
        log.info("Requête de création d'abonnement: user={}, plan={}", 
            request.getUserId(), request.getPlan());
        
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Récupère l'abonnement actif de l'utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Abonnement actif ou 404
     */
    @GetMapping("/current")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Récupérer l'abonnement actuel",
        description = "Retourne l'abonnement actif de l'utilisateur authentifié."
    )
    public ResponseEntity<SubscriptionDTO> getCurrentSubscription(
            @RequestParam UUID userId) {
        
        log.info("Récupération abonnement pour user={}", userId);
        
        return subscriptionService.getActiveSubscription(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Annule un abonnement.
     * 
     * <p><b>Exemple:</b></p>
     * <pre>{@code
     * POST /api/v1/subscriptions/cancel
     * {
     *   "subscriptionId": "550e8400-e29b-41d4-a716-446655440000",
     *   "immediately": false,
     *   "reason": "Prix trop élevé"
     * }
     * }</pre>
     * 
     * @param request Détails de l'annulation
     * @return Abonnement mis à jour
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Annuler un abonnement",
        description = "Annule l'abonnement de l'utilisateur. " +
                     "Si immediately=true, annulation immédiate. " +
                     "Sinon, annulation à la fin de la période en cours."
    )
    public ResponseEntity<SubscriptionDTO> cancelSubscription(
            @Valid @RequestBody CancelSubscriptionRequest request) {
        
        log.info("Annulation abonnement: id={}, immediately={}", 
            request.getSubscriptionId(), request.getImmediately());
        
        SubscriptionDTO subscription = subscriptionService.cancelSubscription(request);
        
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Change le plan d'abonnement.
     * 
     * @param subscriptionId ID de l'abonnement
     * @param newPlan Nouveau plan
     * @return Abonnement mis à jour
     */
    @PutMapping("/{subscriptionId}/plan")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Changer de plan",
        description = "Upgrade ou downgrade vers un autre plan. " +
                     "Le changement est effectif immédiatement."
    )
    public ResponseEntity<SubscriptionDTO> updatePlan(
            @PathVariable UUID subscriptionId,
            @RequestParam SubscriptionPlan newPlan) {
        
        log.info("Changement de plan: subscription={}, newPlan={}", 
            subscriptionId, newPlan);
        
        SubscriptionDTO subscription = subscriptionService.updatePlan(
            subscriptionId, 
            newPlan
        );
        
        return ResponseEntity.ok(subscription);
    }
}

/**
 * Controller REST pour la gestion des jetons.
 * 
 * <p><b>Endpoints:</b></p>
 * <ul>
 *   <li>POST /api/v1/tokens/purchase - Acheter des jetons</li>
 *   <li>GET /api/v1/tokens/balance - Consulter le solde</li>
 *   <li>GET /api/v1/tokens/history - Historique des transactions</li>
 * </ul>
 * 
 * @author Developer 5
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tokens", description = "Gestion des jetons")
public class TokenController {
    
    private final TokenService tokenService;
    
    /**
     * Achète des jetons.
     * 
     * <p><b>Exemple:</b></p>
     * <pre>{@code
     * POST /api/v1/tokens/purchase
     * {
     *   "userId": "550e8400-e29b-41d4-a716-446655440000",
     *   "tokenAmount": 500,
     *   "paymentMethodId": "pm_card_visa"
     * }
     * }</pre>
     * 
     * @param request Détails de l'achat
     * @return Transaction créée
     */
    @PostMapping("/purchase")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Acheter des jetons",
        description = "Achète des jetons. Packs disponibles: " +
                     "100 jetons (4.99€), 500 jetons (19.99€), 1000 jetons (34.99€)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Jetons achetés avec succès"
        ),
        @ApiResponse(
            responseCode = "402",
            description = "Erreur de paiement"
        )
    })
    public ResponseEntity<TokenTransactionDTO> purchaseTokens(
            @Valid @RequestBody PurchaseTokensRequest request) {
        
        log.info("Achat de jetons: user={}, amount={}", 
            request.getUserId(), request.getTokenAmount());
        
        TokenTransactionDTO transaction = tokenService.purchaseTokens(request);
        
        return ResponseEntity.ok(transaction);
    }
    
    /**
     * Récupère le solde de jetons de l'utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Solde actuel
     */
    @GetMapping("/balance")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Consulter le solde",
        description = "Retourne le solde actuel de jetons de l'utilisateur."
    )
    public ResponseEntity<TokenBalanceResponse> getBalance(
            @RequestParam UUID userId) {
        
        log.info("Récupération solde pour user={}", userId);
        
        TokenBalanceResponse balance = tokenService.getBalance(userId);
        
        return ResponseEntity.ok(balance);
    }
    
    /**
     * Récupère l'historique des transactions de jetons.
     * 
     * @param userId ID de l'utilisateur
     * @param page Numéro de page (défaut: 0)
     * @param size Taille de page (défaut: 20)
     * @return Historique paginé
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Historique des transactions",
        description = "Retourne l'historique paginé des transactions de jetons."
    )
    public ResponseEntity<TokenHistoryResponse> getHistory(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Récupération historique: user={}, page={}", userId, page);
        
        TokenHistoryResponse history = tokenService.getHistory(userId, page, size);
        
        return ResponseEntity.ok(history);
    }
    
    /**
     * Endpoint interne pour consommer des jetons.
     * 
     * <p><b>Note:</b> Cet endpoint est appelé par les autres modules
     * (images, vidéo, etc.) pour débiter des jetons.</p>
     * 
     * @param request Détails de la consommation
     * @return Transaction créée
     */
    @PostMapping("/consume")
    @PreAuthorize("hasRole('SYSTEM') or hasRole('SERVICE')")
    @Operation(
        summary = "Consommer des jetons",
        description = "Endpoint interne pour débiter des jetons. " +
                     "Réservé aux appels inter-services."
    )
    public ResponseEntity<TokenTransactionDTO> consumeTokens(
            @Valid @RequestBody ConsumeTokensRequest request) {
        
        log.info("Consommation de jetons: user={}, amount={}, type={}", 
            request.getUserId(), request.getAmount(), request.getType());
        
        TokenTransactionDTO transaction = tokenService.consumeTokens(request);
        
        return ResponseEntity.ok(transaction);
    }
}

/**
 * Controller pour les webhooks Stripe.
 * 
 * <p>Traite les événements Stripe en temps réel.</p>
 * 
 * @author Developer 5
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Webhooks Stripe")
public class WebhookController {
    
    private final StripeService stripeService;
    private final WebhookService webhookService;
    
    /**
     * Endpoint webhook Stripe.
     * 
     * <p><b>Configuration Stripe:</b></p>
     * <ol>
     *   <li>Dashboard Stripe → Webhooks</li>
     *   <li>Ajouter endpoint: https://api.nexusai.com/api/v1/webhooks/stripe</li>
     *   <li>Sélectionner événements à écouter</li>
     *   <li>Copier le signing secret dans la config</li>
     * </ol>
     * 
     * @param payload Body de la requête Stripe
     * @param signatureHeader Header Stripe-Signature
     * @return 200 OK
     */
    @PostMapping("/stripe")
    @Operation(
        summary = "Webhook Stripe",
        description = "Endpoint pour recevoir les événements Stripe en temps réel."
    )
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) {
        
        log.info("Webhook Stripe reçu");
        
        try {
            // Valider signature et parser événement
            Event event = stripeService.handleWebhook(payload, signatureHeader);
            
            // Traiter selon le type d'événement
            webhookService.processEvent(event);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

// ============================================================================
// EXCEPTION HANDLER GLOBAL
// ============================================================================

package com.nexusai.payment.web.exception;

import com.nexusai.payment.api.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;

/**
 * Gestionnaire global des exceptions.
 * 
 * <p>Capture toutes les exceptions levées par les controllers
 * et retourne des réponses HTTP appropriées.</p>
 * 
 * @author Developer 5
 * @since 1.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Gère les erreurs de paiement.
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(
            PaymentException ex) {
        
        log.error("Erreur de paiement: {}", ex.getMessage());
        
        Map<String, Object> error = Map.of(
            "timestamp", Instant.now().toString(),
            "status", 402,
            "error", "Payment Required",
            "message", ex.getMessage(),
            "errorCode", ex.getErrorCode()
        );
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }
    
    /**
     * Gère les erreurs de jetons insuffisants.
     */
    @ExceptionHandler(InsufficientTokensException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientTokensException(
            InsufficientTokensException ex) {
        
        log.warn("Jetons insuffisants: {}", ex.getMessage());
        
        Map<String, Object> error = Map.of(
            "timestamp", Instant.now().toString(),
            "status", 402,
            "error", "Insufficient Tokens",
            "message", ex.getMessage(),
            "required", ex.getRequired(),
            "available", ex.getAvailable()
        );
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }
    
    /**
     * Gère les erreurs génériques.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex) {
        
        log.error("Erreur inattendue", ex);
        
        Map<String, Object> error = Map.of(
            "timestamp", Instant.now().toString(),
            "status", 500,
            "error", "Internal Server Error",
            "message", "Une erreur inattendue s'est produite"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

// ============================================================================
// APPLICATION PRINCIPALE
// ============================================================================

package com.nexusai.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application principale du service Payment.
 * 
 * <p><b>Modules:</b></p>
 * <ul>
 *   <li>payment-api: Contrats (DTOs, interfaces)</li>
 *   <li>payment-domain: Logique métier</li>
 *   <li>payment-infrastructure: Implémentations techniques</li>
 *   <li>payment-application: Services applicatifs</li>
 *   <li>payment-web: REST API</li>
 * </ul>
 * 
 * @author NexusAI Team
 * @since 1.0
 */
@SpringBootApplication
@EnableScheduling
public class PaymentServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

// ============================================================================
// CONFIGURATION APPLICATION
// ============================================================================

// application.yml
/**
 * Configuration de l'application Payment Service.
 * 
 * Variables d'environnement requises:
 * - STRIPE_API_KEY: Clé API Stripe
 * - STRIPE_WEBHOOK_SECRET: Secret webhook Stripe
 * - DATABASE_URL: URL de la base PostgreSQL
 * - KAFKA_BROKERS: URL des brokers Kafka
 */
/*
server:
  port: 8082

spring:
  application:
    name: payment-service
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/nexusai_payment}
    username: ${DATABASE_USER:nexusai}
    password: ${DATABASE_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

stripe:
  api:
    key: ${STRIPE_API_KEY}
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET}

logging:
  level:
    com.nexusai.payment: DEBUG
    org.springframework.kafka: INFO
*/