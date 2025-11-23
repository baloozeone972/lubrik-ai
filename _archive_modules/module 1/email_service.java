package com.nexusai.auth.service;

import com.nexusai.core.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service d'envoi d'emails.
 * 
 * Gère l'envoi de tous les types d'emails de l'application :
 * - Vérification d'email
 * - Réinitialisation de mot de passe
 * - Notifications
 * - Alertes
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public interface EmailService {
    
    /**
     * Envoie un email de vérification.
     * 
     * @param user Utilisateur
     * @param verificationToken Token de vérification
     */
    void sendVerificationEmail(User user, String verificationToken);
    
    /**
     * Envoie un email de bienvenue.
     * 
     * @param user Utilisateur
     */
    void sendWelcomeEmail(User user);
    
    /**
     * Envoie un email de réinitialisation de mot de passe.
     * 
     * @param user Utilisateur
     * @param resetToken Token de réinitialisation
     */
    void sendPasswordResetEmail(User user, String resetToken);
    
    /**
     * Envoie une confirmation de changement de mot de passe.
     * 
     * @param user Utilisateur
     */
    void sendPasswordChangeConfirmation(User user);
    
    /**
     * Envoie une notification de changement d'abonnement.
     * 
     * @param user Utilisateur
     * @param oldPlan Ancien plan
     * @param newPlan Nouveau plan
     */
    void sendSubscriptionChangeEmail(User user, String oldPlan, String newPlan);
    
    /**
     * Envoie une notification de paiement réussi.
     * 
     * @param user Utilisateur
     * @param amount Montant
     * @param description Description
     */
    void sendPaymentSuccessEmail(User user, Double amount, String description);
}

