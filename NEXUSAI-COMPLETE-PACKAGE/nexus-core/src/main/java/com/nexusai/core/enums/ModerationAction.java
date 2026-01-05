package com.nexusai.core.enums;

/**
 * Action prise suite à une modération.
 */
public enum ModerationAction {
    NONE,               // Aucune action
    CONTENT_DELETED,    // Contenu supprimé
    USER_WARNED,        // Utilisateur averti
    USER_SUSPENDED,     // Utilisateur suspendu (temporaire)
    USER_BANNED,        // Utilisateur banni (permanent)
    REPORTED_AUTHORITY  // Signalé aux autorités (contenu illégal)
}
