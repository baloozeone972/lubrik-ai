package com.nexusai.auth.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration Redis pour le caching et le stockage de sessions.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableCaching
public class RedisConfig {
    
    /**
     * Template Redis générique.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Utiliser StringRedisSerializer pour les clés
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Utiliser GenericJackson2JsonRedisSerializer pour les valeurs
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Configuration du cache manager Redis.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // TTL par défaut : 1 heure
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()
                    )
                )
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()
                    )
                )
                .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("users", 
                    config.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("tokens", 
                    config.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("subscriptions", 
                    config.entryTtl(Duration.ofHours(2)))
                .build();
    }
}

// ══════════════════════════════════════════════════════════════════════

package com.nexusai.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Filtre de rate limiting basé sur Redis.
 * 
 * Limite le nombre de requêtes par IP pour éviter les abus.
 * 
 * Configuration par défaut :
 * - 100 requêtes par minute par IP
 * - 1000 requêtes par heure par IP
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Limites par défaut
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int MAX_REQUESTS_PER_HOUR = 1000;
    
    // Endpoints exclus du rate limiting
    private static final String[] EXCLUDED_PATHS = {
        "/actuator/health",
        "/swagger-ui",
        "/v3/api-docs"
    };
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Exclure certains endpoints
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = getClientIp(request);
        
        // Vérifier les limites
        if (isRateLimitExceeded(clientIp)) {
            log.warn("Rate limit dépassé pour l'IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Trop de requêtes. Veuillez réessayer plus tard.\"}"
            );
            return;
        }
        
        // Incrémenter les compteurs
        incrementRequestCount(clientIp);
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Vérifie si la limite de requêtes est dépassée.
     */
    private boolean isRateLimitExceeded(String clientIp) {
        // Vérifier limite par minute
        String minuteKey = "rate_limit:minute:" + clientIp;
        Long minuteCount = getRequestCount(minuteKey);
        
        if (minuteCount != null && minuteCount >= MAX_REQUESTS_PER_MINUTE) {
            return true;
        }
        
        // Vérifier limite par heure
        String hourKey = "rate_limit:hour:" + clientIp;
        Long hourCount = getRequestCount(hourKey);
        
        return hourCount != null && hourCount >= MAX_REQUESTS_PER_HOUR;
    }
    
    /**
     * Récupère le nombre de requêtes.
     */
    private Long getRequestCount(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : null;
    }
    
    /**
     * Incrémente les compteurs de requêtes.
     */
    private void incrementRequestCount(String clientIp) {
        String minuteKey = "rate_limit:minute:" + clientIp;
        String hourKey = "rate_limit:hour:" + clientIp;
        
        // Incrémenter et définir l'expiration si nécessaire
        redisTemplate.opsForValue().increment(minuteKey);
        redisTemplate.expire(minuteKey, Duration.ofMinutes(1));
        
        redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, Duration.ofHours(1));
    }
    
    /**
     * Récupère l'adresse IP réelle du client.
     */
    private String getClientIp(HttpServletRequest request) {
        // Vérifier les headers de proxy
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Prendre la première IP si plusieurs sont présentes
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }
    
    /**
     * Vérifie si le chemin est exclu du rate limiting.
     */
    private boolean isExcluded(String path) {
        for (String excludedPath : EXCLUDED_PATHS) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }
}

// ══════════════════════════════════════════════════════════════════════

package com.nexusai.auth.config;

import com.nexusai.auth.filter.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du filtre de rate limiting.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Configuration
@RequiredArgsConstructor
public class FilterConfig {
    
    private final RateLimitingFilter rateLimitingFilter;
    
    /**
     * Enregistre le filtre de rate limiting.
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        registrationBean.setFilter(rateLimitingFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1); // Exécuté en premier
        
        return registrationBean;
    }
}

// ══════════════════════════════════════════════════════════════════════

/**
 * Utilisation du cache dans les services.
 * 
 * Exemple dans UserService :
 */
package com.nexusai.auth.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;

public class UserServiceWithCache {
    
    /**
     * Met en cache le résultat.
     */
    @Cacheable(value = "users", key = "#userId")
    public User getUserById(UUID userId) {
        // ...
    }
    
    /**
     * Met à jour le cache.
     */
    @CachePut(value = "users", key = "#userId")
    public User updateUser(UUID userId, UpdateUserRequest request) {
        // ...
    }
    
    /**
     * Invalide le cache.
     */
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(UUID userId) {
        // ...
    }
    
    /**
     * Invalide tout le cache users.
     */
    @CacheEvict(value = "users", allEntries = true)
    public void clearUserCache() {
        // ...
    }
}
