package com.nexusai.payment.dto;
import com.nexusai.core.enums.SubscriptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO représentant un abonnement utilisateur.
 *
 * <p>Contient toutes les informations nécessaires pour afficher
 * et gérer un abonnement côté client.</p>
 *
 * @author Developer 1
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDTO {

    /** Identifiant unique de l'abonnement */
    private UUID id;

    /** Identifiant de l'utilisateur */
    private UUID userId;

    /** Plan d'abonnement */
    private SubscriptionType plan;

    /** Statut de l'abonnement */
    private SubscriptionStatus status;

    /** Date de début */
    private Instant startDate;

    /** Date de fin (null si actif) */
    private Instant endDate;

    /** Renouvellement automatique activé */
    private Boolean autoRenewal;

    /** Prix mensuel */
    private BigDecimal monthlyPrice;

    /** ID Stripe de l'abonnement */
    private String stripeSubscriptionId;

    /** Date de création */
    private Instant createdAt;

    /** Date de dernière modification */
    private Instant updatedAt;
}
