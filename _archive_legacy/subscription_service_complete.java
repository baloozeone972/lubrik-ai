package com.nexusai.auth.service;

import com.nexusai.auth.dto.request.SubscriptionRequest;
import com.nexusai.auth.dto.response.SubscriptionResponse;
import com.nexusai.auth.repository.SubscriptionRepository;
import com.nexusai.auth.repository.UserRepository;
import com.nexusai.core.domain.Subscription;
import com.nexusai.core.domain.User;
import com.nexusai.core.enums.SubscriptionPlan;
import com.nexusai.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des abonnements.
 * 
 * Gère toutes les opérations liées aux abonnements :
 * - Création d'abonnement
 * - Upgrade/Downgrade
 * - Annulation
 * - Renouvellement
 * - Calcul de prorata
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    /**
     * Récupère l'abonnement actif d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Subscription
     */
    public Subscription getUserSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Aucun abonnement trouvé pour l'utilisateur: " + userId
                ));
    }
    
    /**
     * Récupère tous les plans disponibles.
     * 
     * @return Liste des plans avec détails
     */
    public List<SubscriptionPlanInfo> getAvailablePlans() {
        return List.of(
            SubscriptionPlanInfo.builder()
                .plan(SubscriptionPlan.FREE)
                .name("Gratuit")
                .monthlyPrice(SubscriptionPlan.FREE.getMonthlyPrice())
                .features(List.of(
                    "100 messages par mois",
                    "1 compagnon",
                    "Génération d'images basique",
                    "Support communautaire"
                ))
                .build(),
                
            SubscriptionPlanInfo.builder()
                .plan(SubscriptionPlan.STANDARD)
                .name("Standard")
                .monthlyPrice(SubscriptionPlan.STANDARD.getMonthlyPrice())
                .features(List.of(
                    "1,000 messages par mois",
                    "3 compagnons",
                    "Génération d'images HD",
                    "Priorité de génération",
                    "Support email"
                ))
                .build(),
                
            SubscriptionPlanInfo.builder()
                .plan(SubscriptionPlan.PREMIUM)
                .name("Premium")
                .monthlyPrice(SubscriptionPlan.PREMIUM.getMonthlyPrice())
                .features(List.of(
                    "Messages illimités",
                    "10 compagnons",
                    "Génération d'images 4K",
                    "Génération vidéo (bêta)",
                    "Personnalisation avancée",
                    "Support prioritaire"
                ))
                .build(),
                
            SubscriptionPlanInfo.builder()
                .plan(SubscriptionPlan.VIP)
                .name("VIP")
                .monthlyPrice(SubscriptionPlan.VIP.getMonthlyPrice())
                .features(List.of(
                    "Tout Premium +",
                    "Compagnons illimités",
                    "Contenu exclusif",
                    "API access",
                    "Support 24/7",
                    "Badge VIP"
                ))
                .build(),
                
            SubscriptionPlanInfo.builder()
                .plan(SubscriptionPlan.VIP_PLUS)
                .name("VIP Plus")
                .monthlyPrice(SubscriptionPlan.VIP_PLUS.getMonthlyPrice())
                .features(List.of(
                    "Tout VIP +",
                    "Développement de features sur-mesure",
                    "Compte manager dédié",
                    "Accès anticipé aux nouvelles fonctionnalités",
                    "Formation personnalisée"
                ))
                .build()
        );
    }
    
    /**
     * Crée un nouvel abonnement.
     * 
     * @param userId ID de l'utilisateur
     * @param request Requête d'abonnement
     * @return Subscription créé
     */
    @Transactional
    public Subscription createSubscription(UUID userId, SubscriptionRequest request) {
        log.info("Création abonnement {} pour utilisateur {}", request.getPlan(), userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        // Vérifier si un abonnement existe déjà
        if (subscriptionRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("Un abonnement existe déjà pour cet utilisateur");
        }
        
        SubscriptionPlan plan = SubscriptionPlan.valueOf(request.getPlan());
        
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .startDate(LocalDateTime.now())
                .endDate(plan == SubscriptionPlan.FREE ? null : 
                        LocalDateTime.now().plusMonths(1))
                .autoRenewal(request.isAutoRenewal())
                .monthlyPrice(plan.getMonthlyPrice())
                .build();
        
        subscriptionRepository.save(subscription);
        
        log.info("Abonnement créé avec succès: {}", subscription.getId());
        
        return subscription;
    }
    
    /**
     * Upgrade un abonnement.
     * 
     * @param userId ID de l'utilisateur
     * @param newPlan Nouveau plan
     * @return Subscription mis à jour
     */
    @Transactional
    public Subscription upgradeSubscription(UUID userId, String newPlan) {
        log.info("Upgrade abonnement utilisateur {} vers {}", userId, newPlan);
        
        Subscription subscription = getUserSubscription(userId);
        SubscriptionPlan currentPlan = subscription.getPlan();
        SubscriptionPlan targetPlan = SubscriptionPlan.valueOf(newPlan);
        
        // Vérifier que c'est bien un upgrade
        if (targetPlan.getMonthlyPrice().compareTo(currentPlan.getMonthlyPrice()) <= 0) {
            throw new IllegalArgumentException("Le nouveau plan doit être supérieur au plan actuel");
        }
        
        // Calculer le crédit prorata
        BigDecimal prorata = calculateProrata(subscription, targetPlan);
        
        // Mettre à jour l'abonnement
        subscription.setPlan(targetPlan);
        subscription.setMonthlyPrice(targetPlan.getMonthlyPrice());
        
        if (subscription.getEndDate() != null) {
            // Étendre la date de fin en fonction du prorata
            long daysToAdd = prorata.divide(targetPlan.getMonthlyPrice()
                    .divide(BigDecimal.valueOf(30), 2, BigDecimal.ROUND_HALF_UP), 
                    0, BigDecimal.ROUND_DOWN).longValue();
            subscription.setEndDate(LocalDateTime.now().plusDays(daysToAdd).plusMonths(1));
        }
        
        subscriptionRepository.save(subscription);
        
        // Envoyer notification
        emailService.sendSubscriptionChangeEmail(
            subscription.getUser(),
            currentPlan.name(),
            targetPlan.name()
        );
        
        log.info("Abonnement upgradé avec succès. Crédit prorata: {}", prorata);
        
        return subscription;
    }
    
    /**
     * Downgrade un abonnement.
     * 
     * @param userId ID de l'utilisateur
     * @param newPlan Nouveau plan
     * @return Subscription mis à jour
     */
    @Transactional
    public Subscription downgradeSubscription(UUID userId, String newPlan) {
        log.info("Downgrade abonnement utilisateur {} vers {}", userId, newPlan);
        
        Subscription subscription = getUserSubscription(userId);
        SubscriptionPlan currentPlan = subscription.getPlan();
        SubscriptionPlan targetPlan = SubscriptionPlan.valueOf(newPlan);
        
        // Vérifier que c'est bien un downgrade
        if (targetPlan.getMonthlyPrice().compareTo(currentPlan.getMonthlyPrice()) >= 0) {
            throw new IllegalArgumentException("Le nouveau plan doit être inférieur au plan actuel");
        }
        
        // Le downgrade prend effet à la fin de la période actuelle
        subscription.setPlan(targetPlan);
        subscription.setMonthlyPrice(targetPlan.getMonthlyPrice());
        
        // Si le plan cible est FREE, définir autoRenewal à false
        if (targetPlan == SubscriptionPlan.FREE) {
            subscription.setAutoRenewal(false);
        }
        
        subscriptionRepository.save(subscription);
        
        // Envoyer notification
        emailService.sendSubscriptionChangeEmail(
            subscription.getUser(),
            currentPlan.name(),
            targetPlan.name()
        );
        
        log.info("Abonnement downgradé avec succès. Changement effectif à: {}", 
                subscription.getEndDate());
        
        return subscription;
    }
    
    /**
     * Annule un abonnement.
     * 
     * @param userId ID de l'utilisateur
     * @param immediately Si true, annulation immédiate
     * @return Subscription mis à jour
     */
    @Transactional
    public Subscription cancelSubscription(UUID userId, boolean immediately) {
        log.info("Annulation abonnement utilisateur {} (immédiat: {})", userId, immediately);
        
        Subscription subscription = getUserSubscription(userId);
        
        subscription.setAutoRenewal(false);
        
        if (immediately) {
            subscription.setPlan(SubscriptionPlan.FREE);
            subscription.setEndDate(LocalDateTime.now());
            subscription.setMonthlyPrice(BigDecimal.ZERO);
        }
        
        subscriptionRepository.save(subscription);
        
        log.info("Abonnement annulé. Fin de service: {}", 
                immediately ? "Immédiat" : subscription.getEndDate());
        
        return subscription;
    }
    
    /**
     * Réactive un abonnement annulé.
     * 
     * @param userId ID de l'utilisateur
     * @return Subscription réactivé
     */
    @Transactional
    public Subscription reactivateSubscription(UUID userId) {
        log.info("Réactivation abonnement utilisateur {}", userId);
        
        Subscription subscription = getUserSubscription(userId);
        
        if (subscription.getAutoRenewal()) {
            throw new IllegalStateException("L'abonnement n'est pas annulé");
        }
        
        subscription.setAutoRenewal(true);
        
        // Si expiré, renouveler immédiatement
        if (subscription.getEndDate() != null && 
            LocalDateTime.now().isAfter(subscription.getEndDate())) {
            subscription.setEndDate(LocalDateTime.now().plusMonths(1));
        }
        
        subscriptionRepository.save(subscription);
        
        log.info("Abonnement réactivé avec succès");
        
        return subscription;
    }
    
    /**
     * Renouvelle automatiquement les abonnements expirés.
     * Cette méthode doit être appelée par un scheduler.
     * 
     * @return Nombre d'abonnements renouvelés
     */
    @Transactional
    public int renewExpiredSubscriptions() {
        log.info("Démarrage du renouvellement automatique des abonnements");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Trouver les abonnements à renouveler
        List<Subscription> toRenew = subscriptionRepository.findAll().stream()
                .filter(s -> s.getAutoRenewal())
                .filter(s -> s.getEndDate() != null && s.getEndDate().isBefore(now))
                .toList();
        
        int renewed = 0;
        
        for (Subscription subscription : toRenew) {
            try {
                // TODO: Traiter le paiement avec Stripe
                
                // Prolonger l'abonnement
                subscription.setEndDate(subscription.getEndDate().plusMonths(1));
                subscriptionRepository.save(subscription);
                
                renewed++;
                
                log.info("Abonnement renouvelé: {}", subscription.getId());
            } catch (Exception e) {
                log.error("Erreur lors du renouvellement de l'abonnement {}: {}", 
                        subscription.getId(), e.getMessage());
            }
        }
        
        log.info("Renouvellement terminé. {} abonnements renouvelés", renewed);
        
        return renewed;
    }
    
    /**
     * Calcule le crédit prorata lors d'un changement d'abonnement.
     * 
     * @param subscription Abonnement actuel
     * @param newPlan Nouveau plan
     * @return Montant du crédit
     */
    private BigDecimal calculateProrata(Subscription subscription, SubscriptionPlan newPlan) {
        if (subscription.getEndDate() == null) {
            return BigDecimal.ZERO;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = ChronoUnit.DAYS.between(now, subscription.getEndDate());
        
        if (daysRemaining <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculer le montant non utilisé
        BigDecimal dailyRate = subscription.getMonthlyPrice()
                .divide(BigDecimal.valueOf(30), 2, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal unusedAmount = dailyRate.multiply(BigDecimal.valueOf(daysRemaining));
        
        return unusedAmount;
    }
    
    /**
     * Convertit une Subscription en SubscriptionResponse.
     * 
     * @param subscription Subscription
     * @return SubscriptionResponse
     */
    public SubscriptionResponse toResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .plan(subscription.getPlan().name())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .autoRenewal(subscription.getAutoRenewal())
                .monthlyPrice(subscription.getMonthlyPrice())
                .isActive(subscription.isActive())
                .build();
    }
    
    // ========== DTOs ==========
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubscriptionPlanInfo {
        private SubscriptionPlan plan;
        private String name;
        private BigDecimal monthlyPrice;
        private List<String> features;
    }
}
