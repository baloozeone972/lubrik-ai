package com.nexusai.payment.service;

import com.nexusai.commons.exception.BusinessException;
import com.nexusai.payment.dto.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${stripe.secret-key:sk_test_placeholder}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:whsec_placeholder}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public SubscriptionResponse createSubscription(UUID userId, CreateSubscriptionRequest request) {
        try {
            // Get or create Stripe customer
            String customerId = getOrCreateCustomer(userId, request.getEmail());

            // Attach payment method
            PaymentMethod paymentMethod = PaymentMethod.retrieve(request.getPaymentMethodId());
            paymentMethod.attach(PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build());

            // Set as default payment method
            Customer.retrieve(customerId).update(CustomerUpdateParams.builder()
                    .setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
                            .setDefaultPaymentMethod(request.getPaymentMethodId())
                            .build())
                    .build());

            // Create subscription
            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(request.getPriceId())
                            .build())
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                            .setSaveDefaultPaymentMethod(
                                    SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                            .build())
                    .addExpand("latest_invoice.payment_intent")
                    .build();

            Subscription subscription = Subscription.create(params);

            log.info("Subscription created: {} for user {}", subscription.getId(), userId);

            return mapToResponse(subscription);
        } catch (StripeException e) {
            log.error("Stripe error creating subscription", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to create subscription: " + e.getMessage());
        }
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(UUID userId, String subscriptionId, boolean immediately) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);

            if (immediately) {
                subscription = subscription.cancel();
            } else {
                subscription = subscription.update(SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build());
            }

            log.info("Subscription {} cancelled for user {}", subscriptionId, userId);

            return mapToResponse(subscription);
        } catch (StripeException e) {
            log.error("Stripe error cancelling subscription", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to cancel subscription: " + e.getMessage());
        }
    }

    @Transactional
    public SubscriptionResponse updateSubscription(UUID userId, String subscriptionId, String newPriceId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            SubscriptionItem item = subscription.getItems().getData().get(0);

            subscription = subscription.update(SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(item.getId())
                            .setPrice(newPriceId)
                            .build())
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                    .build());

            log.info("Subscription {} updated to new plan for user {}", subscriptionId, userId);

            return mapToResponse(subscription);
        } catch (StripeException e) {
            log.error("Stripe error updating subscription", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to update subscription: " + e.getMessage());
        }
    }

    public SubscriptionResponse getSubscription(String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            return mapToResponse(subscription);
        } catch (StripeException e) {
            log.error("Stripe error retrieving subscription", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to retrieve subscription: " + e.getMessage());
        }
    }

    public List<InvoiceDTO> getInvoices(UUID userId, int limit) {
        try {
            String customerId = getCustomerId(userId);
            if (customerId == null) {
                return Collections.emptyList();
            }

            InvoiceListParams params = InvoiceListParams.builder()
                    .setCustomer(customerId)
                    .setLimit((long) limit)
                    .build();

            return Invoice.list(params).getData().stream()
                    .map(this::mapInvoice)
                    .toList();
        } catch (StripeException e) {
            log.error("Stripe error listing invoices", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to list invoices: " + e.getMessage());
        }
    }

    public PaymentMethodDTO addPaymentMethod(UUID userId, String paymentMethodId) {
        try {
            String customerId = getCustomerId(userId);
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            paymentMethod.attach(PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build());

            return mapPaymentMethod(paymentMethod);
        } catch (StripeException e) {
            log.error("Stripe error adding payment method", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to add payment method: " + e.getMessage());
        }
    }

    public List<PaymentMethodDTO> getPaymentMethods(UUID userId) {
        try {
            String customerId = getCustomerId(userId);
            if (customerId == null) {
                return Collections.emptyList();
            }

            PaymentMethodListParams params = PaymentMethodListParams.builder()
                    .setCustomer(customerId)
                    .setType(PaymentMethodListParams.Type.CARD)
                    .build();

            return PaymentMethod.list(params).getData().stream()
                    .map(this::mapPaymentMethod)
                    .toList();
        } catch (StripeException e) {
            log.error("Stripe error listing payment methods", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to list payment methods: " + e.getMessage());
        }
    }

    public void deletePaymentMethod(UUID userId, String paymentMethodId) {
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            paymentMethod.detach();
            log.info("Payment method {} removed for user {}", paymentMethodId, userId);
        } catch (StripeException e) {
            log.error("Stripe error deleting payment method", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to delete payment method: " + e.getMessage());
        }
    }

    public SetupIntentDTO createSetupIntent(UUID userId) {
        try {
            String customerId = getOrCreateCustomer(userId, null);

            SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                    .setCustomer(customerId)
                    .addPaymentMethodType("card")
                    .build();

            SetupIntent setupIntent = SetupIntent.create(params);

            return SetupIntentDTO.builder()
                    .clientSecret(setupIntent.getClientSecret())
                    .build();
        } catch (StripeException e) {
            log.error("Stripe error creating setup intent", e);
            throw new BusinessException("PAYMENT_ERROR", "Failed to create setup intent: " + e.getMessage());
        }
    }

    private String getOrCreateCustomer(UUID userId, String email) throws StripeException {
        // In production, would look up customer from database
        String customerId = getCustomerId(userId);

        if (customerId == null) {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("user_id", userId.toString())
                    .build();

            Customer customer = Customer.create(params);
            customerId = customer.getId();

            // Would save to database
            log.info("Created Stripe customer {} for user {}", customerId, userId);
        }

        return customerId;
    }

    private String getCustomerId(UUID userId) {
        // In production, would look up from database
        return null;
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .cancelAtPeriodEnd(subscription.getCancelAtPeriodEnd())
                .build();
    }

    private InvoiceDTO mapInvoice(Invoice invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .number(invoice.getNumber())
                .status(invoice.getStatus())
                .amountDue(invoice.getAmountDue())
                .amountPaid(invoice.getAmountPaid())
                .currency(invoice.getCurrency())
                .pdfUrl(invoice.getInvoicePdf())
                .createdAt(invoice.getCreated())
                .build();
    }

    private PaymentMethodDTO mapPaymentMethod(PaymentMethod pm) {
        PaymentMethod.Card card = pm.getCard();
        return PaymentMethodDTO.builder()
                .id(pm.getId())
                .brand(card.getBrand())
                .last4(card.getLast4())
                .expMonth(card.getExpMonth().intValue())
                .expYear(card.getExpYear().intValue())
                .isDefault(false) // Would check from customer default
                .build();
    }
}
