// =============================================================================
// FICHIER 1: SecurityConfig.java
// =============================================================================
package com.nexusai.moderation.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de sécurité Spring Security avec JWT.
 * 
 * @author NexusAI Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                // APIs de modération (requiert authentification)
                .requestMatchers("/api/v1/moderation/**").authenticated()
                
                // APIs d'incidents (requiert rôle MODERATOR ou ADMIN)
                .requestMatchers("/api/v1/moderation/incidents/**")
                    .hasAnyRole("MODERATOR", "ADMIN")
                
                // APIs de consentement (requiert authentification)
                .requestMatchers("/api/v1/moderation/consents/**").authenticated()
                
                // Tout le reste requiert authentification
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> 
                oauth2.jwt(jwt -> {})
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "https://nexusai.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

// =============================================================================
// FICHIER 2: CacheConfig.java
// =============================================================================
package com.nexusai.moderation.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration du cache Redis.
 * 
 * Caches configurés :
 * - moderation-rules : 5 minutes
 * - user-moderation-levels : 1 heure
 * - kyc-status : 1 heure
 * 
 * @author NexusAI Team
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configuration par défaut
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
        
        // Configurations spécifiques par cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Règles de modération : 5 minutes
        cacheConfigurations.put("moderation-rules",
            defaultConfig.entryTtl(Duration.ofMinutes(5))
        );
        
        // Niveaux de modération utilisateur : 1 heure
        cacheConfigurations.put("user-moderation-levels",
            defaultConfig.entryTtl(Duration.ofHours(1))
        );
        
        // Statuts KYC : 1 heure
        cacheConfigurations.put("kyc-status",
            defaultConfig.entryTtl(Duration.ofHours(1))
        );
        
        // Consentements actifs : 30 minutes
        cacheConfigurations.put("active-consents",
            defaultConfig.entryTtl(Duration.ofMinutes(30))
        );
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

// =============================================================================
// FICHIER 3: OpenApiConfig.java
// =============================================================================
package com.nexusai.moderation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI (Swagger) pour documentation API.
 * 
 * @author NexusAI Team
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI nexusAIModerationAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NexusAI Moderation API")
                .description("API de modération de contenu multi-niveaux")
                .version("1.0.0")
                .contact(new Contact()
                    .name("NexusAI Team")
                    .email("api@nexusai.com")
                    .url("https://nexusai.com")
                )
                .license(new License()
                    .name("Proprietary")
                    .url("https://nexusai.com/license")
                )
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:8084")
                    .description("Development"),
                new Server()
                    .url("https://api.nexusai.com")
                    .description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token obtenu via le service d'authentification")
                )
            );
    }
}

// =============================================================================
// FICHIER 4: IncidentController.java
// =============================================================================
package com.nexusai.moderation.controller;

import com.nexusai.moderation.model.entity.ModerationIncident;
import com.nexusai.moderation.model.enums.Severity;
import com.nexusai.moderation.service.incident.IncidentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Contrôleur REST pour la gestion des incidents de modération.
 * 
 * @author NexusAI Team
 */
@RestController
@RequestMapping("/api/v1/moderation/incidents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Incidents", description = "Gestion des incidents de modération")
@SecurityRequirement(name = "bearerAuth")
public class IncidentController {
    
    private final IncidentManagementService incidentService;
    
