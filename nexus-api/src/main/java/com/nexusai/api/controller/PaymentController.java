package com.nexusai.api.controller;

import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.payment.dto.*;
import com.nexusai.payment.service.PaymentService;
import com.nexusai.payment.webhook.StripeWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment and subscription management endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final StripeWebhookHandler webhookHandler;

    // Subscription endpoints

    @PostMapping("/subscriptions")
    @Operation(summary = "Create a new subscription")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse subscription = paymentService.createSubscription(
                principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Get subscription details")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subscriptionId) {
        SubscriptionResponse subscription = paymentService.getSubscription(subscriptionId);
        return ResponseEntity.ok(subscription);
    }

    @PutMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Update subscription plan")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subscriptionId,
            @RequestBody Map<String, String> request) {
        SubscriptionResponse subscription = paymentService.updateSubscription(
                principal.getUserId(), subscriptionId, request.get("priceId"));
        return ResponseEntity.ok(subscription);
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Cancel subscription")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subscriptionId,
            @RequestParam(defaultValue = "false") boolean immediately) {
        SubscriptionResponse subscription = paymentService.cancelSubscription(
                principal.getUserId(), subscriptionId, immediately);
        return ResponseEntity.ok(subscription);
    }

    // Payment methods

    @PostMapping("/payment-methods")
    @Operation(summary = "Add a payment method")
    public ResponseEntity<PaymentMethodDTO> addPaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        PaymentMethodDTO paymentMethod = paymentService.addPaymentMethod(
                principal.getUserId(), request.get("paymentMethodId"));
        return ResponseEntity.ok(paymentMethod);
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "Get payment methods")
    public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethods(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<PaymentMethodDTO> methods = paymentService.getPaymentMethods(principal.getUserId());
        return ResponseEntity.ok(methods);
    }

    @DeleteMapping("/payment-methods/{paymentMethodId}")
    @Operation(summary = "Remove a payment method")
    public ResponseEntity<Void> deletePaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String paymentMethodId) {
        paymentService.deletePaymentMethod(principal.getUserId(), paymentMethodId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/setup-intent")
    @Operation(summary = "Create a setup intent for adding payment method")
    public ResponseEntity<SetupIntentDTO> createSetupIntent(
            @AuthenticationPrincipal UserPrincipal principal) {
        SetupIntentDTO setupIntent = paymentService.createSetupIntent(principal.getUserId());
        return ResponseEntity.ok(setupIntent);
    }

    // Invoices

    @GetMapping("/invoices")
    @Operation(summary = "Get invoices")
    public ResponseEntity<List<InvoiceDTO>> getInvoices(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "10") int limit) {
        List<InvoiceDTO> invoices = paymentService.getInvoices(principal.getUserId(), limit);
        return ResponseEntity.ok(invoices);
    }

    // Webhook

    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook endpoint")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        webhookHandler.handleWebhook(payload, signature);
        return ResponseEntity.ok("Webhook received");
    }

    // Plans

    @GetMapping("/plans")
    @Operation(summary = "Get available subscription plans")
    public ResponseEntity<List<PlanDTO>> getPlans() {
        // Would fetch from database or Stripe
        List<PlanDTO> plans = List.of(
                PlanDTO.builder()
                        .id("free")
                        .name("Free")
                        .priceMonthly(0)
                        .features(List.of("1 AI Companion", "100 messages/day", "Basic support"))
                        .build(),
                PlanDTO.builder()
                        .id("starter")
                        .name("Starter")
                        .priceMonthly(999) // cents
                        .stripePriceId("price_starter_monthly")
                        .features(List.of("3 AI Companions", "1000 messages/day", "Voice messages", "Email support"))
                        .build(),
                PlanDTO.builder()
                        .id("pro")
                        .name("Professional")
                        .priceMonthly(2499)
                        .stripePriceId("price_pro_monthly")
                        .features(List.of("10 AI Companions", "Unlimited messages", "API access", "Priority support"))
                        .popular(true)
                        .build(),
                PlanDTO.builder()
                        .id("enterprise")
                        .name("Enterprise")
                        .priceMonthly(9999)
                        .stripePriceId("price_enterprise_monthly")
                        .features(List.of("Unlimited Companions", "Custom AI training", "SSO", "Dedicated support"))
                        .build()
        );
        return ResponseEntity.ok(plans);
    }
}
