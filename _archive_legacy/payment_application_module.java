/**
 * ============================================================================
 * MODULE: payment-application
 * ============================================================================
 * Ce module contient les services applicatifs qui orchestrent
 * la logique métier et les use cases.
 * 
 * DÉVELOPPEUR ASSIGNÉ: Developer 2 (Équipe Backend Core)
 * DÉPENDANCES: payment-api, payment-domain, payment-infrastructure
 * ============================================================================
 */

package com.nexusai.payment.application.service;

import com.nexusai.payment.api.dto.*;
import com.nexusai.payment.api.request.*;
import com.nexusai.payment.api.response.*;
import com.nexusai.payment.domain.entity.*;
import com.nexusai.payment.domain.usecase.*;
import com.nexusai.payment.infrastructure.repository.*;
import com.nexusai.payment.infrastructure.stripe.StripeService;
import com.nexusai.payment.infrastructure.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

/**
 * Service applicatif pour la gestion des abonnements.
 * 
 * <p><b>Responsabilités:</b></p>
 * <ul>
 *   <li>Orchestrer les use cases d'abonnement</li>
 *   <li>Gérer les transactions</li>
 *   <li>Mapper entre entités et DTOs</li>
 *   <li>Coordonner avec Stripe et la base de données</li>
 * </ul>
 * 
 * <p><b>Design Pattern:</b> Facade + Service Layer</p>
 * 
 * @author Developer 2
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final StripeService stripeService;
    private final EventPublisher eventPublisher;
    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final SubscriptionMapper mapper;
    
    /**
     * Crée un nouvel abonnement.
     * 
     * <p><b>Workflow complet:</b></p>
     * <ol>
     *   <li>Validation de la requête</li>
     *   <li>Vérification qu'aucun abonnement actif n'existe</li>
     *   <li>Création abonnement Stripe</li>
     *   <li>Persistance en base de données</li>
     *   <li>Publication événement Kafka</li>
     *   <li>Construction de la réponse</li>
     * </ol>
     * 
     * <p><b>Exemple d'utilisation:</b></p>
     * <pre>{@code
     * CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
     *     .userId(userId)
     *     .plan(SubscriptionPlan.PREMIUM)
     *     .paymentMethodId("pm_123")
     *     .build();
     * 
     * SubscriptionResponse response = subscriptionService.createSubscription(request);
     * }</pre>
     * 
     * @param request Données de l'abonnement
     * @return Réponse avec l'abonnement créé
     * @throws PaymentException Si le paiement échoue
     * @throws IllegalStateException Si l'utilisateur a déjà un abonnement actif
     */
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Début création abonnement: userId={}, plan={}", 
            request.getUserId(), request.getPlan());
        
        try {
            // Déléguer au use case
            SubscriptionDTO subscription = createSubscriptionUseCase.execute(request);
            
            // Construire réponse
            SubscriptionResponse response = SubscriptionResponse.builder()
                .success(true)
                .message("Abonnement créé avec succès")
                .subscription(subscription)
                .build();
            
            log.info("Abonnement créé avec succès: subscriptionId={}", 
                subscription.getId());
            
            return response;
            
        } catch (PaymentException e) {
            log.error("Erreur lors de la création de l'abonnement", e);
            
            return SubscriptionResponse.builder()
                .success(false)
                .message("Erreur lors du paiement: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Récupère l'abonnement actif d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Abonnement actif ou Optional.empty()
     */
    @Transactional(readOnly = true)
    public Optional<SubscriptionDTO> getActiveSubscription(UUID userId) {
        log.debug("Récupération abonnement actif pour userId={}", userId);
        
        return subscriptionRepository.findActiveByUserId(userId)
            .map(mapper::toDTO);
    }
    
    /**
     * Annule un abonnement.
     * 
     * <p><b>Comportements:</b></p>
     * <ul>
     *   <li>Si immediately=true: Annulation immédiate, remboursement au prorata</li>
     *   <li>Si immediately=false: Annulation à la fin de la période, pas de remboursement</li>
     * </ul>
     * 
     * @param request Détails de l'annulation
     * @return Abonnement mis à jour
     * @throws IllegalArgumentException Si l'abonnement n'existe pas
     */
    @Transactional
    public SubscriptionDTO cancelSubscription(CancelSubscriptionRequest request) {
        log.info("Annulation abonnement: subscriptionId={}, immediately={}", 
            request.getSubscriptionId(), request.getImmediately());
        
        // Récupérer abonnement
        Subscription subscription = subscriptionRepository
            .findById(request.getSubscriptionId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Abonnement non trouvé: " + request.getSubscriptionId()
            ));
        
        // Annuler dans Stripe
        stripeService.cancelSubscription(
            subscription.getStripeSubscriptionId(),
            request.getImmediately()
        );
        
        // Mettre à jour entité
        subscription.cancel(request.getImmediately());
        subscription = subscriptionRepository.save(subscription);
        
        // Émettre événement
        eventPublisher.publish(new SubscriptionCanceledEvent(
            subscription.getId(),
            subscription.getUserId(),
            subscription.getPlan(),
            request.getImmediately()
        ));
        
        log.info("Abonnement annulé: subscriptionId={}", subscription.getId());
        
        return mapper.toDTO(subscription);
    }
    
    /**
     * Change le plan d'un abonnement (upgrade/downgrade).
     * 
     * <p><b>Règles de facturation:</b></p>
     * <ul>
     *   <li>Upgrade: Facturation immédiate du prorata de la différence</li>
     *   <li>Downgrade: Crédit pour la prochaine période</li>
     * </ul>
     * 
     * @param subscriptionId ID de l'abonnement
     * @param newPlan Nouveau plan
     * @return Abonnement mis à jour
     */
    @Transactional
    public SubscriptionDTO updatePlan(UUID subscriptionId, SubscriptionPlan newPlan) {
        log.info("Changement de plan: subscriptionId={}, newPlan={}", 
            subscriptionId, newPlan);
        
        // Récupérer abonnement
        Subscription subscription = subscriptionRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Abonnement non trouvé: " + subscriptionId
            ));
        
        SubscriptionPlan oldPlan = subscription.getPlan();
        
        // Mettre à jour dans Stripe (gère automatiquement le prorata)
        stripeService.updateSubscriptionPlan(
            subscription.getStripeSubscriptionId(),
            newPlan
        );
        
        // Mettre à jour entité
        if (newPlan.getMonthlyPrice().compareTo(oldPlan.getMonthlyPrice()) > 0) {
            subscription.upgradeTo(newPlan);
        } else {
            subscription.downgradeTo(newPlan);
        }
        
        subscription = subscriptionRepository.save(subscription);
        
        // Émettre événement
        eventPublisher.publish(new SubscriptionUpgradedEvent(
            subscription.getId(),
            subscription.getUserId(),
            oldPlan,
            newPlan
        ));
        
        log.info("Plan mis à jour: {} → {}", oldPlan, newPlan);
        
        return mapper.toDTO(subscription);
    }
    
    /**
     * Met à jour le paramètre de renouvellement automatique.
     * 
     * @param subscriptionId ID de l'abonnement
     * @param autoRenewal true pour activer, false pour désactiver
     * @return Abonnement mis à jour
     */
    @Transactional
    public SubscriptionDTO updateAutoRenewal(UUID subscriptionId, boolean autoRenewal) {
        log.info("Mise à jour auto-renewal: subscriptionId={}, autoRenewal={}", 
            subscriptionId, autoRenewal);
        
        Subscription subscription = subscriptionRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Abonnement non trouvé: " + subscriptionId
            ));
        
        subscription.setAutoRenewal(autoRenewal);
        subscription = subscriptionRepository.save(subscription);
        
        log.info("Auto-renewal mis à jour");
        
        return mapper.toDTO(subscription);
    }
}

