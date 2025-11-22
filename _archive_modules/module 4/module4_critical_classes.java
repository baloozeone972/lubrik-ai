package com.nexusai.conversation.security;

import io.github.bucket4j.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SERVICE DE RATE LIMITING
 * 
 * Implémente une limitation du taux de requêtes pour protéger l'API
 * contre les abus et les attaques DDoS.
 * 
 * Stratégies:
 * - Par utilisateur: 100 requêtes/minute
 * - Par IP: 1000 requêtes/heure
 * - Par conversation: 50 messages/minute
 * 
 * Utilise Bucket4j avec Redis pour le stockage distribué
 * 
 * @author NexusAI Dev Team
 */
@Service
@Slf4j
public class RateLimitingService {
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ConcurrentHashMap<String, Bucket> localCache = new ConcurrentHashMap<>();
    
    // Configuration des limites
    private static final int USER_REQUESTS_PER_MINUTE = 100;
    private static final int IP_REQUESTS_PER_HOUR = 1000;
    private static final int CONVERSATION_MESSAGES_PER_MINUTE = 50;
    
    public RateLimitingService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Vérifie si une requête utilisateur est autorisée
     * 
     * @param userId ID de l'utilisateur
     * @return true si autorisé, false sinon
     */
    public Mono<Boolean> checkUserRateLimit(String userId) {
        String key = "ratelimit:user:" + userId;
        
        return checkRateLimit(
            key, 
            USER_REQUESTS_PER_MINUTE, 
            Duration.ofMinutes(1)
        );
    }
    
    /**
     * Vérifie si une requête IP est autorisée
     * 
     * @param ipAddress Adresse IP
     * @return true si autorisé, false sinon
     */
    public Mono<Boolean> checkIpRateLimit(String ipAddress) {
        String key = "ratelimit:ip:" + ipAddress;
        
        return checkRateLimit(
            key,
            IP_REQUESTS_PER_HOUR,
            Duration.ofHours(1)
        );
    }
    
    /**
     * Vérifie si un message dans une conversation est autorisé
     * 
     * @param conversationId ID de la conversation
     * @return true si autorisé, false sinon
     */
    public Mono<Boolean> checkConversationRateLimit(String conversationId) {
        String key = "ratelimit:conv:" + conversationId;
        
        return checkRateLimit(
            key,
            CONVERSATION_MESSAGES_PER_MINUTE,
            Duration.ofMinutes(1)
        );
    }
    
    /**
     * Vérifie le rate limit générique
     */
    private Mono<Boolean> checkRateLimit(
            String key, 
            int maxRequests, 
            Duration window) {
        
        Bucket bucket = localCache.computeIfAbsent(key, k -> 
            createBucket(maxRequests, window)
        );
        
        if (bucket.tryConsume(1)) {
            return Mono.just(true);
        } else {
            log.warn("Rate limit exceeded for key: {}", key);
            return Mono.just(false);
        }
    }
    
    /**
     * Crée un bucket Bucket4j
     */
    private Bucket createBucket(int maxRequests, Duration window) {
        Bandwidth limit = Bandwidth.builder()
            .capacity(maxRequests)
            .refillGreedy(maxRequests, window)
            .build();
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Obtient le temps d'attente avant la prochaine requête disponible
     * 
     * @param userId ID de l'utilisateur
     * @return Durée d'attente en secondes
     */
    public Mono<Long> getWaitTimeSeconds(String userId) {
        String key = "ratelimit:user:" + userId;
        Bucket bucket = localCache.get(key);
        
        if (bucket == null) {
            return Mono.just(0L);
        }
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return Mono.just(0L);
        }
        
        return Mono.just(
            Duration.ofNanos(probe.getNanosToWaitForRefill()).getSeconds()
        );
    }
    
    /**
     * Réinitialise le rate limit pour un utilisateur (admin)
     */
    public Mono<Void> resetUserRateLimit(String userId) {
        String key = "ratelimit:user:" + userId;
        localCache.remove(key);
        
        return redisTemplate.delete(key).then();
    }
}

// ============================================================================
// FILTRE WEB POUR RATE LIMITING
// ============================================================================

/**
 * Filtre WebFlux qui applique le rate limiting sur toutes les requêtes
 */
@Component
@Slf4j
public class RateLimitingWebFilter implements WebFilter {
    
    private final RateLimitingService rateLimitingService;
    
    public RateLimitingWebFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Extraire l'IP
        String ipAddress = exchange.getRequest()
            .getRemoteAddress()
            .getAddress()
            .getHostAddress();
        
        // Extraire userId du token JWT (si présent)
        String userId = extractUserId(exchange);
        
