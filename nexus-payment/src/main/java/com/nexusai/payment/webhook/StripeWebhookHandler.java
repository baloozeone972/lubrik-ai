package com.nexusai.payment.webhook;

import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookHandler {

    @Value("${stripe.webhook-secret:whsec_placeholder}")
    private String webhookSecret;

    public void handleWebhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);

            log.info("Received Stripe webhook: {}", event.getType());

            switch (event.getType()) {
                case "customer.subscription.created" -> handleSubscriptionCreated(event);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
                case "invoice.paid" -> handleInvoicePaid(event);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
                case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentFailed(event);
                default -> log.debug("Unhandled webhook event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error handling Stripe webhook", e);
            throw new RuntimeException("Webhook handling failed", e);
        }
    }

    private void handleSubscriptionCreated(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (subscription != null) {
            log.info("Subscription created: {}", subscription.getId());
            // Update database with subscription details
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (subscription != null) {
            log.info("Subscription updated: {} - status: {}", subscription.getId(), subscription.getStatus());
            // Update database with new status
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (subscription != null) {
            log.info("Subscription cancelled: {}", subscription.getId());
            // Update user to free tier
        }
    }

    private void handleInvoicePaid(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (invoice != null) {
            log.info("Invoice paid: {} - amount: {}", invoice.getId(), invoice.getAmountPaid());
            // Record payment transaction
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (invoice != null) {
            log.warn("Invoice payment failed: {}", invoice.getId());
            // Notify user of payment failure
        }
    }

    private void handlePaymentSucceeded(Event event) {
        log.info("Payment succeeded");
        // Record successful payment
    }

    private void handlePaymentFailed(Event event) {
        log.warn("Payment failed");
        // Handle payment failure, notify user
    }
}