/**
 * Service applicatif pour la gestion des jetons.
 * 
 * <p><b>Responsabilités:</b></p>
 * <ul>
 *   <li>Gérer l'achat de jetons</li>
 *   <li>Gérer la consommation de jetons</li>
 *   <li>Fournir le solde et l'historique</li>
 * </ul>
 * 
 * @author Developer 2
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
    
    private final TokenWalletRepository walletRepository;
    private final TokenTransactionRepository transactionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final StripeService stripeService;
    private final EventPublisher eventPublisher;
    private final PurchaseTokensUseCase purchaseTokensUseCase;
    private final TokenMapper mapper;
    
    /**
     * Achète des jetons.
     * 
     * <p><b>Workflow:</b></p>
     * <ol>
     *   <li>Calculer le prix selon le montant</li>
     *   <li>Créer PaymentIntent Stripe</li>
     *   <li>Créditer le wallet</li>
     *   <li>Enregistrer la transaction</li>
     *   <li>Émettre événement</li>
     * </ol>
     * 
     * @param request Détails de l'achat
     * @return Transaction créée
     * @throws PaymentException Si le paiement échoue
     */
    @Transactional
    public TokenTransactionDTO purchaseTokens(PurchaseTokensRequest request) {
        log.info("Achat de jetons: userId={}, amount={}", 
            request.getUserId(), request.getTokenAmount());
        
        // Déléguer au use case
        return purchaseTokensUseCase.execute(request);
    }
    
    /**
     * Consomme des jetons pour une action.
     * 
     * <p><b>Types de consommation:</b></p>
     * <ul>
     *   <li>SPENT_IMAGE: Génération d'image (10-50 jetons)</li>
     *   <li>SPENT_VIDEO: Génération de vidéo (100-500 jetons)</li>
     *   <li>SPENT_MESSAGE: Message avancé (1-5 jetons)</li>
     * </ul>
     * 
     * @param request Détails de la consommation
     * @return Transaction créée
     * @throws InsufficientTokensException Si solde insuffisant
     */
    @Transactional
    public TokenTransactionDTO consumeTokens(ConsumeTokensRequest request) {
        log.info("Consommation de jetons: userId={}, amount={}, type={}", 
            request.getUserId(), request.getAmount(), request.getType());
        
        // Récupérer wallet
        TokenWallet wallet = walletRepository.findByUserId(request.getUserId())
            .orElseThrow(() -> new IllegalStateException(
                "Wallet non trouvé pour l'utilisateur: " + request.getUserId()
            ));
        
        // Vérifier solde
        if (!wallet.hasSufficientBalance(request.getAmount())) {
            throw new InsufficientTokensException(
                request.getAmount(),
                wallet.getBalance()
            );
        }
        
        // Consommer jetons
        wallet.consumeTokens(request.getAmount());
        walletRepository.save(wallet);
        
        // Créer transaction
        TokenTransaction transaction = TokenTransaction.builder()
            .walletId(wallet.getId())
            .type(request.getType())
            .amount(-request.getAmount()) // Négatif pour dépense
            .description(request.getDescription())
            .metadata(request.getMetadata())
            .build();
        
        transaction = transactionRepository.save(transaction);
        
        // Émettre événement
        eventPublisher.publish(new TokensConsumedEvent(
            wallet.getUserId(),
            request.getAmount(),
            request.getType(),
            request.getDescription()
        ));
        
        log.info("Jetons consommés: walletId={}, newBalance={}", 
            wallet.getId(), wallet.getBalance());
        
        return mapper.toDTO(transaction);
    }
    
    /**
     * Récupère le solde de jetons d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Solde actuel avec statistiques
     */
    @Transactional(readOnly = true)
    public TokenBalanceResponse getBalance(UUID userId) {
        log.debug("Récupération solde pour userId={}", userId);
        
        TokenWallet wallet = walletRepository.findByUserId(userId)
            .orElseGet(() -> {
                // Créer wallet si inexistant
                TokenWallet newWallet = TokenWallet.builder()
                    .userId(userId)
                    .build();
                return walletRepository.save(newWallet);
            });
        
        // Calculer statistiques du jour (optionnel)
        Integer earnedToday = 0; // À implémenter avec une requête
        Integer spentToday = 0;  // À implémenter avec une requête
        
        return TokenBalanceResponse.builder()
            .userId(userId)
            .balance(wallet.getBalance())
            .earnedToday(earnedToday)
            .spentToday(spentToday)
            .build();
    }
    
    /**
     * Récupère l'historique paginé des transactions.
     * 
     * @param userId Identifiant de l'utilisateur
     * @param page Numéro de page (0-indexed)
     * @param pageSize Taille de la page
     * @return Historique paginé
     */
    @Transactional(readOnly = true)
    public TokenHistoryResponse getHistory(UUID userId, int page, int pageSize) {
        log.debug("Récupération historique: userId={}, page={}", userId, page);
        
        // Récupérer wallet
        TokenWallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException(
                "Wallet non trouvé pour l'utilisateur: " + userId
            ));
        
        // Récupérer transactions paginées
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<TokenTransaction> transactionsPage = 
            transactionRepository.findByWalletIdPaged(wallet.getId(), pageable);
        
        // Mapper vers DTOs
        List<TokenTransactionDTO> transactions = transactionsPage.getContent()
            .stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
        
        return TokenHistoryResponse.builder()
            .transactions(transactions)
            .page(page)
            .pageSize(pageSize)
            .totalElements(transactionsPage.getTotalElements())
            .build();
    }
    
    /**
     * Crédite des jetons gratuitement (bonus, parrainage, etc.).
     * 
     * <p><b>Utilisation:</b> Appelé par d'autres services pour
     * offrir des jetons bonus.</p>
     * 
     * @param userId Identifiant de l'utilisateur
     * @param amount Nombre de jetons à créditer
     * @param reason Raison du crédit
     */
    @Transactional
    public void creditTokens(UUID userId, Integer amount, String reason) {
        log.info("Crédit de jetons gratuits: userId={}, amount={}, reason={}", 
            userId, amount, reason);
        
        TokenWallet wallet = walletRepository.findByUserId(userId)
            .orElseGet(() -> {
                TokenWallet newWallet = TokenWallet.builder()
                    .userId(userId)
                    .build();
                return walletRepository.save(newWallet);
            });
        
        wallet.addTokens(amount);
        walletRepository.save(wallet);
        
        TokenTransaction transaction = TokenTransaction.builder()
            .walletId(wallet.getId())
            .type(TokenTransactionType.EARNED)
            .amount(amount)
            .description(reason)
            .build();
        
        transactionRepository.save(transaction);
        
        log.info("Jetons crédités: walletId={}, newBalance={}", 
            wallet.getId(), wallet.getBalance());
    }
}