    /**
     * Liste les incidents avec pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Liste les incidents", description = "Requiert rôle MODERATOR ou ADMIN")
    public ResponseEntity<Page<ModerationIncident>> getIncidents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(
            sort[1].equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
            sort[0]
        ));
        
        Page<ModerationIncident> incidents = incidentService.getIncidents(
            status, severity, pageable
        );
        
        return ResponseEntity.ok(incidents);
    }
    
    /**
     * Récupère un incident par ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Détails d'un incident")
    public ResponseEntity<ModerationIncident> getIncident(@PathVariable UUID id) {
        return incidentService.getIncident(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Review d'un incident.
     */
    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Reviewer un incident")
    public ResponseEntity<ModerationIncident> reviewIncident(
            @PathVariable UUID id,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID reviewerId = UUID.fromString(jwt.getSubject());
        
        ModerationIncident updated = incidentService.updateIncidentStatus(
            id,
            request.status(),
            reviewerId,
            request.notes()
        );
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Escalade d'un incident.
     */
    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Escalader un incident")
    public ResponseEntity<Map<String, Object>> escalateIncident(@PathVariable UUID id) {
        boolean escalated = incidentService.escalateIfNeeded(id);
        
        return ResponseEntity.ok(Map.of(
            "incidentId", id,
            "escalated", escalated
        ));
    }
    
    /**
     * Statistiques des incidents.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Statistiques des incidents")
    public ResponseEntity<IncidentManagementService.IncidentStatistics> getStatistics(
            @RequestParam(required = false) String since) {
        
        LocalDateTime sinceDate = since != null
            ? LocalDateTime.parse(since)
            : LocalDateTime.now().minusDays(30);
        
        var stats = incidentService.getStatistics(sinceDate);
        
        return ResponseEntity.ok(stats);
    }
    
    // DTOs
    
    public record ReviewRequest(
        String status,
        String notes
    ) {}
}

// =============================================================================
// FICHIER 5: ConsentController.java
// =============================================================================
package com.nexusai.moderation.controller;

import com.nexusai.moderation.model.entity.AdultContentConsent;
import com.nexusai.moderation.service.consent.ConsentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Contrôleur REST pour la gestion des consentements.
 * 
 * @author NexusAI Team
 */
@RestController
@RequestMapping("/api/v1/moderation/consents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consents", description = "Gestion des consentements adultes")
@SecurityRequirement(name = "bearerAuth")
public class ConsentController {
    
    private final ConsentManagementService consentService;
    
    /**
     * Crée un consentement pour contenu adulte.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Créer un consentement", 
               description = "Requiert KYC Level 3 + Plan VIP+")
    public ResponseEntity<ConsentResponse> createConsent(
            @RequestBody ConsentRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        try {
            AdultContentConsent consent = consentService.createConsent(
                userId,
                request.consentType(),
                ipAddress,
                userAgent
            );
            
            return ResponseEntity.ok(new ConsentResponse(
                consent.getId().toString(),
                consent.getConsentType(),
                consent.getSignedAt(),
                consent.getExpiresAt(),
                "Consentement créé avec succès"
            ));
            
        } catch (ConsentManagementService.InsufficientKYCException e) {
            return ResponseEntity.badRequest().body(new ConsentResponse(
                null, null, null, null,
                "KYC Level 3 requis. Veuillez compléter votre vérification d'identité."
            ));
        }
    }
    
    /**
     * Liste les consentements actifs de l'utilisateur.
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Liste les consentements actifs")
    public ResponseEntity<List<ConsentResponse>> getActiveConsents(
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        
        List<ConsentResponse> consents = consentService.getActiveConsents(userId).stream()
            .map(c -> new ConsentResponse(
                c.getId().toString(),
                c.getConsentType(),
                c.getSignedAt(),
                c.getExpiresAt(),
                null
            ))
            .toList();
        
        return ResponseEntity.ok(consents);
    }
    
    /**
     * Révoque un consentement.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Révoquer un consentement")
    public ResponseEntity<Map<String, String>> revokeConsent(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        
        consentService.revokeConsent(id, userId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Consentement révoqué avec succès",
            "consentId", id.toString()
        ));
    }
    
    /**
     * Vérifie la signature d'un consentement.
     */
    @GetMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Vérifier la signature d'un consentement")
    public ResponseEntity<Map<String, Object>> verifyConsent(@PathVariable UUID id) {
        boolean valid = consentService.verifyConsentSignature(id);
        
        return ResponseEntity.ok(Map.of(
            "consentId", id,
            "signatureValid", valid
        ));
    }
    
    // Helper methods
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
    
    // DTOs
    
    public record ConsentRequest(
        String consentType
    ) {}
    
    public record ConsentResponse(
        String consentId,
        String consentType,
        java.time.LocalDateTime signedAt,
        java.time.LocalDateTime expiresAt,
        String message
    ) {}
}

// =============================================================================
// FICHIER 6: Repository Complémentaires
// =============================================================================
package com.nexusai.moderation.repository;

import com.nexusai.moderation.model.entity.AdultContentConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface AdultContentConsentRepository extends JpaRepository<AdultContentConsent, UUID> {
    
    List<AdultContentConsent> findByUserId(UUID userId);
    
    List<AdultContentConsent> findByUserIdAndRevokedFalse(UUID userId);
    
    @Query("SELECT c FROM AdultContentConsent c " +
           "WHERE c.userId = :userId " +
           "AND c.consentType = :consentType " +
           "AND c.revoked = false " +
           "AND c.expiresAt > CURRENT_TIMESTAMP")
    Optional<AdultContentConsent> findActiveConsentByUserIdAndType(
        @Param("userId") UUID userId,
        @Param("consentType") String consentType
    );
    
    List<AdultContentConsent> findByExpiresAtBefore(LocalDateTime date);
}

// Extensions ModerationIncidentRepository
// Ajouter ces méthodes à l'interface existante:
/*
Page<ModerationIncident> findByStatus(String status, Pageable pageable);
Page<ModerationIncident> findBySeverity(Severity severity, Pageable pageable);
Page<ModerationIncident> findByStatusAndSeverity(String status, Severity severity, Pageable pageable);
long countByCreatedAtAfter(LocalDateTime date);
long countByCreatedAtAfterAndSeverity(LocalDateTime date, Severity severity);
long countByCreatedAtAfterAndStatus(LocalDateTime date, String status);
List<ModerationIncident> findByCreatedAtBeforeAndStatusNot(LocalDateTime date, String status);
*/

// Extensions UserWarningRepository
/*
List<UserWarning> findByUserIdAndExpiresAtAfter(UUID userId, LocalDateTime date);
List<UserWarning> findByExpiresAtBefore(LocalDateTime date);
*/

// =============================================================================
// FIN - Configuration & Controllers
// =============================================================================
