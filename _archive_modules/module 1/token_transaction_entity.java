package com.nexusai.core.domain;

import com.nexusai.core.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une transaction de jetons.
 * 
 * Permet de tracer toutes les opérations sur les jetons :
 * - Achats
 * - Gains (bonus quotidien, récompenses)
 * - Dépenses (génération d'images, etc.)
 * - Remboursements
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "token_transactions", indexes = {
    @Index(name = "idx_token_tx_wallet", columnList = "wallet_id"),
    @Index(name = "idx_token_tx_created", columnList = "created_at"),
    @Index(name = "idx_token_tx_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Portefeuille concerné par la transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private TokenWallet wallet;
    
    /**
     * Type de transaction (PURCHASE, EARN, SPEND, REFUND, EXPIRE).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;
    
    /**
     * Montant de la transaction (positif ou négatif).
     */
    @Column(nullable = false)
    private Integer amount;
    
    /**
     * Solde après la transaction.
     */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;
    
    /**
     * Description de la transaction.
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * Référence vers l'entité liée (ex: ID de génération d'image).
     */
    @Column(name = "reference_id")
    private UUID referenceId;
    
    /**
     * Type de référence (IMAGE_GENERATION, VIDEO_GENERATION, etc.).
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    
    /**
     * Métadonnées additionnelles au format JSON.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * Date de création de la transaction.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Méthode utilitaire pour créer une transaction d'achat.
     * 
     * @param wallet Portefeuille
     * @param amount Montant acheté
     * @param description Description
     * @return TokenTransaction
     */
    public static TokenTransaction createPurchase(TokenWallet wallet, int amount, String description) {
        return TokenTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.PURCHASE)
                .amount(amount)
                .balanceAfter(wallet.getBalance() + amount)
                .description(description)
                .build();
    }
    
    /**
     * Méthode utilitaire pour créer une transaction de dépense.
     * 
     * @param wallet Portefeuille
     * @param amount Montant dépensé
     * @param referenceType Type de référence
     * @param referenceId ID de référence
     * @param description Description
     * @return TokenTransaction
     */
    public static TokenTransaction createSpend(
            TokenWallet wallet, 
            int amount, 
            String referenceType, 
            UUID referenceId,
            String description) {
        return TokenTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.SPEND)
                .amount(-amount)
                .balanceAfter(wallet.getBalance() - amount)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description(description)
                .build();
    }
    
    /**
     * Méthode utilitaire pour créer une transaction de gain.
     * 
     * @param wallet Portefeuille
     * @param amount Montant gagné
     * @param description Description
     * @return TokenTransaction
     */
    public static TokenTransaction createEarn(TokenWallet wallet, int amount, String description) {
        return TokenTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.EARN)
                .amount(amount)
                .balanceAfter(wallet.getBalance() + amount)
                .description(description)
                .build();
    }
}