/**
 * Service pour traiter les webhooks Stripe.
 * 
 * <p>Gère tous les événements Stripe en temps réel.</p>
 * 
 * @author Developer 2
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final EventPublisher eventPublisher;
    
    /**
     * Traite un événement Stripe.
     * 
     * <p><b>Événements gérés:</b></p>
     * <ul>
     *   <li>customer.subscription.updated</li>
     *   <li>customer.subscription.deleted</li>
     *   <li>invoice.payment_succeeded</li>
     *   <li>invoice.payment_failed</li>
     *   <li>payment_intent.succeeded</li>
     *   <li>payment_intent.payment_failed</li>
     * </ul>
     * 
     * @param event Événement Stripe parsé
     */
    @Transactional
    public void processEvent(Event event) {
        log.info("Traitement événement Stripe: type={}", event.getType());
        
        try {
            switch (event.getType()) {
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                    
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                    
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                    
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                    
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                    
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                    
                default:
                    log.debug("Événement non géré: {}", event.getType());
            }
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement", e);
            // Ne pas relancer l'exception pour ne pas bloquer Stripe
        }
    }
    
    /**
     * Gère la mise à jour d'un abonnement Stripe.
     */
    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription stripeSubscription = 
            (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow();
        
        String subscriptionId = stripeSubscription.getId();
        
        // Récupérer notre abonnement
        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
            .ifPresent(subscription -> {
                // Mettre à jour le statut selon Stripe
                String stripeStatus = stripeSubscription.getStatus();
                
                if ("active".equals(stripeStatus)) {
                    subscription.reactivate();
                } else if ("canceled".equals(stripeStatus)) {
                    subscription.cancel(true);
                }
                
                subscriptionRepository.save(subscription);
                
                log.info("Abonnement mis à jour depuis Stripe: {}", subscriptionId);
            });
    }
    
    /**
     * Gère la suppression d'un abonnement Stripe.
     */
    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSubscription = 
            (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow();
        
        String subscriptionId = stripeSubscription.getId();
        
        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
            .ifPresent(subscription -> {
                subscription.cancel(true);
                subscriptionRepository.save(subscription);
                
                log.info("Abonnement supprimé depuis Stripe: {}", subscriptionId);
            });
    }
    
    /**
     * Gère le succès d'un paiement de facture.
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow();
        
        log.info("Paiement de facture réussi: invoiceId={}, amount={}", 
            invoice.getId(), invoice.getAmountPaid());
        
        // Enregistrer la transaction
        // TODO: Implémenter selon les besoins
    }
    
    /**
     * Gère l'échec d'un paiement de facture.
     */
    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow();
        
        log.warn("Échec paiement de facture: invoiceId={}", invoice.getId());
        
        // Suspendre l'abonnement
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId != null) {
            subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .ifPresent(subscription -> {
                    subscription.suspend();
                    subscriptionRepository.save(subscription);
                    
                    log.info("Abonnement suspendu suite à échec de paiement");
                });
        }
    }
    
    /**
     * Gère le succès d'un PaymentIntent.
     */
    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow();
        
        log.info("PaymentIntent réussi: paymentIntentId={}, amount={}", 
            paymentIntent.getId(), paymentIntent.getAmount());
        
        // Mettre à jour la transaction en base
        paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
            .ifPresent(transaction -> {
                transaction.setStatus("SUCCEEDED");
                paymentRepository.save(transaction);
            });
    }
    
    /**
     * Gère l'échec d'un PaymentIntent.
     */
    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
            .getObject()
            .orElseThrow();
        
        log.warn("PaymentIntent échoué: paymentIntentId={}", paymentIntent.getId());
        
        paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
            .ifPresent(transaction -> {
                transaction.setStatus("FAILED");
                paymentRepository.save(transaction);
            });
    }
}