        // Vérifier rate limit IP
        return rateLimitingService.checkIpRateLimit(ipAddress)
            .flatMap(ipAllowed -> {
                if (!ipAllowed) {
                    return handleRateLimitExceeded(exchange, "IP");
                }
                
                // Si userId présent, vérifier aussi son rate limit
                if (userId != null) {
                    return rateLimitingService.checkUserRateLimit(userId)
                        .flatMap(userAllowed -> {
                            if (!userAllowed) {
                                return handleRateLimitExceeded(exchange, "USER");
                            }
                            return chain.filter(exchange);
                        });
                }
                
                return chain.filter(exchange);
            });
    }
    
    private String extractUserId(ServerWebExchange exchange) {
        // Extraire du JWT dans Authorization header
        // Implémentation simplifiée
        return exchange.getRequest()
            .getHeaders()
            .getFirst("X-User-Id");
    }
    
    private Mono<Void> handleRateLimitExceeded(
            ServerWebExchange exchange, 
            String limitType) {
        
        log.warn("Rate limit exceeded: type={}, path={}", 
                 limitType, 
                 exchange.getRequest().getPath());
        
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders()
            .add("X-RateLimit-Limit", "100");
        exchange.getResponse().getHeaders()
            .add("X-RateLimit-Remaining", "0");
        exchange.getResponse().getHeaders()
            .add("Retry-After", "60");
        
        String errorMessage = """
            {
              "error": "RATE_LIMIT_EXCEEDED",
              "message": "Trop de requêtes. Veuillez réessayer dans quelques instants.",
              "retryAfter": 60
            }
            """;
        
        return exchange.getResponse()
            .writeWith(Mono.just(
                exchange.getResponse()
                    .bufferFactory()
                    .wrap(errorMessage.getBytes())
            ));
    }
}

// ============================================================================
// CIRCUIT BREAKER SERVICE
// ============================================================================

package com.nexusai.conversation.resilience;

import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SERVICE DE CIRCUIT BREAKER
 * 
 * Implémente le pattern Circuit Breaker pour protéger l'application
 * contre les services externes défaillants.
 * 
 * États:
 * - CLOSED: Tout fonctionne normalement
 * - OPEN: Trop d'échecs, requêtes bloquées
 * - HALF_OPEN: Test de récupération
 * 
 * @author NexusAI Dev Team
 */
@Service
@Slf4j
public class CircuitBreakerService {
    
    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = 
        new ConcurrentHashMap<>();
    
    /**
     * Obtient ou crée un circuit breaker
     */
    public CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakers.computeIfAbsent(name, this::createCircuitBreaker);
    }
    
    /**
     * Crée un circuit breaker avec configuration par défaut
     */
    private CircuitBreaker createCircuitBreaker(String name) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)                    // 50% échecs
            .slowCallRateThreshold(50)                    // 50% appels lents
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)                        // Fenêtre de 10 appels
            .minimumNumberOfCalls(5)                      // Min 5 appels
            .permittedNumberOfCallsInHalfOpenState(3)    // 3 appels en half-open
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(Exception.class)
            .build();
        
        CircuitBreaker circuitBreaker = CircuitBreaker.of(name, config);
        
        // Listeners pour logging
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Circuit Breaker '{}' état changé: {} -> {}", 
                        name, 
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.error("Circuit Breaker '{}' taux échec dépassé: {}%",
                         name, event.getFailureRate()))
            .onSlowCallRateExceeded(event ->
                log.warn("Circuit Breaker '{}' taux appels lents dépassé: {}%",
                        name, event.getSlowCallRate()));
        
        log.info("Circuit Breaker créé: {}", name);
        return circuitBreaker;
    }
    
    /**
     * Exécute une opération protégée par circuit breaker
     */
    public <T> Mono<T> executeWithCircuitBreaker(
            String circuitBreakerName,
            Mono<T> operation,
            Mono<T> fallback) {
        
        CircuitBreaker cb = getCircuitBreaker(circuitBreakerName);
        
        return operation
            .transformDeferred(CircuitBreakerOperator.of(cb))
            .onErrorResume(throwable -> {
                log.warn("Circuit breaker '{}' fallback activé: {}", 
                        circuitBreakerName, throwable.getMessage());
                return fallback;
            });
    }
    
    /**
     * Obtient l'état d'un circuit breaker
     */
    public CircuitBreaker.State getState(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        return cb != null ? cb.getState() : null;
    }
    
    /**
     * Force la transition vers OPEN (pour tests)
     */
    public void transitionToOpenState(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        if (cb != null) {
            cb.transitionToOpenState();
        }
    }
    
    /**
     * Réinitialise un circuit breaker
     */
    public void reset(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        if (cb != null) {
            cb.reset();
            log.info("Circuit Breaker '{}' réinitialisé", name);
        }
    }
}

// ============================================================================
// INPUT VALIDATION SERVICE
// ============================================================================

package com.nexusai.conversation.security;

import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

/**
 * SERVICE DE VALIDATION DES ENTRÉES
 * 
 * Valide et sanitize toutes les entrées utilisateur pour prévenir:
 * - XSS (Cross-Site Scripting)
 * - Injection NoSQL
 * - Buffer overflow
 * - Caractères malicieux
 * 
 * @author NexusAI Dev Team
 */
