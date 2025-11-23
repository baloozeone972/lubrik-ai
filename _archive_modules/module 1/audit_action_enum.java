package com.nexusai.core.enums;

/**
 * Énumération des actions auditées dans l'application.
 * 
 * Définit tous les types d'actions importantes qui doivent être
 * tracées dans le journal d'audit pour la sécurité et la conformité.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
public enum AuditAction {
    
    // ========== ACTIONS UTILISATEUR ==========
    
    /**
     * Création d'un nouveau compte utilisateur.
     */
    USER_REGISTER("Inscription utilisateur", "INFO"),
    
    /**
     * Connexion réussie.
     */
    USER_LOGIN("Connexion réussie", "INFO"),
    
    /**
     * Tentative de connexion échouée.
     */
    USER_LOGIN_FAILED("Connexion échouée", "WARNING"),
    
    /**
     * Déconnexion.
     */
    USER_LOGOUT("Déconnexion", "INFO"),
    
    /**
     * Modification du profil utilisateur.
     */
    USER_PROFILE_UPDATE("Modification profil", "INFO"),
    
    /**
     * Changement de mot de passe.
     */
    USER_PASSWORD_CHANGE("Changement mot de passe", "INFO"),
    
    /**
     * Réinitialisation de mot de passe.
     */
    USER_PASSWORD_RESET("Réinitialisation mot de passe", "INFO"),
    
    /**
     * Vérification d'email.
     */
    USER_EMAIL_VERIFIED("Email vérifié", "INFO"),
    
    /**
     * Suppression de compte.
     */
    USER_ACCOUNT_DELETE("Suppression compte", "WARNING"),
    
    /**
     * Verrouillage de compte.
     */
    USER_ACCOUNT_LOCK("Verrouillage compte", "WARNING"),
    
    /**
     * Déverrouillage de compte.
     */
    USER_ACCOUNT_UNLOCK("Déverrouillage compte", "INFO"),
    
    // ========== ACTIONS ABONNEMENT ==========
    
    /**
     * Création d'un abonnement.
     */
    SUBSCRIPTION_CREATE("Création abonnement", "INFO"),
    
    /**
     * Mise à niveau d'abonnement.
     */
    SUBSCRIPTION_UPGRADE("Upgrade abonnement", "INFO"),
    
    /**
     * Rétrogradation d'abonnement.
     */
    SUBSCRIPTION_DOWNGRADE("Downgrade abonnement", "INFO"),
    
    /**
     * Annulation d'abonnement.
     */
    SUBSCRIPTION_CANCEL("Annulation abonnement", "INFO"),
    
    /**
     * Renouvellement d'abonnement.
     */
    SUBSCRIPTION_RENEW("Renouvellement abonnement", "INFO"),
    
    // ========== ACTIONS TOKENS ==========
    
    /**
     * Achat de jetons.
     */
    TOKEN_PURCHASE("Achat jetons", "INFO"),
    
    /**
     * Utilisation de jetons.
     */
    TOKEN_SPEND("Utilisation jetons", "INFO"),
    
    /**
     * Gain de jetons (bonus, etc.).
     */
    TOKEN_EARN("Gain jetons", "INFO"),
    
    /**
     * Remboursement de jetons.
     */
    TOKEN_REFUND("Remboursement jetons", "WARNING"),
    
    // ========== ACTIONS PAIEMENT ==========
    
    /**
     * Paiement réussi.
     */
    PAYMENT_SUCCESS("Paiement réussi", "INFO"),
    
    /**
     * Paiement échoué.
     */
    PAYMENT_FAILED("Paiement échoué", "ERROR"),
    
    /**
     * Remboursement effectué.
     */
    PAYMENT_REFUND("Remboursement effectué", "WARNING"),
    
    // ========== ACTIONS SÉCURITÉ ==========
    
    /**
     * Tentative d'accès non autorisé.
     */
    SECURITY_UNAUTHORIZED_ACCESS("Accès non autorisé", "ERROR"),
    
    /**
     * Détection d'activité suspecte.
     */
    SECURITY_SUSPICIOUS_ACTIVITY("Activité suspecte", "CRITICAL"),
    
    /**
     * Échec de validation 2FA.
     */
    SECURITY_2FA_FAILED("Échec 2FA", "WARNING"),
    
    /**
     * Révocation de token.
     */
    SECURITY_TOKEN_REVOKED("Révocation token", "WARNING"),
    
    // ========== ACTIONS DONNÉES ==========
    
    /**
     * Création de données.
     */
    DATA_CREATE("Création données", "INFO"),
    
    /**
     * Modification de données.
     */
    DATA_UPDATE("Modification données", "INFO"),
    
    /**
     * Suppression de données.
     */
    DATA_DELETE("Suppression données", "WARNING"),
    
    /**
     * Export de données (RGPD).
     */
    DATA_EXPORT("Export données", "INFO"),
    
    // ========== ACTIONS ADMIN ==========
    
    /**
     * Action administrative.
     */
    ADMIN_ACTION("Action administrateur", "WARNING"),
    
    /**
     * Modification de rôle utilisateur.
     */
    ADMIN_ROLE_CHANGE("Modification rôle", "WARNING"),
    
    /**
     * Configuration système modifiée.
     */
    ADMIN_CONFIG_CHANGE("Modification configuration", "WARNING"),
    
    /**
     * Exécution de script admin.
     */
    ADMIN_SCRIPT_EXECUTION("Exécution script", "CRITICAL");
    
    private final String displayName;
    private final String defaultSeverity;
    
    /**
     * Constructeur.
     * 
     * @param displayName Nom d'affichage
     * @param defaultSeverity Sévérité par défaut
     */
    AuditAction(String displayName, String defaultSeverity) {
        this.displayName = displayName;
        this.defaultSeverity = defaultSeverity;
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
     * Retourne la sévérité par défaut.
     * 
     * @return Sévérité par défaut
     */
    public String getDefaultSeverity() {
        return defaultSeverity;
    }
    
    /**
     * Indique si c'est une action sensible nécessitant une attention particulière.
     * 
     * @return true si action sensible, false sinon
     */
    public boolean isSensitive() {
        return defaultSeverity.equals("WARNING") 
            || defaultSeverity.equals("ERROR") 
            || defaultSeverity.equals("CRITICAL");
    }
}
