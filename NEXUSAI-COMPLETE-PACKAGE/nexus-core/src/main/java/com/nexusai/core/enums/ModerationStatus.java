package com.nexusai.core.enums;

/**
 * Statut de modération d'un contenu.
 */
public enum ModerationStatus {
    PENDING,        // En attente de modération
    APPROVED,       // Approuvé (auto ou manuel)
    FLAGGED,        // Signalé pour revue manuelle
    REJECTED,       // Rejeté
    APPEALED        // En appel
}
