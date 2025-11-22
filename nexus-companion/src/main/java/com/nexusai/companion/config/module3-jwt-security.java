// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/security/JwtTokenProvider.java
// Description: Générateur et validateur de tokens JWT
// ============================================================================

package com.nexusai.companion.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Fournisseur de tokens JWT pour l'authentification.
 * Génère, valide et parse les tokens JWT.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}") // 24h par défaut
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}") // 7 jours par défaut
    private long refreshExpiration;
    
    /**
     * Génère un token JWT d'accès.
     * 
     * @param userId ID de l'utilisateur
     * @return Token JWT
     */
    public String generateToken(String userId) {
        return generateToken(userId, jwtExpiration);
    }
    
    /**
     * Génère un refresh token JWT.
     * 
     * @param userId ID de l'utilisateur
     * @return Refresh token JWT
     */
    public String generateRefreshToken(String userId) {
        return generateToken(userId, refreshExpiration);
    }
    
    /**
     * Génère un token JWT avec durée personnalisée.
     * 
     * @param userId ID de l'utilisateur
     * @param expiration Durée de validité en millisecondes
     * @return Token JWT
     */
    private String generateToken(String userId, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", expiration == jwtExpiration ? "access" : "refresh");
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    /**
     * Extrait l'ID utilisateur depuis le token.
     * 
     * @param token Token JWT
     * @return ID de l'utilisateur
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    /**
     * Valide un token JWT.
     * 
     * @param token Token JWT à valider
     * @return true si le token est valide
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token JWT invalide: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse et valide un token JWT.
     * 
     * @param token Token JWT
     * @return Claims du token
     * @throws JwtException Si le token est invalide
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * Obtient la clé de signature.
     * 
     * @return Clé de signature
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Vérifie si le token est expiré.
     * 
     * @param token Token JWT
     * @return true si le token est expiré
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/security/JwtAuthenticationFilter.java
// Description: Filtre Spring Security pour valider les JWT
// ============================================================================

package com.nexusai.companion.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtre d'authentification JWT.
 * Extrait et valide le token JWT de chaque requête.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extraire le token JWT de la requête
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Extraire l'ID utilisateur du token
                String userId = tokenProvider.getUserIdFromToken(jwt);
                
                // Créer l'authentification Spring Security
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.emptyList()
                    );
                
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // Configurer le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Authentification réussie pour l'utilisateur: {}", userId);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'authentification JWT: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extrait le token JWT du header Authorization.
     * 
     * @param request Requête HTTP
     * @return Token JWT ou null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/security/JwtAuthenticationEntryPoint.java
// Description: Gestion des erreurs d'authentification
// ============================================================================

package com.nexusai.companion.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Point d'entrée pour gérer les erreurs d'authentification.
 * Retourne une réponse JSON 401 en cas d'échec d'authentification.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        log.error("Erreur d'authentification: {}", authException.getMessage());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", Instant.now().toString());
        errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", "Authentification requise");
        errorDetails.put("path", request.getRequestURI());
        
        objectMapper.writeValue(response.getOutputStream(), errorDetails);
    }
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/security/SecurityConfig.java
// Description: Configuration Spring Security avec JWT (VERSION COMPLÈTE)
// ============================================================================

package com.nexusai.companion.config;

import com.nexusai.companion.security.JwtAuthenticationEntryPoint;
import com.nexusai.companion.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration complète de Spring Security avec JWT.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    /**
     * Configure la chaîne de filtres de sécurité.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> 
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                
                // Galerie publique
                .requestMatchers("/api/v1/companions/public/**").permitAll()
                .requestMatchers("/api/v1/companions/templates/**").permitAll()
                
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            // Ajouter le filtre JWT avant le filtre d'authentification standard
            .addFilterBefore(
                jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    /**
     * Configuration CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:4200",
            "https://nexusai.com"
        ));
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

// ============================================================================
// FICHIER: src/main/java/com/nexusai/companion/security/CurrentUser.java
// Description: Annotation pour injecter l'utilisateur courant
// ============================================================================

package com.nexusai.companion.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Annotation pour injecter l'ID de l'utilisateur authentifié.
 * 
 * Usage:
 * <pre>
 * public ResponseEntity<?> myEndpoint(@CurrentUser String userId) {
 *     // userId est automatiquement extrait du JWT
 * }
 * </pre>
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}

// ============================================================================
// FICHIER: src/main/resources/application.yml (ajouts JWT)
// Description: Configuration JWT
// ============================================================================

/**
# Ajouter à application.yml:

jwt:
  secret: ${JWT_SECRET:NexusAI-Super-Secret-Key-Change-This-In-Production-Min-512-Bits}
  expiration: 86400000    # 24 heures
  refresh-expiration: 604800000  # 7 jours
*/

// ============================================================================
// FICHIER: src/test/java/com/nexusai/companion/security/JwtTokenProviderTest.java
// Description: Tests unitaires pour JwtTokenProvider
// ============================================================================

package com.nexusai.companion.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour JwtTokenProvider.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@DisplayName("Tests du JwtTokenProvider")
class JwtTokenProviderTest {
    
    private JwtTokenProvider tokenProvider;
    
    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        
        // Configurer les propriétés via reflection
        ReflectionTestUtils.setField(
            tokenProvider, 
            "jwtSecret", 
            "test-secret-key-must-be-at-least-512-bits-for-HS512-algorithm-security"
        );
        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpiration", 86400000L);
    }
    
    @Test
    @DisplayName("Génération de token JWT")
    void testGenerateToken() {
        String userId = "user-123";
        String token = tokenProvider.generateToken(userId);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // Format JWT: header.payload.signature
    }
    
    @Test
    @DisplayName("Extraction de l'ID utilisateur depuis le token")
    void testGetUserIdFromToken() {
        String userId = "user-456";
        String token = tokenProvider.generateToken(userId);
        
        String extractedUserId = tokenProvider.getUserIdFromToken(token);
        
        assertEquals(userId, extractedUserId);
    }
    
    @Test
    @DisplayName("Validation d'un token valide")
    void testValidateValidToken() {
        String token = tokenProvider.generateToken("user-789");
        
        boolean isValid = tokenProvider.validateToken(token);
        
        assertTrue(isValid);
    }
    
    @Test
    @DisplayName("Validation d'un token invalide")
    void testValidateInvalidToken() {
        String invalidToken = "invalid.jwt.token";
        
        boolean isValid = tokenProvider.validateToken(invalidToken);
        
        assertFalse(isValid);
    }
    
    @Test
    @DisplayName("Vérification d'expiration du token")
    void testIsTokenExpired() {
        String token = tokenProvider.generateToken("user-999");
        
        boolean isExpired = tokenProvider.isTokenExpired(token);
        
        assertFalse(isExpired); // Le token vient d'être généré
    }
    
    @Test
    @DisplayName("Génération de refresh token")
    void testGenerateRefreshToken() {
        String userId = "user-refresh";
        String refreshToken = tokenProvider.generateRefreshToken(userId);
        
        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateToken(refreshToken));
        
        String extractedUserId = tokenProvider.getUserIdFromToken(refreshToken);
        assertEquals(userId, extractedUserId);
    }
}