package com.nexusai.payment.service;

import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.commons.exception.ValidationException;
import com.nexusai.core.entity.User;
import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.core.repository.UserRepository;
import com.nexusai.payment.dto.CheckoutSessionDTO;
import com.nexusai.payment.dto.SubscriptionDTO;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Service de gestion des paiements via Stripe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Value("${stripe.price-id.standard}")
    private String standardPriceId;

    @Value("${stripe.price-id.premium}")
    private String premiumPriceId;

    @Value("${stripe.price-id.vip}")
    private String vipPriceId;

    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe service initialized");
    }

    /**
     * Crée une session de checkout Stripe.
     */
    public CheckoutSessionDTO createCheckoutSession(UUID userId, SubscriptionType plan) {
        log.info("Creating Stripe checkout session for user: {} plan: {}", userId, plan);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        try {
            String priceId = getPriceIdForPlan(plan);

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setCustomerEmail(user.getEmail())
                    .setClientReferenceId(userId.toString())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setAllowPromotionCodes(true)
                    .putMetadata("userId", userId.toString())
                    .putMetadata("plan", plan.name())
                    .build();

            Session session = Session.create(params);

            log.info("Checkout session created: {}", session.getId());

            return CheckoutSessionDTO.builder()
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            log.error("Error creating Stripe checkout session", e);
            throw new RuntimeException("Failed to create checkout session: " + e.getMessage(), e);
        }
    }

    /**
     * Gère les webhooks Stripe.
     */
    @Transactional
    public void handleWebhookEvent(String payload, String sigHeader) {
        log.info("Handling Stripe webhook");

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            throw new ValidationException("INVALID_SIGNATURE", "Invalid webhook signature");
        }

        log.info("Webhook event received: {}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutCompleted(event);
                break;

            case "customer.subscription.created":
                handleSubscriptionCreated(event);
                break;

            case "customer.subscription.updated":
                handleSubscriptionUpdated(event);
                break;

            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;

            case "invoice.payment_succeeded":
                handlePaymentSucceeded(event);
                break;

            case "invoice.payment_failed":
                handlePaymentFailed(event);
                break;

            default:
                log.info("Unhandled event type: {}", event.getType());
        }
    }

    /**
     * Annule un abonnement.
     */
    @Transactional
    public void cancelSubscription(UUID userId) {
        log.info("Canceling subscription for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (user.getStripeSubscriptionId() == null) {
            throw new ValidationException("NO_SUBSCRIPTION", "User has no active subscription");
        }

        try {
            Subscription subscription = Subscription.retrieve(user.getStripeSubscriptionId());
            subscription.cancel();

            user.setSubscriptionType(SubscriptionType.FREE);
            user.setStripeSubscriptionId(null);
            userRepository.save(user);

            log.info("Subscription canceled for user: {}", userId);

        } catch (StripeException e) {
            log.error("Error canceling subscription", e);
            throw new RuntimeException("Failed to cancel subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Change le plan d'abonnement.
     */
    @Transactional
    public void upgradeSubscription(UUID userId, SubscriptionType newPlan) {
        log.info("Upgrading subscription for user: {} to: {}", userId, newPlan);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (user.getStripeSubscriptionId() == null) {
            throw new ValidationException("NO_SUBSCRIPTION", "User has no active subscription");
        }

        try {
            Subscription subscription = Subscription.retrieve(user.getStripeSubscriptionId());
            
            String newPriceId = getPriceIdForPlan(newPlan);
            String currentItemId = subscription.getItems().getData().get(0).getId();

            Map<String, Object> itemParams = new HashMap<>();
            itemParams.put("id", currentItemId);
            itemParams.put("price", newPriceId);

            Map<String, Object> params = new HashMap<>();
            params.put("items", Collections.singletonList(itemParams));
            params.put("proration_behavior", "always_invoice");

            subscription = subscription.update(params);

            user.setSubscriptionType(newPlan);
            userRepository.save(user);

            log.info("Subscription upgraded for user: {}", userId);

        } catch (StripeException e) {
            log.error("Error upgrading subscription", e);
            throw new RuntimeException("Failed to upgrade subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère les factures d'un utilisateur.
     */
    public List<Invoice> getUserInvoices(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        if (user.getStripeCustomerId() == null) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("customer", user.getStripeCustomerId());
            params.put("limit", 10);

            InvoiceCollection invoices = Invoice.list(params);
            return invoices.getData();

        } catch (StripeException e) {
            log.error("Error fetching invoices", e);
            return Collections.emptyList();
        }
    }

    /**
     * Traite la complétion d'un checkout.
     */
    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (session == null) return;

        String userId = session.getClientReferenceId();
        String subscriptionId = session.getSubscription();

        log.info("Checkout completed for user: {}", userId);

        try {
            User user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));

            // Récupérer le plan depuis les metadata
            String planStr = session.getMetadata().get("plan");
            SubscriptionType plan = SubscriptionType.valueOf(planStr);

            user.setSubscriptionType(plan);
            user.setStripeCustomerId(session.getCustomer());
            user.setStripeSubscriptionId(subscriptionId);
            userRepository.save(user);

            log.info("User {} upgraded to {}", userId, plan);

        } catch (Exception e) {
            log.error("Error processing checkout completion", e);
        }
    }

    /**
     * Traite la création d'un abonnement.
     */
    private void handleSubscriptionCreated(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        log.info("Subscription created: {}", subscription != null ? subscription.getId() : "null");
    }

    /**
     * Traite la mise à jour d'un abonnement.
     */
    private void handleSubscriptionUpdated(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        log.info("Subscription updated: {}", subscription != null ? subscription.getId() : "null");
    }

    /**
     * Traite la suppression d'un abonnement.
     */
    @Transactional
    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (subscription == null) return;

        log.info("Subscription deleted: {}", subscription.getId());

        // Trouver l'utilisateur et rétrograder
        User user = userRepository.findByStripeSubscriptionId(subscription.getId())
                .orElse(null);

        if (user != null) {
            user.setSubscriptionType(SubscriptionType.FREE);
            user.setStripeSubscriptionId(null);
            userRepository.save(user);
            log.info("User {} downgraded to FREE", user.getId());
        }
    }

    /**
     * Traite un paiement réussi.
     */
    private void handlePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        log.info("Payment succeeded for invoice: {}", invoice != null ? invoice.getId() : "null");
    }

    /**
     * Traite un paiement échoué.
     */
    @Transactional
    private void handlePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        
        if (invoice == null) return;

        log.warn("Payment failed for invoice: {}", invoice.getId());

        // Optionnel: Suspendre l'utilisateur après plusieurs échecs
        String customerId = invoice.getCustomer();
        User user = userRepository.findByStripeCustomerId(customerId).orElse(null);

        if (user != null) {
            // TODO: Implémenter logique de suspension
            log.warn("Payment failed for user: {}", user.getId());
        }
    }

    /**
     * Retourne le price ID Stripe pour un plan.
     */
    private String getPriceIdForPlan(SubscriptionType plan) {
        return switch (plan) {
            case STANDARD -> standardPriceId;
            case PREMIUM -> premiumPriceId;
            case VIP, VIP_PLUS -> vipPriceId;
            default -> throw new ValidationException("INVALID_PLAN", "Invalid subscription plan");
        };
    }
}
