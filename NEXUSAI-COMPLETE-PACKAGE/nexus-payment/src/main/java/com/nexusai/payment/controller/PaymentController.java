package com.nexusai.payment.controller;

import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.payment.dto.CheckoutSessionDTO;
import com.nexusai.payment.service.StripeService;
import com.stripe.model.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller pour les paiements et abonnements Stripe.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final StripeService stripeService;

    /**
     * Crée une session de checkout Stripe.
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutSessionDTO> createCheckoutSession(
            @RequestParam("plan") String planStr,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        SubscriptionType plan = SubscriptionType.valueOf(planStr.toUpperCase());
        
        log.info("Creating checkout session for user: {} plan: {}", userId, plan);
        
        CheckoutSessionDTO session = stripeService.createCheckoutSession(userId, plan);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Webhook Stripe pour les événements.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        log.info("Received Stripe webhook");
        
        try {
            stripeService.handleWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok("Webhook handled");
        } catch (Exception e) {
            log.error("Error handling webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook error");
        }
    }

    /**
     * Annule l'abonnement de l'utilisateur.
     */
    @DeleteMapping("/subscription")
    public ResponseEntity<Map<String, String>> cancelSubscription(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        
        log.info("Canceling subscription for user: {}", userId);
        
        stripeService.cancelSubscription(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Subscription canceled successfully");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Change le plan d'abonnement.
     */
    @PutMapping("/subscription/upgrade")
    public ResponseEntity<Map<String, String>> upgradeSubscription(
            @RequestParam("plan") String planStr,
            Authentication authentication) {
        
        UUID userId = UUID.fromString(authentication.getName());
        SubscriptionType newPlan = SubscriptionType.valueOf(planStr.toUpperCase());
        
        log.info("Upgrading subscription for user: {} to: {}", userId, newPlan);
        
        stripeService.upgradeSubscription(userId, newPlan);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Subscription upgraded successfully");
        response.put("newPlan", newPlan.name());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les factures de l'utilisateur.
     */
    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> getUserInvoices(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        
        List<Invoice> invoices = stripeService.getUserInvoices(userId);
        
        return ResponseEntity.ok(invoices);
    }

    /**
     * Récupère les plans disponibles.
     */
    @GetMapping("/plans")
    public ResponseEntity<List<Map<String, Object>>> getAvailablePlans() {
        List<Map<String, Object>> plans = List.of(
                createPlanInfo("FREE", 0, "Free", "Basic features", 10, 1),
                createPlanInfo("STANDARD", 9.99, "Standard", "Standard features", 100, 3),
                createPlanInfo("PREMIUM", 19.99, "Premium", "Premium features", 1000, 10),
                createPlanInfo("VIP", 49.99, "VIP", "VIP features", -1, -1)
        );
        
        return ResponseEntity.ok(plans);
    }

    private Map<String, Object> createPlanInfo(String id, double price, String name, 
                                                String description, int messagesPerMonth, int companionsLimit) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("id", id);
        plan.put("name", name);
        plan.put("price", price);
        plan.put("interval", "month");
        plan.put("description", description);
        plan.put("messagesPerMonth", messagesPerMonth);
        plan.put("companionsLimit", companionsLimit);
        return plan;
    }
}
