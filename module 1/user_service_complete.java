package com.nexusai.auth.service;

import com.nexusai.auth.dto.request.UpdateUserRequest;
import com.nexusai.auth.dto.response.UserResponse;
import com.nexusai.auth.dto.response.UserStatisticsResponse;
import com.nexusai.auth.mapper.UserMapper;
import com.nexusai.auth.repository.AuditLogRepository;
import com.nexusai.auth.repository.TokenWalletRepository;
import com.nexusai.auth.repository.UserRepository;
import com.nexusai.core.domain.AuditLog;
import com.nexusai.core.domain.User;
import com.nexusai.core.enums.AuditAction;
import com.nexusai.core.exception.ResourceNotFoundException;
import com.nexusai.core.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service complet de gestion des utilisateurs.
 * 
 * Gère toutes les opérations liées aux utilisateurs :
 * - Récupération et mise à jour de profil
 * - Suppression de compte
 * - Statistiques utilisateur
 * - Recherche et listing (admin)
 * - Upload d'avatar
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final TokenWalletRepository tokenWalletRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    // TODO: Injecter un service de stockage pour les avatars (S3, MinIO, etc.)
    
    /**
     * Récupère un utilisateur par son ID.
     * 
     * @param userId ID de l'utilisateur
     * @return User
     * @throws ResourceNotFoundException Si l'utilisateur n'existe pas
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Utilisateur non trouvé: " + userId
                ));
    }
    
    /**
     * Récupère un utilisateur par email.
     * 
     * @param email Email de l'utilisateur
     * @return User
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Utilisateur non trouvé avec l'email: " + email
                ));
    }
    
    /**
     * Récupère un utilisateur par username.
     * 
     * @param username Nom d'utilisateur
     * @return User
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Utilisateur non trouvé: " + username
                ));
    }
    
    /**
     * Met à jour le profil utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param request Données de mise à jour
     * @param ipAddress Adresse IP
     * @return UserResponse mis à jour
     */
    @Transactional
    public UserResponse updateUserProfile(
            UUID userId, 
            UpdateUserRequest request,
            String ipAddress) {
        
        log.info("Mise à jour du profil utilisateur: {}", userId);
        
        User user = getUserById(userId);
        
        // Sauvegarder les anciennes valeurs pour l'audit
        String oldEmail = user.getEmail();
        String oldUsername = user.getUsername();
        
        // Mettre à jour l'email si fourni et différent
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Cet email est déjà utilisé");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Nécessite une nouvelle vérification
            log.info("Email modifié de {} à {}", oldEmail, request.getEmail());
        }
        
        // Mettre à jour le username si fourni et différent
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Ce nom d'utilisateur est déjà utilisé");
            }
            user.setUsername(request.getUsername());
            log.info("Username modifié de {} à {}", oldUsername, request.getUsername());
        }
        
        userRepository.save(user);
        
        // Log d'audit
        auditLogRepository.save(
            AuditLog.createUpdate(
                user,
                "User",
                userId,
                String.format("{\"email\":\"%s\",\"username\":\"%s\"}", oldEmail, oldUsername),
                String.format("{\"email\":\"%s\",\"username\":\"%s\"}", user.getEmail(), user.getUsername()),
                ipAddress
            )
        );
        
        log.info("Profil utilisateur mis à jour avec succès");
        
        return userMapper.toUserResponse(user);
    }
    
    /**
     * Supprime le compte utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param password Mot de passe pour confirmation
     * @param ipAddress Adresse IP
     */
    @Transactional
    public void deleteAccount(UUID userId, String password, String ipAddress) {
        log.warn("Demande de suppression de compte: {}", userId);
        
        User user = getUserById(userId);
        
        // Vérifier le mot de passe
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Mot de passe incorrect");
        }
        
        // Log d'audit avant suppression
        auditLogRepository.save(
            AuditLog.builder()
                .user(user)
                .action(AuditAction.USER_ACCOUNT_DELETE)
                .entityType("User")
                .entityId(userId)
                .description("Compte supprimé par l'utilisateur")
                .ipAddress(ipAddress)
                .result("SUCCESS")
                .severity("WARNING")
                .build()
        );
        
        // Supprimer l'utilisateur (cascade sur les entités liées)
        userRepository.delete(user);
        
        log.warn("Compte utilisateur supprimé: {}", userId);
    }
    
    /**
     * Récupère les statistiques d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return UserStatisticsResponse
     */
    public UserStatisticsResponse getUserStatistics(UUID userId) {
        User user = getUserById(userId);
        
        // Calculer les statistiques
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accountAge = user.getCreatedAt();
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
            accountAge, now
        );
        
        // Récupérer les statistiques de tokens
        var wallet = tokenWalletRepository.findByUserId(userId);
        int tokenBalance = wallet.map(w -> w.getBalance()).orElse(0);
        int totalTokensEarned = wallet.map(w -> w.getTotalEarned()).orElse(0);
        int totalTokensSpent = wallet.map(w -> w.getTotalSpent()).orElse(0);
        
        // Compter les connexions
        long loginCount = auditLogRepository.findAll().stream()
                .filter(log -> log.getUser() != null && log.getUser().getId().equals(userId))
                .filter(log -> log.getAction() == AuditAction.USER_LOGIN)
                .count();
        
        return UserStatisticsResponse.builder()
                .userId(userId)
                .accountAge(daysSinceCreation)
                .subscriptionPlan(user.getSubscription() != null ? 
                    user.getSubscription().getPlan().name() : "FREE")
                .tokenBalance(tokenBalance)
                .totalTokensEarned(totalTokensEarned)
                .totalTokensSpent(totalTokensSpent)
                .loginCount(loginCount)
                .lastLogin(user.getLastLogin())
                .accountCreated(user.getCreatedAt())
                .emailVerified(user.getEmailVerified())
                .build();
    }
    
    /**
     * Upload un avatar utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param file Fichier image
     * @return URL de l'avatar
     */
    @Transactional
    public String uploadAvatar(UUID userId, MultipartFile file) {
        log.info("Upload avatar pour utilisateur: {}", userId);
        
        // Valider le fichier
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }
        
        // Vérifier le type MIME
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image");
        }
        
        // Vérifier la taille (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("L'image ne doit pas dépasser 5MB");
        }
        
        User user = getUserById(userId);
        
        // TODO: Implémenter le stockage réel (S3, MinIO, etc.)
        // Pour l'instant, on génère juste une URL fictive
        String avatarUrl = "https://storage.nexusai.com/avatars/" + userId + ".jpg";
        
        log.info("Avatar uploadé avec succès: {}", avatarUrl);
        
        return avatarUrl;
    }
    
    /**
     * Recherche des utilisateurs (admin uniquement).
     * 
     * @param searchTerm Terme de recherche
     * @param pageable Pagination
     * @return Page d'utilisateurs
     */
    public Page<UserResponse> searchUsers(String searchTerm, Pageable pageable) {
        log.info("Recherche d'utilisateurs: {}", searchTerm);
        
        Page<User> users;
        
        if (searchTerm == null || searchTerm.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            // Rechercher par email ou username
            users = userRepository.findAll(pageable).stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                u.getUsername().toLowerCase().contains(searchTerm.toLowerCase()))
                    .collect(java.util.stream.Collectors.toList())
                    .stream()
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(
                            list, pageable, userRepository.count()
                        )
                    ));
        }
        
        return users.map(userMapper::toUserResponse);
    }
    
    /**
     * Active ou désactive un utilisateur (admin).
     * 
     * @param userId ID de l'utilisateur
     * @param active true pour activer, false pour désactiver
     * @param adminId ID de l'admin qui effectue l'action
     * @param ipAddress Adresse IP
     */
    @Transactional
    public void setUserActive(UUID userId, boolean active, UUID adminId, String ipAddress) {
        log.info("Changement statut utilisateur {}: active={}", userId, active);
        
        User user = getUserById(userId);
        User admin = getUserById(adminId);
        
        user.setActive(active);
        userRepository.save(user);
        
        // Log d'audit
        auditLogRepository.save(
            AuditLog.builder()
                .user(admin)
                .action(active ? AuditAction.USER_ACCOUNT_UNLOCK : AuditAction.USER_ACCOUNT_LOCK)
                .entityType("User")
                .entityId(userId)
                .description((active ? "Activation" : "Désactivation") + " du compte")
                .ipAddress(ipAddress)
                .result("SUCCESS")
                .severity("WARNING")
                .build()
        );
        
        log.info("Statut utilisateur modifié avec succès");
    }
    
    /**
     * Vérifie si un email existe déjà.
     * 
     * @param email Email à vérifier
     * @return true si existe, false sinon
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Vérifie si un username existe déjà.
     * 
     * @param username Username à vérifier
     * @return true si existe, false sinon
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Compte le nombre total d'utilisateurs.
     * 
     * @return Nombre d'utilisateurs
     */
    public long countUsers() {
        return userRepository.count();
    }
    
    /**
     * Compte le nombre d'utilisateurs actifs.
     * 
     * @return Nombre d'utilisateurs actifs
     */
    public long countActiveUsers() {
        return userRepository.findAll().stream()
                .filter(User::getActive)
                .count();
    }
}

/**
 * DTO pour les statistiques utilisateur.
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class UserStatisticsResponse {
    private UUID userId;
    private Long accountAge; // jours
    private String subscriptionPlan;
    private Integer tokenBalance;
    private Integer totalTokensEarned;
    private Integer totalTokensSpent;
    private Long loginCount;
    private LocalDateTime lastLogin;
    private LocalDateTime accountCreated;
    private Boolean emailVerified;
}