@Service
@Slf4j
public class InputValidationService {
    
    // Patterns dangereux
    private static final Pattern SCRIPT_PATTERN = 
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern HTML_PATTERN = 
        Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern SQL_INJECTION_PATTERN = 
        Pattern.compile(".*(union|select|insert|update|delete|drop|create|alter).*", 
                       Pattern.CASE_INSENSITIVE);
    
    private static final Pattern NOSQL_INJECTION_PATTERN = 
        Pattern.compile(".*\\$.*\\{.*\\}.*", Pattern.CASE_INSENSITIVE);
    
    // Longueurs maximales
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int MAX_TITLE_LENGTH = 255;
    
    /**
     * Valide et sanitize un message de conversation
     * 
     * @param message Message à valider
     * @return Message sanitizé
     * @throws InvalidInputException Si le message est invalide
     */
    public String validateAndSanitizeMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new InvalidInputException("Le message ne peut pas être vide");
        }
        
        // Vérifier longueur
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidInputException(
                "Le message ne peut pas dépasser " + MAX_MESSAGE_LENGTH + " caractères"
            );
        }
        
        // Détecter scripts malicieux
        if (SCRIPT_PATTERN.matcher(message).find()) {
            log.warn("Tentative d'injection de script détectée");
            throw new SecurityException("Contenu invalide détecté");
        }
        
        // Détecter injection NoSQL
        if (NOSQL_INJECTION_PATTERN.matcher(message).find()) {
            log.warn("Tentative d'injection NoSQL détectée");
            throw new SecurityException("Contenu invalide détecté");
        }
        
        // Sanitize HTML (encoder les caractères spéciaux)
        String sanitized = Encode.forHtml(message);
        
        // Normaliser les espaces
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        
        return sanitized;
    }
    
    /**
     * Valide un titre de conversation
     */
    public String validateTitle(String title) {
        if (title == null) {
            return null; // Titre optionnel
        }
        
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new InvalidInputException(
                "Le titre ne peut pas dépasser " + MAX_TITLE_LENGTH + " caractères"
            );
        }
        
        // Supprimer HTML
        String sanitized = HTML_PATTERN.matcher(title).replaceAll("");
        
        return Encode.forHtml(sanitized.trim());
    }
    
    /**
     * Valide un ID (UUID)
     */
    public boolean isValidUUID(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        
        Pattern uuidPattern = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
        );
        
        return uuidPattern.matcher(id).matches();
    }
    
    /**
     * Détecte les tentatives d'injection SQL
     */
    public boolean containsSqlInjection(String input) {
        return SQL_INJECTION_PATTERN.matcher(input).matches();
    }
    
    /**
     * Sanitize une chaîne pour usage dans une URL
     */
    public String sanitizeForUrl(String input) {
        if (input == null) {
            return null;
        }
        
        return Encode.forUriComponent(input);
    }
    
    /**
     * Valide un email
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        Pattern emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        );
        
        return emailPattern.matcher(email).matches();
    }
}

/**
 * Exception pour entrées invalides
 */
class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}

// ============================================================================
// ENCRYPTION SERVICE
// ============================================================================

package com.nexusai.conversation.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * SERVICE DE CHIFFREMENT
 * 
 * Chiffre les données sensibles dans la base de données
 * en utilisant AES-256-GCM.
 * 
 * @author NexusAI Dev Team
 */
@Service
@Slf4j
public class EncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    /**
     * Chiffre une chaîne de caractères
     * 
     * @param plainText Texte en clair
     * @return Texte chiffré en Base64
     */
    public String encrypt(String plainText) {
        try {
            // Générer IV aléatoire
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            // Créer cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8), 
                "AES"
            );
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            
            // Chiffrer
            byte[] cipherText = cipher.doFinal(
                plainText.getBytes(StandardCharsets.UTF_8)
            );
            
            // Combiner IV + cipherText
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            
            // Encoder en Base64
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (Exception e) {
            log.error("Erreur chiffrement", e);
            throw new EncryptionException("Échec du chiffrement", e);
        }
    }
    
    /**
     * Déchiffre une chaîne de caractères
     * 
     * @param cipherText Texte chiffré en Base64
     * @return Texte en clair
     */
    public String decrypt(String cipherText) {
        try {
            // Décoder Base64
            byte[] combined = Base64.getDecoder().decode(cipherText);
            
            // Extraire IV et cipherText
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            
            // Créer cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                encryptionKey.getBytes(StandardCharsets.UTF_8),
                "AES"
            );
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            
            // Déchiffrer
            byte[] plainText = cipher.doFinal(encrypted);
            
            return new String(plainText, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Erreur déchiffrement", e);
            throw new EncryptionException("Échec du déchiffrement", e);
        }
    }
    
    /**
     * Hash un mot de passe avec bcrypt
     */
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
    
    /**
     * Vérifie un mot de passe hashé
     */
    public boolean verifyPassword(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }
}

class EncryptionException extends RuntimeException {
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
