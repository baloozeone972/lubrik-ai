package com.nexusai.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO représentant une session de checkout Stripe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionDTO {
    
    /**
     * ID de la session Stripe
     */
    private String sessionId;
    
    /**
     * URL de redirection vers Stripe Checkout
     */
    private String sessionUrl;
}
