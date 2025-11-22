// ============================================================================
// FICHIER: pom.xml (ajout dépendances Feign)
// Description: Ajouter ces dépendances au pom.xml existant
// ============================================================================

/**
<!-- Spring Cloud OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Resilience4j pour Circuit Breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Dans properties -->
<spring-cloud.version>2023.0.0</spring-cloud.version>

<!-- Dans dependencyManagement -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
*/

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/UserServiceClient.java
// Description: Client Feign pour le Module 1 (User Management)
// ============================================================================

package com.nexusai.companion.client;

import com.nexusai.companion.client.dto.UserResponse;
import com.nexusai.companion.client.dto.SubscriptionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client Feign pour communiquer avec le User Management Service.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@FeignClient(
    name = "user-service",
    url = "${services.user-service.url}",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    
    /**
     * Récupère les informations d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Informations utilisateur
     */
    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUser(@PathVariable("userId") String userId);
    
    /**
     * Récupère l'abonnement actuel d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Informations d'abonnement
     */
    @GetMapping("/api/v1/users/{userId}/subscription")
    SubscriptionResponse getUserSubscription(@PathVariable("userId") String userId);
    
    /**
     * Vérifie si un utilisateur existe.
     * 
     * @param userId ID de l'utilisateur
     * @return true si l'utilisateur existe
     */
    @GetMapping("/api/v1/users/{userId}/exists")
    Boolean userExists(@PathVariable("userId") String userId);
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/UserServiceClientFallback.java
// Description: Fallback pour UserServiceClient (Circuit Breaker)
// ============================================================================

package com.nexusai.companion.client;

import com.nexusai.companion.client.dto.SubscriptionResponse;
import com.nexusai.companion.client.dto.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback pour UserServiceClient en cas d'indisponibilité du service.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Component
@Slf4j
public class UserServiceClientFallback implements UserServiceClient {
    
    @Override
    public UserResponse getUser(String userId) {
        log.warn("Fallback: User Service indisponible pour userId: {}", userId);
        return UserResponse.builder()
            .id(userId)
            .email("unavailable@nexusai.com")
            .username("User " + userId)
            .build();
    }
    
    @Override
    public SubscriptionResponse getUserSubscription(String userId) {
        log.warn("Fallback: User Service indisponible pour subscription userId: {}", userId);
        return SubscriptionResponse.builder()
            .userId(userId)
            .plan("FREE")
            .active(true)
            .build();
    }
    
    @Override
    public Boolean userExists(String userId) {
        log.warn("Fallback: User Service indisponible pour userExists userId: {}", userId);
        return true; // Par défaut, on assume que l'utilisateur existe
    }
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/PaymentServiceClient.java
// Description: Client Feign pour le Module 2 (Payment & Subscription)
// ============================================================================

package com.nexusai.companion.client;

import com.nexusai.companion.client.dto.ConsumeTokensRequest;
import com.nexusai.companion.client.dto.SubscriptionPlanResponse;
import com.nexusai.companion.client.dto.TokenBalanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Client Feign pour communiquer avec le Payment Service.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@FeignClient(
    name = "payment-service",
    url = "${services.payment-service.url}",
    fallback = PaymentServiceClientFallback.class
)
public interface PaymentServiceClient {
    
    /**
     * Récupère le plan d'abonnement actuel d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Plan d'abonnement
     */
    @GetMapping("/api/v1/subscriptions/current")
    SubscriptionPlanResponse getCurrentPlan(@RequestParam("userId") String userId);
    
    /**
     * Consomme des jetons pour une action.
     * 
     * @param request Requête de consommation
     */
    @PostMapping("/api/v1/tokens/consume")
    void consumeTokens(@RequestBody ConsumeTokensRequest request);
    
    /**
     * Récupère le solde de jetons d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Solde de jetons
     */
    @GetMapping("/api/v1/tokens/balance")
    TokenBalanceResponse getTokenBalance(@RequestParam("userId") String userId);
    
    /**
     * Vérifie si l'utilisateur a suffisamment de jetons.
     * 
     * @param userId ID de l'utilisateur
     * @param amount Nombre de jetons requis
     * @return true si l'utilisateur a suffisamment de jetons
     */
    @GetMapping("/api/v1/tokens/has-sufficient")
    Boolean hasSufficientTokens(
        @RequestParam("userId") String userId,
        @RequestParam("amount") int amount
    );
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/PaymentServiceClientFallback.java
// Description: Fallback pour PaymentServiceClient
// ============================================================================

package com.nexusai.companion.client;

import com.nexusai.companion.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback pour PaymentServiceClient.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Component
@Slf4j
public class PaymentServiceClientFallback implements PaymentServiceClient {
    
    @Override
    public SubscriptionPlanResponse getCurrentPlan(String userId) {
        log.warn("Fallback: Payment Service indisponible pour plan userId: {}", userId);
        return SubscriptionPlanResponse.builder()
            .userId(userId)
            .plan("FREE")
            .companionLimit(1)
            .build();
    }
    
    @Override
    public void consumeTokens(ConsumeTokensRequest request) {
        log.warn("Fallback: Impossible de consommer des jetons pour userId: {}", 
                 request.getUserId());
    }
    
    @Override
    public TokenBalanceResponse getTokenBalance(String userId) {
        log.warn("Fallback: Payment Service indisponible pour balance userId: {}", userId);
        return TokenBalanceResponse.builder()
            .userId(userId)
            .balance(0)
            .build();
    }
    
    @Override
    public Boolean hasSufficientTokens(String userId, int amount) {
        log.warn("Fallback: Payment Service indisponible pour hasSufficientTokens userId: {}", 
                 userId);
        return false; // Par défaut, on assume qu'il n'a pas assez de jetons
    }
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/dto/UserResponse.java
// Description: DTO pour les réponses du User Service
// ============================================================================

package com.nexusai.companion.client.dto;

import lombok.*;

/**
 * DTO de réponse pour les informations utilisateur.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String email;
    private String username;
    private String role;
    private boolean active;
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/dto/SubscriptionResponse.java
// Description: DTO pour les informations d'abonnement
// ============================================================================

package com.nexusai.companion.client.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO de réponse pour les informations d'abonnement.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private String userId;
    private String plan;
    private boolean active;
    private Instant startDate;
    private Instant endDate;
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/dto/SubscriptionPlanResponse.java
// Description: DTO pour le plan d'abonnement
// ============================================================================

package com.nexusai.companion.client.dto;

import lombok.*;

/**
 * DTO de réponse pour le plan d'abonnement.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanResponse {
    private String userId;
    private String plan;
    private int companionLimit;
    private boolean canEvolve;
    private boolean canMerge;
    private boolean canAccessVR;
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/dto/TokenBalanceResponse.java
// Description: DTO pour le solde de jetons
// ============================================================================

package com.nexusai.companion.client.dto;

import lombok.*;

/**
 * DTO de réponse pour le solde de jetons.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBalanceResponse {
    private String userId;
    private int balance;
    private int totalEarned;
    private int totalSpent;
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/client/dto/ConsumeTokensRequest.java
// Description: DTO pour consommer des jetons
// ============================================================================

package com.nexusai.companion.client.dto;

import lombok.*;

/**
 * DTO de requête pour consommer des jetons.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeTokensRequest {
    private String userId;
    private int amount;
    private String action;
    private String description;
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/config/FeignConfig.java
// Description: Configuration Feign
// ============================================================================

package com.nexusai.companion.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Configuration Feign pour les appels inter-services.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Configuration
public class FeignConfig {
    
    /**
     * Active les logs Feign en mode FULL pour le debug.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    /**
     * Intercepteur pour propager les headers d'authentification.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Propager le userId depuis le contexte de sécurité
            Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
            
            if (authentication != null && authentication.getPrincipal() != null) {
                String userId = authentication.getPrincipal().toString();
                requestTemplate.header("X-User-Id", userId);
            }
            
            // Ajouter d'autres headers communs
            requestTemplate.header("X-Service-Name", "companion-service");
        };
    }
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/CompanionServiceApplication.java
// Description: Activer Feign Clients (ajout annotation)
// ============================================================================

package com.nexusai.companion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Application principale avec Feign activé.
 */
@SpringBootApplication
@EnableFeignClients  // AJOUTER CETTE ANNOTATION
public class CompanionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompanionServiceApplication.class, args);
    }
}

// ============================================================================
// FICHIER: src/main/resources/application.yml (ajouts Feign)
// Description: Configuration Feign
// ============================================================================

/**
# Ajouter à application.yml:

services:
  user-service:
    url: ${USER_SERVICE_URL:http://localhost:8081}
  payment-service:
    url: ${PAYMENT_SERVICE_URL:http://localhost:8082}

feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: full
  circuitbreaker:
    enabled: true

resilience4j:
  circuitbreaker:
    instances:
      user-service:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
      payment-service:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
*/

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/service/QuotaService.java
// Description: Mise à jour pour utiliser PaymentServiceClient
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.client.PaymentServiceClient;
import com.nexusai.companion.client.dto.SubscriptionPlanResponse;
import com.nexusai.companion.repository.CompanionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service de vérification des quotas (VERSION MISE À JOUR avec Feign).
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuotaService {
    
    private final CompanionRepository companionRepository;
    private final PaymentServiceClient paymentServiceClient;
    
    /**
     * Vérifie si l'utilisateur peut créer un nouveau compagnon.
     */
    public boolean canCreateCompanion(String userId) {
        long currentCount = companionRepository.countByUserId(userId);
        
        // Appeler le Payment Service pour récupérer le plan
        SubscriptionPlanResponse plan = paymentServiceClient.getCurrentPlan(userId);
        int limit = plan.getCompanionLimit();
        
        log.debug("User {} has {}/{} companions (plan: {})", 
                  userId, currentCount, limit, plan.getPlan());
        
        return currentCount < limit;
    }
    
    /**
     * Récupère le plan d'abonnement de l'utilisateur.
     */
    public SubscriptionPlanResponse getUserPlan(String userId) {
        return paymentServiceClient.getCurrentPlan(userId);
    }
}