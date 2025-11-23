/**
 * ============================================================================
 * MODULE: payment-infrastructure
 * ============================================================================
 * Ce module contient les implémentations techniques :
 * - Intégration Stripe
 * - Repositories JPA
 * - Event Publishing (Kafka)
 * - Configuration
 * 
 * DÉVELOPPEURS ASSIGNÉS:
 * - Developer 3: Stripe Integration
 * - Developer 4: Kafka Events
 * 
 * DÉPENDANCES: payment-api, payment-domain
 * ============================================================================
 */

package com.nexusai.payment.infrastructure.stripe;

import com.stripe.Stripe;
import com.stripe.model.*;
import com.stripe.param.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;

/**
 * Service d'intégration avec Stripe.
 * 
 * <p><b>Responsabilités:</b></p>
 * <ul>
 *   <li>Créer des abonnements Stripe</li>
 *   <li>Gérer les PaymentIntents</li>
 *   <li>Gérer les webhooks</li>
 *   <li>Annuler/Modifier des abonnements</li>
 * </ul>
 * 
 * <p><b>Documentation Stripe:</b></p>
 * <ul>
 *   <li>https://stripe.com/docs/api/subscriptions</li>
 *   <li>https://stripe.com/docs/api/payment_intents</li>
 * </ul>
 * 
 * @author Developer 3
 * @since 1.0
 */
@Service
@Slf4j
public class StripeService {
    
    @Value("${stripe.api.key}")
    private String apiKey;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    /**
     * Initialise l'API Stripe avec la clé API.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
        log.info("Stripe API initialisée");
    }
    
    /**
     * Crée un nouvel abonnement Stripe.
     * 
     * <p><b>Workflow:</b></p>
     * <ol>
     *   <li>Récupérer ou créer le customer Stripe</li>
     *   <li>Attacher le moyen de paiement au customer</li>
     *   <li>Créer l'abonnement</li>
     * </ol>
     * 
     * <p><b>Exemple:</b></p>
     * <pre>{@code
     * String subId = stripeService.createSubscription(
     *     "user-123",
     *     SubscriptionPlan.PREMIUM,
     *     "pm_card_visa"
     * );
     * }</pre>
     * 
     * @param userId Identifiant de l'utilisateur
     * @param plan Plan d'abonnement
     * @param paymentMethodId ID du moyen de paiement Stripe
     * @return ID de l'abonnement Stripe créé
     * @throws StripeException Si erreur Stripe
     */
    public String createSubscription(
            String userId, 
            SubscriptionPlan plan, 
            String paymentMethodId) {
        
        try {
            log.info("Création abonnement Stripe pour user {}", userId);
            
            // 1. Récupérer ou créer customer
            Customer customer = getOrCreateCustomer(userId);
            
            // 2. Attacher payment method
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            paymentMethod.attach(
                PaymentMethodAttachParams.builder()
                    .setCustomer(customer.getId())
                    .build()
            );
            
            // 3. Définir comme default payment method
            customer.update(
                CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                        CustomerUpdateParams.InvoiceSettings.builder()
                            .setDefaultPaymentMethod(paymentMethodId)
                            .build()
                    )
                    .build()
            );
            
            // 4. Créer l'abonnement
            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customer.getId())
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(getPriceIdForPlan(plan))
                        .build()
                )
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .addExpand("latest_invoice.payment_intent")
                .build();
            
            Subscription subscription = Subscription.create(params);
            
            log.info("Abonnement Stripe créé: {}", subscription.getId());
            
            return subscription.getId();
            
        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la création d'abonnement", e);
            throw new PaymentException(
                "Erreur lors de la création de l'abonnement: " + e.getMessage(),
                "STRIPE_ERROR",
                e
            );
        }
    }
    
    /**
     * Récupère ou crée un customer Stripe pour l'utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Customer Stripe
     * @throws StripeException Si erreur Stripe
     */
    private Customer getOrCreateCustomer(String userId) throws StripeException {
        // Chercher customer existant
        CustomerSearchParams searchParams = CustomerSearchParams.builder()
            .setQuery("metadata['userId']:'" + userId + "'")
            .build();
        
        CustomerSearchResult result = Customer.search(searchParams);
        
        if (!result.getData().isEmpty()) {
            return result.getData().get(0);
        }
        
        // Créer nouveau customer
        CustomerCreateParams createParams = CustomerCreateParams.builder()
            .putMetadata("userId", userId)
            .build();
        
        return Customer.create(createParams);
    }
    
    /**
     * Retourne l'ID du Price Stripe selon le plan.
     * 
     * <p><b>Note:</b> Ces Price IDs doivent être créés dans le dashboard
     * Stripe et configurés dans l'application.</p>
     * 
     * @param plan Plan d'abonnement
     * @return Price ID Stripe
     */
    private String getPriceIdForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> "price_free";
            case STANDARD -> "price_standard_999";
            case PREMIUM -> "price_premium_1999";
            case VIP_PLUS -> "price_vip_4999";
        };
    }
    
    /**
     * Annule un abonnement Stripe.
     * 
     * @param stripeSubscriptionId ID de l'abonnement Stripe
     * @param immediately Annuler immédiatement ou à la fin de période
     * @throws StripeException Si erreur Stripe
     */
    public void cancelSubscription(String stripeSubscriptionId, boolean immediately) {
        try {
            Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
            
            if (immediately) {
                subscription.cancel();
                log.info("Abonnement {} annulé immédiatement", stripeSubscriptionId);
            } else {
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();
                subscription.update(params);
                log.info("Abonnement {} sera annulé à la fin de période", stripeSubscriptionId);
            }
            
        } catch (StripeException e) {
            log.error("Erreur lors de l'annulation de l'abonnement", e);
            throw new PaymentException(
                "Erreur lors de l'annulation: " + e.getMessage(),
                "STRIPE_ERROR",
                e
            );
        }
    }
    
    /**
     * Crée un PaymentIntent pour un paiement unique (jetons).
     * 
     * @param userId Identifiant de l'utilisateur
     * @param amount Montant en euros
     * @param paymentMethodId ID du moyen de paiement
     * @return ID du PaymentIntent créé
     * @throws StripeException Si erreur Stripe
     */
    public String createPayment(
            String userId, 
            BigDecimal amount, 
            String paymentMethodId) {
        
        try {
            log.info("Création PaymentIntent pour {} EUR", amount);
            
            // Stripe utilise des centimes
            Long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
            
            Customer customer = getOrCreateCustomer(userId);
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("eur")
                .setCustomer(customer.getId())
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();
            
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            
            log.info("PaymentIntent créé: {}", paymentIntent.getId());
            
            return paymentIntent.getId();
            
        } catch (StripeException e) {
            log.error("Erreur lors de la création du paiement", e);
            throw new PaymentException(
                "Erreur lors du paiement: " + e.getMessage(),
                "STRIPE_ERROR",
                e
            );
        }
    }
    
    /**
     * Valide et traite un webhook Stripe.
     * 
     * <p><b>Événements gérés:</b></p>
     * <ul>
     *   <li>customer.subscription.created</li>
     *   <li>customer.subscription.updated</li>
     *   <li>customer.subscription.deleted</li>
     *   <li>invoice.payment_succeeded</li>
     *   <li>invoice.payment_failed</li>
     * </ul>
     * 
     * @param payload Payload du webhook
     * @param signature Signature Stripe
     * @return Event Stripe parsé
     * @throws SignatureVerificationException Si signature invalide
     */
    public Event handleWebhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(
                payload,
                signature,
                webhookSecret
            );
            
            log.info("Webhook reçu: type={}", event.getType());
            
            return event;
            
        } catch (SignatureVerificationException e) {
            log.error("Signature webhook invalide", e);
            throw new SecurityException("Signature webhook invalide", e);
        }
    }
}

