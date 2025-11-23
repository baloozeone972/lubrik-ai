/**
 * ============================================================================
 * MODULE: payment-domain
 * ============================================================================
 * Ce module contient la logique métier : entités, value objects, use cases.
 * 
 * DÉVELOPPEUR ASSIGNÉ: Developer 1 (Équipe Backend Core)
 * DÉPENDANCES: payment-api
 * ============================================================================
 */

package com.nexusai.payment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

// ============================================================================
// ENTITÉS JPA
// ============================================================================

/**
 * Entité représentant un abonnement utilisateur.
 * 
 * <p><b>Responsabilités:</b></p>
 * <ul>
 *   <li>Stockage des données d'abonnement</li>
 *   <li>Validation des règles métier</li>
 *   <li>Gestion du cycle de vie de l'abonnement</li>
 * </ul>
 * 
 * <p><b>Table:</b> subscriptions</p>
 * 
 * @author Developer 1
 * @since 1.0
 */
@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_sub_user", columnList = "user_id"),
    @Index(name = "idx_sub_status", columnList = "status"),
    @Index(name = "idx_sub_stripe", columnList = "stripe_subscription_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    /**
     * Identifiant de l'utilisateur propriétaire.
     * 
     * <p>Référence vers le module User Management.</p>
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    /**
     * Plan d'abonnement actuel.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private SubscriptionPlan plan;
    
    /**
     * Statut de l'abonnement.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    
    /**
     * Date de début de l'abonnement.
     */
    @Column(name = "start_date", nullable = false)
    private Instant startDate;
    
    /**
     * Date de fin de l'abonnement (null si actif).
     */
    @Column(name = "end_date")
    private Instant endDate;
    
    /**
     * Renouvellement automatique activé.
     */
    @Column(name = "auto_renewal", nullable = false)
    @Builder.Default
    private Boolean autoRenewal = true;
    
    /**
     * Prix mensuel de l'abonnement.
     */
    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;
    
    /**
     * ID de l'abonnement dans Stripe.
     * 
     * <p>Format: sub_xxxxxxxxxxxxx</p>
     */
    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;
    
    /**
     * Date de création.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    /**
     * Date de dernière modification.
     */
    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    // ========================================================================
    // MÉTHODES MÉTIER
    // ========================================================================
    
    /**
     * Annule l'abonnement.
     * 
     * <p>Si {@code immediately} est true, l'abonnement est annulé
     * immédiatement. Sinon, il reste actif jusqu'à la fin de la période.</p>
     * 
     * @param immediately Annuler immédiatement ou non
     */
    public void cancel(boolean immediately) {
        if (immediately) {
            this.status = SubscriptionStatus.CANCELED;
            this.endDate = Instant.now();
        } else {
            this.status = SubscriptionStatus.CANCELED;
            // endDate reste à la fin de période
        }
        this.autoRenewal = false;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Suspend l'abonnement (problème de paiement).
     */
    public void suspend() {
        this.status = SubscriptionStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Réactive un abonnement suspendu.
     */
    public void reactivate() {
        if (this.status == SubscriptionStatus.SUSPENDED) {
            this.status = SubscriptionStatus.ACTIVE;
            this.updatedAt = Instant.now();
        }
    }
    
    /**
     * Vérifie si l'abonnement est actif.
     * 
     * @return true si actif, false sinon
     */
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE &&
               (endDate == null || Instant.now().isBefore(endDate));
    }
    
    /**
     * Upgrade vers un plan supérieur.
     * 
     * @param newPlan Nouveau plan
     * @throws IllegalArgumentException Si le plan n'est pas supérieur
     */
    public void upgradeTo(SubscriptionPlan newPlan) {
        if (newPlan.getMonthlyPrice().compareTo(this.plan.getMonthlyPrice()) <= 0) {
            throw new IllegalArgumentException(
                "Le nouveau plan doit être supérieur au plan actuel"
            );
        }
        this.plan = newPlan;
        this.monthlyPrice = newPlan.getMonthlyPrice();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Downgrade vers un plan inférieur.
     * 
     * @param newPlan Nouveau plan
     */
    public void downgradeTo(SubscriptionPlan newPlan) {
        this.plan = newPlan;
        this.monthlyPrice = newPlan.getMonthlyPrice();
        this.updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

/**
 * Entité représentant un portefeuille de jetons.
 * 
 * <p>Chaque utilisateur possède un unique wallet pour gérer ses jetons.</p>
 * 
 * @author Developer 1
 * @since 1.0
 */
@Entity
@Table(name = "token_wallets", indexes = {
    @Index(name = "idx_wallet_user", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenWallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    /**
     * Identifiant de l'utilisateur propriétaire.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    /**
     * Solde actuel de jetons.
     * 
     * <p>Doit toujours être >= 0.</p>
     */
    @Column(name = "balance", nullable = false)
    @Builder.Default
    private Integer balance = 0;
    
    /**
     * Total de jetons gagnés (historique).
     */
    @Column(name = "total_earned", nullable = false)
    @Builder.Default
    private Integer totalEarned = 0;
    
    /**
     * Total de jetons dépensés (historique).
     */
    @Column(name = "total_spent", nullable = false)
    @Builder.Default
    private Integer totalSpent = 0;
    
    /**
     * Date de création.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    // ========================================================================
    // MÉTHODES MÉTIER
    // ========================================================================
    
    /**
     * Ajoute des jetons au wallet.
     * 
     * <p><b>Exemple:</b></p>
     * <pre>{@code
     * wallet.addTokens(100); // Achète 100 jetons
     * }</pre>
     * 
     * @param amount Nombre de jetons à ajouter (doit être > 0)
     * @throws IllegalArgumentException Si amount <= 0
     */
    public void addTokens(Integer amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        this.balance += amount;
        this.totalEarned += amount;
    }
    
    /**
     * Consomme des jetons du wallet.
     * 
     * <p><b>Exemple:</b></p>
     * <pre>{@code
     * wallet.consumeTokens(50); // Consomme 50 jetons
     * }</pre>
     * 
     * @param amount Nombre de jetons à consommer (doit être > 0)
     * @throws IllegalArgumentException Si amount <= 0
     * @throws InsufficientTokensException Si solde insuffisant
     */
    public void consumeTokens(Integer amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        if (this.balance < amount) {
            throw new InsufficientTokensException(amount, this.balance);
        }
        this.balance -= amount;
        this.totalSpent += amount;
    }
    
    /**
     * Vérifie si le wallet a suffisamment de jetons.
     * 
     * @param amount Montant requis
     * @return true si suffisant, false sinon
     */
    public boolean hasSufficientBalance(Integer amount) {
        return this.balance >= amount;
    }
}

/**
 * Entité représentant une transaction de jetons.
 * 
 * <p>Enregistre toutes les opérations effectuées sur un wallet.</p>
 * 
 * @author Developer 1
 * @since 1.0
 */
@Entity
@Table(name = "token_transactions", indexes = {
    @Index(name = "idx_tx_wallet", columnList = "wallet_id"),
    @Index(name = "idx_tx_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    /**
     * Référence au wallet.
     */
    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;
    
    /**
     * Type de transaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TokenTransactionType type;
    
    /**
     * Montant de la transaction.
     * 
     * <p>Positif pour ajout, négatif pour dépense.</p>
     */
    @Column(name = "amount", nullable = false)
    private Integer amount;
    
    /**
     * Description de la transaction.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * Métadonnées additionnelles (format JSON).
     * 
     * <p>Exemple: {"imageId": "uuid", "quality": "HD"}</p>
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * Date de la transaction.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

/**
 * Entité représentant une transaction de paiement.
 * 
 * <p>Enregistre tous les paiements effectués via Stripe.</p>
 * 
 * @author Developer 1
 * @since 1.0
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_user", columnList = "user_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_stripe", columnList = "stripe_payment_intent_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    /**
     * Type de paiement.
     * 
     * <p>Exemples: "SUBSCRIPTION", "TOKENS", "REFUND"</p>
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;
    
    /**
     * Montant du paiement.
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    /**
     * Devise.
     */
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";
    
    /**
     * Statut du paiement.
     * 
     * <p>Valeurs: "PENDING", "SUCCEEDED", "FAILED", "REFUNDED"</p>
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
    
    /**
     * ID du PaymentIntent Stripe.
     */
    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;
    
    /**
     * Date de création.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

// ============================================================================
// USE CASES
// ============================================================================

package com.nexusai.payment.domain.usecase;

import com.nexusai.payment.api.dto.SubscriptionDTO;
import com.nexusai.payment.api.request.CreateSubscriptionRequest;
import com.nexusai.payment.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case: Créer un nouvel abonnement.
 * 
 * <p><b>Responsabilités:</b></p>
 * <ul>
 *   <li>Valider la requête</li>
 *   <li>Appeler Stripe pour créer l'abonnement</li>
 *   <li>Persister l'abonnement en base</li>
 *   <li>Émettre un événement</li>
 * </ul>
 * 
 * <p><b>Règles métier:</b></p>
 * <ul>
 *   <li>Un utilisateur ne peut avoir qu'un seul abonnement actif</li>
 *   <li>Le paiement doit être valide avant création</li>
 * </ul>
 * 
 * @author Developer 1
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateSubscriptionUseCase {
    
    private final SubscriptionRepository subscriptionRepository;
    private final StripeService stripeService;
    private final EventPublisher eventPublisher;
    
    /**
     * Exécute le use case de création d'abonnement.
     * 
     * <p><b>Workflow:</b></p>
     * <ol>
     *   <li>Vérifier qu'aucun abonnement actif n'existe</li>
     *   <li>Créer l'abonnement dans Stripe</li>
     *   <li>Persister en base de données</li>
     *   <li>Émettre événement "subscription.created"</li>
     * </ol>
     * 
     * @param request Données de l'abonnement
     * @return DTO de l'abonnement créé
     * @throws PaymentException Si le paiement échoue
     */
    @Transactional
    public SubscriptionDTO execute(CreateSubscriptionRequest request) {
        log.info("Création abonnement pour user {}, plan {}", 
            request.getUserId(), request.getPlan());
        
        // 1. Vérifier abonnement existant
        subscriptionRepository.findActiveByUserId(request.getUserId())
            .ifPresent(sub -> {
                throw new IllegalStateException(
                    "L'utilisateur a déjà un abonnement actif"
                );
            });
        
        // 2. Créer abonnement Stripe
        String stripeSubId = stripeService.createSubscription(
            request.getUserId().toString(),
            request.getPlan(),
            request.getPaymentMethodId()
        );
        
        // 3. Créer entité
        Subscription subscription = Subscription.builder()
            .userId(request.getUserId())
            .plan(request.getPlan())
            .status(SubscriptionStatus.ACTIVE)
            .startDate(Instant.now())
            .monthlyPrice(request.getPlan().getMonthlyPrice())
            .stripeSubscriptionId(stripeSubId)
            .autoRenewal(true)
            .build();
        
        // 4. Persister
        subscription = subscriptionRepository.save(subscription);
        
        // 5. Émettre événement
        eventPublisher.publish(new SubscriptionCreatedEvent(
            subscription.getId(),
            subscription.getUserId(),
            subscription.getPlan()
        ));
        
        log.info("Abonnement créé avec succès: {}", subscription.getId());
        
        return mapToDTO(subscription);
    }
    
    private SubscriptionDTO mapToDTO(Subscription entity) {
        return SubscriptionDTO.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .plan(entity.getPlan())
            .status(entity.getStatus())
            .startDate(entity.getStartDate())
            .endDate(entity.getEndDate())
            .autoRenewal(entity.getAutoRenewal())
            .monthlyPrice(entity.getMonthlyPrice())
            .stripeSubscriptionId(entity.getStripeSubscriptionId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}

/**
 * Use Case: Acheter des jetons.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseTokensUseCase {
    
    private final TokenWalletRepository walletRepository;
    private final TokenTransactionRepository transactionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final StripeService stripeService;
    private final EventPublisher eventPublisher;
    
    /**
     * Calcule le prix selon le nombre de jetons.
     * 
     * <p><b>Tarifs:</b></p>
     * <ul>
     *   <li>100 jetons = 4.99€</li>
     *   <li>500 jetons = 19.99€</li>
     *   <li>1000 jetons = 34.99€</li>
     * </ul>
     */
    private BigDecimal calculatePrice(Integer tokenAmount) {
        if (tokenAmount == 100) return new BigDecimal("4.99");
        if (tokenAmount == 500) return new BigDecimal("19.99");
        if (tokenAmount == 1000) return new BigDecimal("34.99");
        
        // Prix par défaut: 0.05€ par jeton
        return new BigDecimal(tokenAmount * 0.05);
    }
    
    @Transactional
    public TokenTransactionDTO execute(PurchaseTokensRequest request) {
        log.info("Achat de {} jetons pour user {}", 
            request.getTokenAmount(), request.getUserId());
        
        // 1. Calculer prix
        BigDecimal price = calculatePrice(request.getTokenAmount());
        
        // 2. Effectuer paiement Stripe
        String paymentIntentId = stripeService.createPayment(
            request.getUserId().toString(),
            price,
            request.getPaymentMethodId()
        );
        
        // 3. Enregistrer transaction paiement
        PaymentTransaction payment = PaymentTransaction.builder()
            .userId(request.getUserId())
            .type("TOKENS")
            .amount(price)
            .currency("EUR")
            .status("SUCCEEDED")
            .stripePaymentIntentId(paymentIntentId)
            .build();
        paymentRepository.save(payment);
        
        // 4. Récupérer ou créer wallet
        TokenWallet wallet = walletRepository.findByUserId(request.getUserId())
            .orElseGet(() -> {
                TokenWallet newWallet = TokenWallet.builder()
                    .userId(request.getUserId())
                    .build();
                return walletRepository.save(newWallet);
            });
        
        // 5. Ajouter jetons
        wallet.addTokens(request.getTokenAmount());
        walletRepository.save(wallet);
        
        // 6. Créer transaction jetons
        TokenTransaction transaction = TokenTransaction.builder()
            .walletId(wallet.getId())
            .type(TokenTransactionType.PURCHASE)
            .amount(request.getTokenAmount())
            .description("Achat de " + request.getTokenAmount() + " jetons")
            .build();
        transaction = transactionRepository.save(transaction);
        
        // 7. Émettre événement
        eventPublisher.publish(new TokensPurchasedEvent(
            wallet.getUserId(),
            request.getTokenAmount(),
            price
        ));
        
        log.info("Achat réussi: {} jetons ajoutés", request.getTokenAmount());
        
        return mapToDTO(transaction);
    }
}