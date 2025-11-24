/**
 * ============================================================================
 * MODULE: payment-api
 * ============================================================================
 * Ce module contient tous les contrats API : DTOs, Requests, Responses
 * et interfaces publiques.
 * 
 * DÉVELOPPEUR ASSIGNÉ: Developer 1 (Équipe Backend Core)
 * DÉPENDANCES: Aucune (module indépendant)
 * ============================================================================
 */

package com.nexusai.payment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

// ============================================================================
// ENUMS
// ============================================================================

/**
 * Plans d'abonnement disponibles dans NexusAI.
 * 
 * <p>Chaque plan offre des fonctionnalités différentes et des quotas spécifiques.</p>
 * 
 * @author Developer 1
 * @since 1.0
 */
public enum SubscriptionPlan {
    /**
     * Plan gratuit avec fonctionnalités limitées.
     * - Modération stricte obligatoire
     * - 1 compagnon maximum
     * - 50 messages/jour
     */
    FREE(BigDecimal.ZERO, "Gratuit"),
    
    /**
     * Plan standard avec fonctionnalités de base.
     * Prix: 9.99€/mois
     * - Modération stricte obligatoire
     * - 3 compagnons maximum
     * - Messages illimités
     * - Images basiques
     */
    STANDARD(new BigDecimal("9.99"), "Standard"),
    
    /**
     * Plan premium avec fonctionnalités avancées.
     * Prix: 19.99€/mois
     * - Choix modération (stricte ou légère)
     * - 10 compagnons maximum
     * - Génération images HD
     * - Génération vidéos courtes
     */
    PREMIUM(new BigDecimal("19.99"), "Premium"),
    
    /**
     * Plan VIP+ avec toutes les fonctionnalités.
     * Prix: 49.99€/mois
     * - Modération optionnelle (avec KYC niveau 3)
     * - Compagnons illimités
     * - Génération vidéos 4K
     * - Accès VR
     */
    VIP_PLUS(new BigDecimal("49.99"), "VIP+");
    
    private final BigDecimal monthlyPrice;
    private final String displayName;
    
    SubscriptionPlan(BigDecimal monthlyPrice, String displayName) {
        this.monthlyPrice = monthlyPrice;
        this.displayName = displayName;
    }
    
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public String getDisplayName() { return displayName; }
    public boolean isFreeOrStandard() { return this == FREE || this == STANDARD; }
}

/**
 * Statut d'un abonnement.
 * 
 * @author Developer 1
 * @since 1.0
 */
public enum SubscriptionStatus {
    /** Abonnement actif et en cours */
    ACTIVE,
    
    /** Abonnement annulé mais encore valide jusqu'à la fin de période */
    CANCELED,
    
    /** Abonnement expiré */
    EXPIRED,
    
    /** Abonnement suspendu (problème de paiement) */
    SUSPENDED,
    
    /** En période d'essai */
    TRIAL
}

/**
 * Type de transaction de jetons.
 * 
 * @author Developer 1
 * @since 1.0
 */
public enum TokenTransactionType {
    /** Achat de jetons */
    PURCHASE,
    
    /** Jetons gagnés (bonus, parrainage, etc.) */
    EARNED,
    
    /** Consommation pour génération d'image */
    SPENT_IMAGE,
    
    /** Consommation pour génération de vidéo */
    SPENT_VIDEO,
    
    /** Consommation pour message */
    SPENT_MESSAGE,
    
    /** Remboursement */
    REFUND
}

// ============================================================================
// DTOs - SUBSCRIPTION
// ============================================================================

/**
 * DTO représentant un abonnement utilisateur.
 * 
 * <p>Contient toutes les informations nécessaires pour afficher
 * et gérer un abonnement côté client.</p>
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDTO {
    
    /** Identifiant unique de l'abonnement */
    private UUID id;
    
    /** Identifiant de l'utilisateur */
    private UUID userId;
    
    /** Plan d'abonnement */
    private SubscriptionPlan plan;
    
    /** Statut de l'abonnement */
    private SubscriptionStatus status;
    
    /** Date de début */
    private Instant startDate;
    
    /** Date de fin (null si actif) */
    private Instant endDate;
    
    /** Renouvellement automatique activé */
    private Boolean autoRenewal;
    
    /** Prix mensuel */
    private BigDecimal monthlyPrice;
    
    /** ID Stripe de l'abonnement */
    private String stripeSubscriptionId;
    
    /** Date de création */
    private Instant createdAt;
    
    /** Date de dernière modification */
    private Instant updatedAt;
}