// ============================================================================
// REPOSITORIES JPA
// ============================================================================

package com.nexusai.payment.infrastructure.repository;

import com.nexusai.payment.domain.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Repository pour les abonnements.
 * 
 * <p>Fournit les opérations CRUD et des requêtes spécialisées.</p>
 * 
 * @author Developer 3
 * @since 1.0
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    
    /**
     * Récupère l'abonnement actif d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Abonnement actif ou Optional.empty()
     */
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId " +
           "AND s.status = 'ACTIVE' " +
           "AND (s.endDate IS NULL OR s.endDate > CURRENT_TIMESTAMP)")
    Optional<Subscription> findActiveByUserId(@Param("userId") UUID userId);
    
    /**
     * Récupère tous les abonnements d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Liste des abonnements
     */
    List<Subscription> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Récupère un abonnement par son ID Stripe.
     * 
     * @param stripeSubscriptionId ID Stripe
     * @return Abonnement ou Optional.empty()
     */
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    /**
     * Compte les abonnements actifs pour un plan donné.
     * 
     * @param plan Plan d'abonnement
     * @return Nombre d'abonnements actifs
     */
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan = :plan AND s.status = 'ACTIVE'")
    Long countActiveByPlan(@Param("plan") SubscriptionPlan plan);
}

/**
 * Repository pour les wallets de jetons.
 * 
 * @author Developer 3
 * @since 1.0
 */
@Repository
public interface TokenWalletRepository extends JpaRepository<TokenWallet, UUID> {
    
    /**
     * Récupère le wallet d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Wallet ou Optional.empty()
     */
    Optional<TokenWallet> findByUserId(UUID userId);
    
    /**
     * Vérifie si un utilisateur a un wallet.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return true si le wallet existe
     */
    boolean existsByUserId(UUID userId);
}

/**
 * Repository pour les transactions de jetons.
 * 
 * @author Developer 3
 * @since 1.0
 */
@Repository
public interface TokenTransactionRepository extends JpaRepository<TokenTransaction, UUID> {
    
    /**
     * Récupère les transactions d'un wallet, triées par date DESC.
     * 
     * @param walletId Identifiant du wallet
     * @return Liste des transactions
     */
    List<TokenTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
    
