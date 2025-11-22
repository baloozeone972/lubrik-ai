package com.nexusai.auth.scheduler;

import com.nexusai.auth.repository.*;
import com.nexusai.auth.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tâches planifiées pour la maintenance automatique.
 * 
 * Exécute des tâches périodiques :
 * - Nettoyage des anciennes données
 * - Renouvellement des abonnements
 * - Expiration des tokens
 * - Statistiques quotidiennes
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasks {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final AuditLogRepository auditLogRepository;
    private final SubscriptionService subscriptionService;
    
    /**
     * Nettoie les tokens expirés.
     * Exécuté toutes les heures.
     */
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures
    @Transactional
    public void cleanExpiredTokens() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("Démarrage du nettoyage des tokens expirés");
        log.info("═══════════════════════════════════════════════════════");
        
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            
            // Supprimer les refresh tokens expirés depuis plus de 30 jours
            refreshTokenRepository.deleteByExpiresAtBefore(cutoffDate);
            log.info("✓ Refresh tokens expirés nettoyés");
            
            // Marquer les vérifications d'email expirées
            emailVerificationRepository.markExpiredVerifications(LocalDateTime.now());
            log.info("✓ Vérifications d'email expirées marquées");
            
            // Supprimer les anciennes vérifications d'email (90 jours)
            emailVerificationRepository.deleteByCreatedAtBefore(
                LocalDateTime.now().minusDays(90)
            );
            log.info("✓ Anciennes vérifications d'email supprimées");
            
            // Supprimer les anciennes réinitialisations de mot de passe (90 jours)
            passwordResetRepository.deleteByCreatedAtBefore(
                LocalDateTime.now().minusDays(90)
            );
            log.info("✓ Anciennes réinitialisations de mot de passe supprimées");
            
            log.info("═══════════════════════════════════════════════════════");
            log.info("Nettoyage des tokens terminé avec succès");
            log.info("═══════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des tokens: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Nettoie les anciens logs d'audit.
     * Exécuté tous les jours à 2h du matin.
     */
    @Scheduled(cron = "0 0 2 * * *") // Tous les jours à 2h
    @Transactional
    public void cleanOldAuditLogs() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("Démarrage du nettoyage des logs d'audit");
        log.info("═══════════════════════════════════════════════════════");
        
        try {
            // Garder les logs d'audit pendant 1 an
            LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);
            
            long countBefore = auditLogRepository.count();
            
            // Note: Cette méthode devrait être implémentée dans AuditLogRepository
            // auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
            
            long countAfter = auditLogRepository.count();
            long deleted = countBefore - countAfter;
            
            log.info("✓ {} logs d'audit supprimés (plus de 1 an)", deleted);
            
            log.info("═══════════════════════════════════════════════════════");
            log.info("Nettoyage des logs d'audit terminé");
            log.info("═══════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des logs d'audit: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Renouvelle automatiquement les abonnements.
     * Exécuté tous les jours à 3h du matin.
     */
    @Scheduled(cron = "0 0 3 * * *") // Tous les jours à 3h
    @Transactional
    public void renewSubscriptions() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("Démarrage du renouvellement automatique des abonnements");
        log.info("═══════════════════════════════════════════════════════");
        
        try {
            int renewed = subscriptionService.renewExpiredSubscriptions();
            
            log.info("✓ {} abonnement(s) renouvelé(s)", renewed);
            
            log.info("═══════════════════════════════════════════════════════");
            log.info("Renouvellement des abonnements terminé");
            log.info("═══════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("Erreur lors du renouvellement des abonnements: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Génère des statistiques quotidiennes.
     * Exécuté tous les jours à 23h59.
     */
    @Scheduled(cron = "0 59 23 * * *") // Tous les jours à 23h59
    public void generateDailyStatistics() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("Génération des statistiques quotidiennes");
        log.info("═══════════════════════════════════════════════════════");
        
        try {
            LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime tomorrow = today.plusDays(1);
            
            // Compter les nouvelles inscriptions du jour
            // Note: Nécessite une méthode dans UserRepository
            // long newUsers = userRepository.countByCreatedAtBetween(today, tomorrow);
            
            // Compter les connexions du jour
            long loginCount = auditLogRepository.findByDateRange(
                today, tomorrow, org.springframework.data.domain.Pageable.unpaged()
            ).stream()
                .filter(log -> log.getAction() == com.nexusai.core.enums.AuditAction.USER_LOGIN)
                .count();
            
            log.info("📊 Statistiques du jour :");
            log.info("   - Connexions : {}", loginCount);
            // log.info("   - Nouvelles inscriptions : {}", newUsers);
            
            log.info("═══════════════════════════════════════════════════════");
            log.info("Génération des statistiques terminée");
            log.info("═══════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération des statistiques: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Vérifie la santé du système.
     * Exécuté toutes les 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    public void healthCheck() {
        log.debug("Health check automatique - Système opérationnel");
        
        // Vérifier la connectivité à la base de données
        try {
            long userCount = auditLogRepository.count();
            log.debug("✓ Base de données accessible ({} logs)", userCount);
        } catch (Exception e) {
            log.error("✗ Problème de connectivité à la base de données", e);
        }
        
        // Autres vérifications possibles :
        // - Connectivité Redis
        // - Espace disque
        // - Mémoire disponible
        // - Etc.
    }
    
    /**
     * Envoie un rapport hebdomadaire.
     * Exécuté tous les lundis à 9h.
     */
    @Scheduled(cron = "0 0 9 * * MON") // Tous les lundis à 9h
    public void sendWeeklyReport() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("Génération du rapport hebdomadaire");
        log.info("═══════════════════════════════════════════════════════");
        
        try {
            LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
            LocalDateTime now = LocalDateTime.now();
            
            // Collecter les statistiques de la semaine
            long weeklyLogins = auditLogRepository.findByDateRange(
                weekAgo, now, org.springframework.data.domain.Pageable.unpaged()
            ).stream()
                .filter(log -> log.getAction() == com.nexusai.core.enums.AuditAction.USER_LOGIN)
                .count();
            
            log.info("📊 Rapport de la semaine :");
            log.info("   - Connexions : {}", weeklyLogins);
            
            // TODO: Envoyer par email aux administrateurs
            
            log.info("═══════════════════════════════════════════════════════");
            log.info("Rapport hebdomadaire généré");
            log.info("═══════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport: {}", e.getMessage(), e);
        }
    }
}

/**
 * Configuration pour activer le scheduling.
 * À ajouter dans NexusAuthApplication.java :
 * 
 * @EnableScheduling
 * public class NexusAuthApplication {
 *     ...
 * }
 */