/**
 * Implémentation du service d'envoi d'emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    @Value("${app.url:http://localhost:3000}")
    private String appUrl;
    
    @Value("${app.name:NexusAI}")
    private String appName;
    
    @Value("${app.support.email:support@nexusai.com}")
    private String supportEmail;
    
    // TODO: Injecter JavaMailSender ou SendGrid client
    // private final JavaMailSender mailSender;
    
    @Override
    public void sendVerificationEmail(User user, String verificationToken) {
        log.info("Envoi email de vérification à: {}", user.getEmail());
        
        String verificationUrl = appUrl + "/verify-email?token=" + verificationToken;
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("verificationUrl", verificationUrl);
        variables.put("appName", appName);
        
        String subject = "Vérifiez votre adresse email - " + appName;
        String htmlContent = buildVerificationEmailTemplate(variables);
        
        sendEmail(user.getEmail(), subject, htmlContent);
    }
    
    @Override
    public void sendWelcomeEmail(User user) {
        log.info("Envoi email de bienvenue à: {}", user.getEmail());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("appName", appName);
        variables.put("appUrl", appUrl);
        
        String subject = "Bienvenue sur " + appName + " !";
        String htmlContent = buildWelcomeEmailTemplate(variables);
        
        sendEmail(user.getEmail(), subject, htmlContent);
    }
    
    @Override
    public void sendPasswordResetEmail(User user, String resetToken) {
        log.info("Envoi email de réinitialisation à: {}", user.getEmail());
        
        String resetUrl = appUrl + "/reset-password?token=" + resetToken;
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("resetUrl", resetUrl);
        variables.put("appName", appName);
        variables.put("expirationTime", "1 heure");
        
        String subject = "Réinitialisation de votre mot de passe - " + appName;
        String htmlContent = buildPasswordResetEmailTemplate(variables);
        
        sendEmail(user.getEmail(), subject, htmlContent);
    }
    
    @Override
    public void sendPasswordChangeConfirmation(User user) {
        log.info("Envoi confirmation changement mot de passe à: {}", user.getEmail());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("changeDate", LocalDateTime.now().toString());
        variables.put("appName", appName);
        variables.put("supportEmail", supportEmail);
        
        String subject = "Votre mot de passe a été modifié - " + appName;
        String htmlContent = buildPasswordChangeConfirmationTemplate(variables);
        
        sendEmail(user.getEmail(), subject, htmlContent);
    }
    
    @Override
    public void sendSubscriptionChangeEmail(User user, String oldPlan, String newPlan) {
        log.info("Envoi notification changement abonnement à: {}", user.getEmail());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("oldPlan", oldPlan);
        variables.put("newPlan", newPlan);
        variables.put("appName", appName);
        
        String subject = "Votre abonnement a été modifié - " + appName;
        String htmlContent = buildSubscriptionChangeTemplate(variables);
        
        sendEmail(user.getEmail(), subject, htmlContent);
    }
    
    @Override
    public void sendPaymentSuccessEmail(User user, Double amount, String description) {
        log.info("Envoi confirmation paiement à: {}", user.getEmail());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("amount", String.format("%.2f", amount));
        variables.put("description", description);
        variables.put("date", LocalDateTime.now().toString());
        variables.put("appName", appName);
        
        String subject = "Confirmation de paiement - " + appName;
        String htmlContent = buildPaymentSuccessTemplate(variables);
        
        sendEmail(user.getEmail(), subject, htmlContent);
    }
    
    /**
     * Envoie un email.
     * 
     * @param to Destinataire
     * @param subject Sujet
     * @param htmlContent Contenu HTML
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            // TODO: Implémenter l'envoi réel avec JavaMailSender ou SendGrid
            log.info("Email envoyé à {} avec le sujet: {}", to, subject);
            
            /* Exemple avec JavaMailSender:
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(supportEmail);
            
            mailSender.send(message);
            */
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi d'email à {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi d'email", e);
        }
    }
    
    // ========== TEMPLATES EMAIL ==========
    
    private String buildVerificationEmailTemplate(Map<String, Object> vars) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #007bff;
                             color: white; text-decoration: none; border-radius: 5px; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Vérifiez votre adresse email</h2>
                    <p>Bonjour %s,</p>
                    <p>Merci de vous être inscrit sur %s ! Pour activer votre compte, 
                       veuillez vérifier votre adresse email en cliquant sur le bouton ci-dessous :</p>
                    <p><a href="%s" class="button">Vérifier mon email</a></p>
                    <p>Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :</p>
                    <p>%s</p>
                    <p class="footer">Ce lien expire dans 24 heures.</p>
                </div>
            </body>
            </html>
            """.formatted(
                vars.get("username"),
                vars.get("appName"),
                vars.get("verificationUrl"),
                vars.get("verificationUrl")
            );
    }
    
    private String buildWelcomeEmailTemplate(Map<String, Object> vars) {
        return """
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Bienvenue sur %s !</h2>
                <p>Bonjour %s,</p>
                <p>Nous sommes ravis de vous accueillir ! Votre compte a été créé avec succès.</p>
                <p>Vous pouvez maintenant profiter de toutes nos fonctionnalités.</p>
                <p><a href="%s">Commencer maintenant</a></p>
            </body>
            </html>
            """.formatted(
                vars.get("appName"),
                vars.get("username"),
                vars.get("appUrl")
            );
    }
    
    private String buildPasswordResetEmailTemplate(Map<String, Object> vars) {
        return """
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Réinitialisation de votre mot de passe</h2>
                <p>Bonjour %s,</p>
                <p>Vous avez demandé la réinitialisation de votre mot de passe.</p>
                <p><a href="%s">Réinitialiser mon mot de passe</a></p>
                <p>Ce lien expire dans %s.</p>
                <p>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>
            </body>
            </html>
            """.formatted(
                vars.get("username"),
                vars.get("resetUrl"),
                vars.get("expirationTime")
            );
    }
    
    private String buildPasswordChangeConfirmationTemplate(Map<String, Object> vars) {
        return """
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Confirmation de changement de mot de passe</h2>
                <p>Bonjour %s,</p>
                <p>Votre mot de passe a été modifié avec succès le %s.</p>
                <p>Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement : %s</p>
            </body>
            </html>
            """.formatted(
                vars.get("username"),
                vars.get("changeDate"),
                vars.get("supportEmail")
            );
    }
    
    private String buildSubscriptionChangeTemplate(Map<String, Object> vars) {
        return """
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Votre abonnement a été modifié</h2>
                <p>Bonjour %s,</p>
                <p>Votre abonnement est passé de %s à %s.</p>
                <p>Les modifications prendront effet immédiatement.</p>
            </body>
            </html>
            """.formatted(
                vars.get("username"),
                vars.get("oldPlan"),
                vars.get("newPlan")
            );
    }
    
    private String buildPaymentSuccessTemplate(Map<String, Object> vars) {
        return """
            <!DOCTYPE html>
            <html>
            <body>
                <h2>Confirmation de paiement</h2>
                <p>Bonjour %s,</p>
                <p>Votre paiement de %s € a été traité avec succès.</p>
                <p>Description : %s</p>
                <p>Date : %s</p>
                <p>Merci pour votre confiance !</p>
            </body>
            </html>
            """.formatted(
                vars.get("username"),
                vars.get("amount"),
                vars.get("description"),
                vars.get("date")
            );
    }
}
