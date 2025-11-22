package com.nexusai.auth.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Represents the authenticated user principal.
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    private UUID userId;
    private String email;
    private String role;
}