// ============================================================================
// MAPPERS
// ============================================================================

package com.nexusai.payment.application.mapper;

import com.nexusai.payment.api.dto.*;
import com.nexusai.payment.domain.entity.*;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

/**
 * Mapper pour les entités Subscription.
 * 
 * <p>Utilise MapStruct pour générer automatiquement
 * le code de mapping.</p>
 * 
 * @author Developer 2
 * @since 1.0
 */
@Mapper(componentModel = "spring")
@Component
public interface SubscriptionMapper {
    
    /**
     * Convertit une entité en DTO.
     * 
     * @param entity Entité source
     * @return DTO
     */
    SubscriptionDTO toDTO(Subscription entity);
    
    /**
     * Convertit un DTO en entité.
     * 
     * @param dto DTO source
     * @return Entité
     */
    Subscription toEntity(SubscriptionDTO dto);
}

/**
 * Mapper pour les entités Token.
 * 
 * @author Developer 2
 * @since 1.0
 */
@Mapper(componentModel = "spring")
@Component
public interface TokenMapper {
    
    TokenWalletDTO toDTO(TokenWallet entity);
    TokenWallet toEntity(TokenWalletDTO dto);
    
    TokenTransactionDTO toDTO(TokenTransaction entity);
    TokenTransaction toEntity(TokenTransactionDTO dto);
}

/**
 * Mapper pour les entités PaymentTransaction.
 * 
 * @author Developer 2
 * @since 1.0
 */
@Mapper(componentModel = "spring")
@Component
public interface PaymentTransactionMapper {
    
    PaymentTransactionDTO toDTO(PaymentTransaction entity);
    PaymentTransaction toEntity(PaymentTransactionDTO dto);
}