/**
 * DTO représentant un portefeuille de jetons.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenWalletDTO {
    
    /** Identifiant unique du wallet */
    private UUID id;
    
    /** Identifiant de l'utilisateur */
    private UUID userId;
    
    /** Solde actuel de jetons */
    private Integer balance;
    
    /** Total de jetons gagnés */
    private Integer totalEarned;
    
    /** Total de jetons dépensés */
    private Integer totalSpent;
    
    /** Date de dernière transaction */
    private Instant lastTransactionAt;
    
    /** Date de création */
    private Instant createdAt;
}

/**
 * DTO représentant une transaction de jetons.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenTransactionDTO {
    
    /** Identifiant unique de la transaction */
    private UUID id;
    
    /** Identifiant du wallet */
    private UUID walletId;
    
    /** Type de transaction */
    private TokenTransactionType type;
    
    /** Montant (positif pour ajout, négatif pour dépense) */
    private Integer amount;
    
    /** Description de la transaction */
    private String description;
    
    /** Métadonnées additionnelles (JSON) */
    private String metadata;
    
    /** Date de la transaction */
    private Instant createdAt;
}

/**
 * DTO représentant une transaction de paiement.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionDTO {
    
    /** Identifiant unique de la transaction */
    private UUID id;
    
    /** Identifiant de l'utilisateur */
    private UUID userId;
    
    /** Type de paiement */
    private String type;
    
    /** Montant */
    private BigDecimal amount;
    
    /** Devise (EUR, USD, etc.) */
    private String currency;
    
    /** Statut du paiement */
    private String status;
    
    /** ID Stripe du PaymentIntent */
    private String stripePaymentIntentId;
    
    /** Date de création */
    private Instant createdAt;
}

// ============================================================================
// REQUEST OBJECTS
// ============================================================================

/**
 * Requête pour créer ou changer un abonnement.
 * 
 * <p><b>Exemple d'utilisation:</b></p>
 * <pre>{@code
 * CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
 *     .userId(UUID.fromString("..."))
 *     .plan(SubscriptionPlan.PREMIUM)
 *     .paymentMethodId("pm_stripe_123")
 *     .build();
 * }</pre>
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {
    
    /** Identifiant de l'utilisateur */
    private UUID userId;
    
    /** Plan d'abonnement souhaité */
    private SubscriptionPlan plan;
    
    /** ID du moyen de paiement Stripe */
    private String paymentMethodId;
    
    /** Code promo (optionnel) */
    private String promoCode;
}

/**
 * Requête pour annuler un abonnement.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelSubscriptionRequest {
    
    /** Identifiant de l'abonnement */
    private UUID subscriptionId;
    
    /** Annuler immédiatement ou à la fin de la période */
    private Boolean immediately;
    
    /** Raison de l'annulation (optionnel) */
    private String reason;
}

/**
 * Requête pour acheter des jetons.
 * 
 * <p><b>Packs disponibles:</b></p>
 * <ul>
 *   <li>100 jetons → 4.99€</li>
 *   <li>500 jetons → 19.99€</li>
 *   <li>1000 jetons → 34.99€</li>
 * </ul>
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseTokensRequest {
    
    /** Identifiant de l'utilisateur */
    private UUID userId;
    
    /** Nombre de jetons à acheter */
    private Integer tokenAmount;
    
    /** ID du moyen de paiement Stripe */
    private String paymentMethodId;
}

/**
 * Requête pour consommer des jetons.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeTokensRequest {
    
    /** Identifiant de l'utilisateur */
    private UUID userId;
    
    /** Nombre de jetons à consommer */
    private Integer amount;
    
    /** Type de consommation */
    private TokenTransactionType type;
    
    /** Description */
    private String description;
    
    /** Métadonnées (JSON) */
    private String metadata;
}

// ============================================================================
// RESPONSE OBJECTS
// ============================================================================

/**
 * Réponse après création/mise à jour d'un abonnement.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    
    /** Succès de l'opération */
    private Boolean success;
    
    /** Message */
    private String message;
    
    /** Données de l'abonnement */
    private SubscriptionDTO subscription;
    
    /** URL Stripe Checkout (si nécessaire) */
    private String checkoutUrl;
}