    /**
     * Récupère les N dernières transactions d'un wallet.
     * 
     * @param walletId Identifiant du wallet
     * @param pageable Pagination
     * @return Page de transactions
     */
    @Query("SELECT t FROM TokenTransaction t WHERE t.walletId = :walletId ORDER BY t.createdAt DESC")
    org.springframework.data.domain.Page<TokenTransaction> findByWalletIdPaged(
        @Param("walletId") UUID walletId,
        org.springframework.data.domain.Pageable pageable
    );
}

/**
 * Repository pour les transactions de paiement.
 * 
 * @author Developer 3
 * @since 1.0
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    
    /**
     * Récupère les transactions d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Liste des transactions
     */
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Récupère une transaction par son PaymentIntent Stripe.
     * 
     * @param paymentIntentId ID du PaymentIntent
     * @return Transaction ou Optional.empty()
     */
    Optional<PaymentTransaction> findByStripePaymentIntentId(String paymentIntentId);
}

// ============================================================================
// KAFKA EVENT PUBLISHERS
// ============================================================================

package com.nexusai.payment.infrastructure.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Événements du domaine Payment.
 * 
 * <p>Ces événements sont publiés sur Kafka pour notification
 * aux autres modules.</p>
 * 
 * @author Developer 4
 * @since 1.0
 */

/**
 * Événement émis lors de la création d'un abonnement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCreatedEvent {
    private UUID subscriptionId;
    private UUID userId;
    private SubscriptionPlan plan;
    @Builder.Default
    private Instant timestamp = Instant.now();
}

/**
 * Événement émis lors du changement de plan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionUpgradedEvent {
    private UUID subscriptionId;
    private UUID userId;
    private SubscriptionPlan oldPlan;
    private SubscriptionPlan newPlan;
    @Builder.Default
    private Instant timestamp = Instant.now();
}

/**
 * Événement émis lors de l'annulation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCanceledEvent {
    private UUID subscriptionId;
    private UUID userId;
    private SubscriptionPlan plan;
    private Boolean immediately;
    @Builder.Default
    private Instant timestamp = Instant.now();
}

/**
 * Événement émis lors de l'achat de jetons.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokensPurchasedEvent {
    private UUID userId;
    private Integer amount;
    private BigDecimal price;
    @Builder.Default
    private Instant timestamp = Instant.now();
}

/**
 * Événement émis lors de la consommation de jetons.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokensConsumedEvent {
    private UUID userId;
    private Integer amount;
    private TokenTransactionType type;
    private String description;
    @Builder.Default
    private Instant timestamp = Instant.now();
}

/**
 * Publisher d'événements Kafka.
 * 
 * <p><b>Topics:</b></p>
 * <ul>
 *   <li>payment.subscription.created</li>
 *   <li>payment.subscription.upgraded</li>
 *   <li>payment.subscription.canceled</li>
 *   <li>payment.tokens.purchased</li>
 *   <li>payment.tokens.consumed</li>
 * </ul>
 * 
 * @author Developer 4
 * @since 1.0
 */
@Service
@Slf4j
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Publie un événement sur Kafka.
     * 
     * @param event Événement à publier
     */
    public void publish(Object event) {
        String topic = getTopicForEvent(event);
        String key = extractKey(event);
        
        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Erreur lors de la publication de l'événement", ex);
                } else {
                    log.info("Événement publié sur {}: {}", topic, event.getClass().getSimpleName());
                }
            });
    }
    
    private String getTopicForEvent(Object event) {
        if (event instanceof SubscriptionCreatedEvent) {
            return "payment.subscription.created";
        } else if (event instanceof SubscriptionUpgradedEvent) {
            return "payment.subscription.upgraded";
        } else if (event instanceof SubscriptionCanceledEvent) {
            return "payment.subscription.canceled";
        } else if (event instanceof TokensPurchasedEvent) {
            return "payment.tokens.purchased";
        } else if (event instanceof TokensConsumedEvent) {
            return "payment.tokens.consumed";
        }
        throw new IllegalArgumentException("Type d'événement inconnu: " + event.getClass());
    }
    
    private String extractKey(Object event) {
        // Utilise le userId comme clé pour garantir l'ordre des événements par utilisateur
        if (event instanceof SubscriptionCreatedEvent e) return e.getUserId().toString();
        if (event instanceof SubscriptionUpgradedEvent e) return e.getUserId().toString();
        if (event instanceof SubscriptionCanceledEvent e) return e.getUserId().toString();
        if (event instanceof TokensPurchasedEvent e) return e.getUserId().toString();
        if (event instanceof TokensConsumedEvent e) return e.getUserId().toString();
        return "default";
    }
}

// ============================================================================
// CONFIGURATION
// ============================================================================

package com.nexusai.payment.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Configuration du module Infrastructure.
 * 
 * @author Developer 3, Developer 4
 * @since 1.0
 */
@Configuration
public class InfrastructureConfig {
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}