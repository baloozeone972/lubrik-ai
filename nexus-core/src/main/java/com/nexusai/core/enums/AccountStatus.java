package com.nexusai.core.enums;

/**
 * Account status for users.
 */
public enum AccountStatus {
    PENDING,    // Email not verified
    ACTIVE,     // Active account
    SUSPENDED,  // Temporarily suspended
    DELETED     // Soft deleted
}
