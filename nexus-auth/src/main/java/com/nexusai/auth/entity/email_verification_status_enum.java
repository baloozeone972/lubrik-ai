package com.nexusai.core.enums;

/**
 * Énumération des statuts de vérification d'email.
 * 
 * Représente les différents états possibles d'une demande
 * de vérification d'adresse email.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
public enum EmailVerificationStatus {
    
    /**
     * Vérification en attente.
     */
    PENDING("En attente", false),
    
    /**
     * Email vérifié avec succès.
     */
    VERIFIED("Vérifié", true),
    
    /**
     * Token de vérification expiré.
     */
    EXPIRED("Expiré", false),
    
    /**
     * Token invalide ou révoqué.
     */
    INVALID("Invalide", false),
    
    /**
     * Email déjà vérifié par ailleurs.
     */
    ALREADY_VERIFIED("Déjà vérifié", true),
    
    /**
     * Vérification annulée par l'utilisateur.
     */
    CANCELLED("Annulée", false);
    
    private final String displayName;
    private final boolean isSuccess;
    
    /**
     * Constructeur.
     * 
     * @param displayName Nom d'affichage
     * @param isSuccess true si c'est un statut de succès
     */
    EmailVerificationStatus(String displayName, boolean isSuccess) {
        this.displayName = displayName;
        this.isSuccess = isSuccess;
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
     * Indique si c'est un statut de succès.
     * 
     * @return true si succès, false sinon
     */
    public boolean isSuccess() {
        return isSuccess;
    }
    
    /**
     * Indique si la vérification peut être réessayée.
     * 
     * @return true si réessai possible, false sinon
     */
    public boolean canRetry() {
        return this == EXPIRED || this == INVALID;
    }
}
