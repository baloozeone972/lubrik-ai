package com.nexusai.core.enums;

/**
 * Statut d'un fichier média.
 */
public enum MediaStatus {
    UPLOADING,   // En cours d'upload
    PROCESSING,  // En cours de traitement (thumbnail, compression, etc.)
    ACTIVE,      // Actif et accessible
    DELETED      // Supprimé (soft delete)
}
