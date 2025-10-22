package com.nexusai.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ══════════════════════════════════════════════════════════════
 * REQUEST DTOs
 * ══════════════════════════════════════════════════════════════
 */

/**
 * DTO pour la création/modification d'un abonnement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {
    
    @NotBlank(message = "Le plan est obligatoire")
    @Pattern(regexp = "FREE|STANDARD|PREMIUM|VIP|VIP_PLUS", 
             message = "Plan invalide")
    private String plan;
    
    private boolean autoRenewal = true;
    
    private String paymentMethodId; // Stripe payment method ID
}

// ══════════════════════════════════════════════════════════════

package com.nexusai.auth.dto.response;

import com.nexusai.core.enums.SubscriptionPlan;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ══════════════════════════════════════════════════════════════
 * RESPONSE DTOs
 * ══════════════════════════════════════════════════════════════
 */

/**
 * DTO de réponse pour un abonnement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    
    private UUID id;
    
    private String plan;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    private Boolean autoRenewal;
    
    private BigDecimal monthlyPrice;
    
    private Boolean isActive;
    
    private String status;
    
    private Long daysRemaining;
    
    /**
     * Calcule les jours restants avant expiration.
     */
    public Long calculateDaysRemaining() {
        if (endDate == null) {
            return null;
        }
        
        long days = java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now(), 
            endDate
        );
        
        return Math.max(0, days);
    }
}