/**
 * Réponse avec le solde de jetons.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBalanceResponse {
    
    /** Identifiant de l'utilisateur */
    private UUID userId;
    
    /** Solde actuel */
    private Integer balance;
    
    /** Jetons gagnés aujourd'hui */
    private Integer earnedToday;
    
    /** Jetons dépensés aujourd'hui */
    private Integer spentToday;
}

/**
 * Réponse avec l'historique des transactions de jetons.
 * 
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenHistoryResponse {
    
    /** Liste des transactions */
    private java.util.List<TokenTransactionDTO> transactions;
    
    /** Pagination: page actuelle */
    private Integer page;
    
    /** Pagination: taille de page */
    private Integer pageSize;
    
    /** Pagination: total d'éléments */
    private Long totalElements;
}

// ============================================================================
// INTERFACES (PORTS) - Pour l'architecture hexagonale
// ============================================================================

package com.nexusai.payment.api.port;

import com.nexusai.payment.api.dto.*;
import com.nexusai.payment.api.request.*;
import com.nexusai.payment.api.response.*;
import java.util.UUID;
import java.util.Optional;

/**
 * Port de sortie pour la gestion des abonnements.
 * 
 * <p>Cette interface définit le contrat que doivent implémenter
 * les adaptateurs d'infrastructure (Stripe, etc.).</p>
 * 
 * @author Developer 1
 * @since 1.0
 */
public interface SubscriptionPort {
    
    /**
     * Crée un nouvel abonnement.
     * 
     * @param request Données de l'abonnement
     * @return Abonnement créé
     * @throws PaymentException Si le paiement échoue
     */
    SubscriptionDTO createSubscription(CreateSubscriptionRequest request);
    
    /**
     * Annule un abonnement existant.
     * 
     * @param request Détails de l'annulation
     * @return Abonnement mis à jour
     */
    SubscriptionDTO cancelSubscription(CancelSubscriptionRequest request);
    
    /**
     * Récupère l'abonnement actif d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Abonnement ou Optional.empty() si aucun
     */
    Optional<SubscriptionDTO> getActiveSubscription(UUID userId);
    
    /**
     * Met à jour le renouvellement automatique.
     * 
     * @param subscriptionId ID de l'abonnement
     * @param autoRenewal Activer/désactiver
     * @return Abonnement mis à jour
     */
    SubscriptionDTO updateAutoRenewal(UUID subscriptionId, boolean autoRenewal);
}

/**
 * Port de sortie pour la gestion des jetons.
 * 
 * @author Developer 1
 * @since 1.0
 */
public interface TokenPort {
    
    /**
     * Achète des jetons.
     * 
     * @param request Détails de l'achat
     * @return Transaction créée
     * @throws PaymentException Si le paiement échoue
     */
    TokenTransactionDTO purchaseTokens(PurchaseTokensRequest request);
    
    /**
     * Consomme des jetons.
     * 
     * @param request Détails de la consommation
     * @return Transaction créée
     * @throws InsufficientTokensException Si solde insuffisant
     */
    TokenTransactionDTO consumeTokens(ConsumeTokensRequest request);
    
    /**
     * Récupère le solde de jetons d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Solde actuel
     */
    TokenBalanceResponse getBalance(UUID userId);
    
    /**
     * Récupère l'historique des transactions.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param page Numéro de page (0-indexed)
     * @param pageSize Taille de la page
     * @return Historique paginé
     */
    TokenHistoryResponse getHistory(UUID userId, int page, int pageSize);
}

// ============================================================================
// EXCEPTIONS
// ============================================================================

package com.nexusai.payment.api.exception;

/**
 * Exception levée lors d'erreurs de paiement.
 * 
 * @author Developer 1
 * @since 1.0
 */
public class PaymentException extends RuntimeException {
    
    private final String errorCode;
    
    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public PaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

/**
 * Exception levée quand le solde de jetons est insuffisant.
 * 
 * @author Developer 1
 * @since 1.0
 */
public class InsufficientTokensException extends RuntimeException {
    
    private final Integer required;
    private final Integer available;
    
    public InsufficientTokensException(Integer required, Integer available) {
        super(String.format(
            "Jetons insuffisants. Requis: %d, Disponible: %d",
            required, available
        ));
        this.required = required;
        this.available = available;
    }
    
    public Integer getRequired() { return required; }
    public Integer getAvailable() { return available; }
}