package com.nexusai.payment.dto;


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
