package com.nexusai.auth.service;

import com.nexusai.auth.repository.TokenTransactionRepository;
import com.nexusai.auth.repository.TokenWalletRepository;
import com.nexusai.core.domain.TokenTransaction;
import com.nexusai.core.domain.TokenWallet;
import com.nexusai.core.domain.User;
import com.nexusai.core.enums.TransactionType;
import com.nexusai.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des jetons.
 * 
 * Gère toutes les opérations liées aux jetons :
 * - Consommation
 * - Ajout (achat, bonus)
 * - Historique des transactions
 * - Bonus quotidiens
 * - Remboursements
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final TokenWalletRepository walletRepository;
    private final TokenTransactionRepository transactionRepository;
    
    // Configuration des bonus
    private static final int DAILY_BONUS_AMOUNT = 50;
    private static final int WELCOME_BONUS_AMOUNT = 100;
    private static final Duration DAILY_BONUS_COOLDOWN = Duration.ofHours(24);
    
    /**
     * Récupère le portefeuille d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return TokenWallet
     * @throws ResourceNotFoundException Si le portefeuille n'existe pas
     */
    public TokenWallet getUserWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Portefeuille non trouvé pour l'utilisateur: " + userId
                ));
    }
    
    /**
     * Récupère le solde d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Solde actuel
     */
    public int getBalance(UUID userId) {
        TokenWallet wallet = getUserWallet(userId);
        return wallet.getBalance();
    }
    
    /**
     * Consomme des jetons.
     * 
     * @param userId ID de l'utilisateur
     * @param amount Montant à consommer
     * @param referenceType Type de référence (IMAGE_GENERATION, etc.)
     * @param referenceId ID de référence
     * @param description Description de l'opération
     * @return TokenTransaction créée
     * @throws IllegalArgumentException Si solde insuffisant
     */
    @Transactional
    public TokenTransaction consumeTokens(
            UUID userId,
            int amount,
            String referenceType,
            UUID referenceId,
            String description) {
        
        log.info("Consommation de {} jetons pour l'utilisateur {}", amount, userId);
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        
        TokenWallet wallet = getUserWallet(userId);
        
        if (!wallet.consume(amount)) {
            throw new IllegalArgumentException(
                "Solde insuffisant. Solde actuel: " + wallet.getBalance() + 
                ", requis: " + amount
            );
        }
        
        TokenTransaction transaction = TokenTransaction.createSpend(
            wallet, amount, referenceType, referenceId, description
        );
        
        walletRepository.save(wallet);
        transactionRepository.save(transaction);
        
        log.info("Jetons consommés avec succès. Nouveau solde: {}", wallet.getBalance());
        
        return transaction;
    }
    
    /**
     * Ajoute des jetons au portefeuille.
     * 
     * @param userId ID de l'utilisateur
     * @param amount Montant à ajouter
     * @param type Type de transaction
     * @param description Description
     * @return TokenTransaction créée
     */
    @Transactional
    public TokenTransaction addTokens(
            UUID userId,
            int amount,
            TransactionType type,
            String description) {
        
        log.info("Ajout de {} jetons ({}) pour l'utilisateur {}", amount, type, userId);
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        
        TokenWallet wallet = getUserWallet(userId);
        wallet.add(amount);
        
        TokenTransaction transaction = TokenTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .build();
        
        walletRepository.save(wallet);
        transactionRepository.save(transaction);
        
        log.info("Jetons ajoutés avec succès. Nouveau solde: {}", wallet.getBalance());
        
        return transaction;
    }
    
    /**
     * Achète des jetons.
     * 
     * @param userId ID de l'utilisateur
     * @param amount Montant de jetons
     * @param paymentReference Référence du paiement
     * @return TokenTransaction créée
     */
    @Transactional
    public TokenTransaction purchaseTokens(UUID userId, int amount, String paymentReference) {
        return addTokens(
            userId,
            amount,
            TransactionType.PURCHASE,
            "Achat de " + amount + " jetons - " + paymentReference
        );
    }
    
    /**
     * Accorde le bonus quotidien si éligible.
     * 
     * @param userId ID de l'utilisateur
     * @return TokenTransaction créée ou null si non éligible
     */
    @Transactional
    public TokenTransaction getDailyBonus(UUID userId) {
        log.info("Tentative de récupération du bonus quotidien pour {}", userId);
        
        TokenWallet wallet = getUserWallet(userId);
        
        if (!canClaimDailyBonus(wallet)) {
            LocalDateTime nextAvailable = wallet.getLastDailyBonus()
                    .plus(DAILY_BONUS_COOLDOWN);
            log.info("Bonus quotidien non disponible avant: {}", nextAvailable);
            throw new IllegalStateException(
                "Bonus quotidien déjà réclamé. Prochaine disponibilité: " + nextAvailable
            );
        }
        
        wallet.add(DAILY_BONUS_AMOUNT);
        wallet.setLastDailyBonus(LocalDateTime.now());
        
        TokenTransaction transaction = TokenTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.DAILY_BONUS)
                .amount(DAILY_BONUS_AMOUNT)
                .balanceAfter(wallet.getBalance())
                .description("Bonus quotidien")
                .build();
        
        walletRepository.save(wallet);
        transactionRepository.save(transaction);
        
        log.info("Bonus quotidien accordé. Nouveau solde: {}", wallet.getBalance());
        
        return transaction;
    }
    
    /**
     * Vérifie si l'utilisateur peut réclamer le bonus quotidien.
     * 
     * @param wallet Portefeuille
     * @return true si éligible, false sinon
     */
    public boolean canClaimDailyBonus(TokenWallet wallet) {
        if (wallet.getLastDailyBonus() == null) {
            return true;
        }
        
        LocalDateTime nextAvailable = wallet.getLastDailyBonus()
                .plus(DAILY_BONUS_COOLDOWN);
        
        return LocalDateTime.now().isAfter(nextAvailable);
    }
    
    /**
     * Calcule le temps restant avant le prochain bonus.
     * 
     * @param userId ID de l'utilisateur
     * @return Duration ou null si disponible maintenant
     */
    public Duration getTimeUntilNextBonus(UUID userId) {
        TokenWallet wallet = getUserWallet(userId);
        
        if (canClaimDailyBonus(wallet)) {
            return Duration.ZERO;
        }
        
        LocalDateTime nextAvailable = wallet.getLastDailyBonus()
                .plus(DAILY_BONUS_COOLDOWN);
        
        return Duration.between(LocalDateTime.now(), nextAvailable);
    }
    
    /**
     * Récupère l'historique des transactions.
     * 
     * @param userId ID de l'utilisateur
     * @param pageable Pagination
     * @return Page de transactions
     */
    public Page<TokenTransaction> getTransactionHistory(UUID userId, Pageable pageable) {
        TokenWallet wallet = getUserWallet(userId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(
            wallet.getId(), pageable
        );
    }
    
    /**
     * Récupère les transactions par type.
     * 
     * @param userId ID de l'utilisateur
     * @param type Type de transaction
     * @return Liste de transactions
     */
    public List<TokenTransaction> getTransactionsByType(UUID userId, TransactionType type) {
        TokenWallet wallet = getUserWallet(userId);
        return transactionRepository.findByWalletIdAndType(wallet.getId(), type);
    }
    
    /**
     * Calcule le total dépensé dans une période.
     * 
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Total dépensé
     */
    public int getTotalSpentInPeriod(
            UUID userId, 
            LocalDateTime startDate, 
            LocalDateTime endDate) {
        
        TokenWallet wallet = getUserWallet(userId);
        return transactionRepository.calculateTotalSpent(
            wallet.getId(), startDate, endDate
        );
    }
    
    /**
     * Rembourse des jetons.
     * 
     * @param userId ID de l'utilisateur
     * @param amount Montant à rembourser
     * @param reason Raison du remboursement
     * @param originalTransactionId Transaction originale
     * @return TokenTransaction créée
     */
    @Transactional
    public TokenTransaction refundTokens(
            UUID userId,
            int amount,
            String reason,
            UUID originalTransactionId) {
        
        log.info("Remboursement de {} jetons pour {}: {}", amount, userId, reason);
        
        TokenWallet wallet = getUserWallet(userId);
        wallet.add(amount);
        
        TokenTransaction transaction = TokenTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.REFUND)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description("Remboursement: " + reason)
                .referenceType("TRANSACTION")
                .referenceId(originalTransactionId)
                .build();
        
        walletRepository.save(wallet);
        transactionRepository.save(transaction);
        
        log.info("Remboursement effectué. Nouveau solde: {}", wallet.getBalance());
        
        return transaction;
    }
    
    /**
     * Accorde le bonus de bienvenue à un nouvel utilisateur.
     * 
     * @param user Nouvel utilisateur
     * @return TokenTransaction créée
     */
    @Transactional
    public TokenTransaction grantWelcomeBonus(User user) {
        log.info("Attribution du bonus de bienvenue à {}", user.getId());
        
        return addTokens(
            user.getId(),
            WELCOME_BONUS_AMOUNT,
            TransactionType.WELCOME_GIFT,
            "Bonus de bienvenue"
        );
    }
    
    /**
     * Obtient des statistiques sur les jetons.
     * 
     * @param userId ID de l'utilisateur
     * @return Map de statistiques
     */
    public TokenStatistics getTokenStatistics(UUID userId) {
        TokenWallet wallet = getUserWallet(userId);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        
        int monthlySpent = getTotalSpentInPeriod(userId, monthStart, now);
        
        return TokenStatistics.builder()
                .currentBalance(wallet.getBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalSpent(wallet.getTotalSpent())
                .monthlySpent(monthlySpent)
                .canClaimDailyBonus(canClaimDailyBonus(wallet))
                .timeUntilNextBonus(getTimeUntilNextBonus(userId))
                .build();
    }
    
    /**
     * Classe interne pour les statistiques de jetons.
     */
    @lombok.Data
    @lombok.Builder
    public static class TokenStatistics {
        private int currentBalance;
        private int totalEarned;
        private int totalSpent;
        private int monthlySpent;
        private boolean canClaimDailyBonus;
        private Duration timeUntilNextBonus;
    }
}
