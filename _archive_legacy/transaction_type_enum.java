package com.nexusai.core.enums;

/**
 * Énumération des types de transactions de jetons.
 * 
 * Définit tous les types d'opérations possibles sur le portefeuille
 * de jetons d'un utilisateur.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
public enum TransactionType {
    
    /**
     * Achat de jetons (paiement).
     */
    PURCHASE("Achat de jetons", true),
    
    /**
     * Gain de jetons (bonus, récompense).
     */
    EARN("Gain de jetons", true),
    
    /**
     * Dépense de jetons (consommation).
     */
    SPEND("Dépense de jetons", false),
    
    /**
     * Remboursement de jetons.
     */
    REFUND("Remboursement", true),
    
    /**
     * Expiration de jetons.
     */
    EXPIRE("Expiration de jetons", false),
    
    /**
     * Bonus quotidien.
     */
    DAILY_BONUS("Bonus quotidien", true),
    
    /**
     * Cadeau de bienvenue.
     */
    WELCOME_GIFT("Cadeau de bienvenue", true),
    
    /**
     * Récompense de parrainage.
     */
    REFERRAL_REWARD("Récompense de parrainage", true),
    
    /**
     * Ajustement administratif.
     */
    ADMIN_ADJUSTMENT("Ajustement administratif", true);
    
    private final String displayName;
    private final boolean isCredit;
    
    /**
     * Constructeur.
     * 
     * @param displayName Nom d'affichage
     * @param isCredit true si c'est un crédit (ajout), false si débit
     */
    TransactionType(String displayName, boolean isCredit) {
        this.displayName = displayName;
        this.isCredit = isCredit;
    }
    
    /**
     * Retourne le nom d'affichage.
     * 
     * @return Nom d'affichage
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Indique si c'est un crédit (ajout de jetons).
     * 
     * @return true si crédit, false si débit
     */
    public boolean isCredit() {
        return isCredit;
    }
    
    /**
     * Indique si c'est un débit (retrait de jetons).
     * 
     * @return true si débit, false si crédit
     */
    public boolean isDebit() {
        return !isCredit;
    }
}
