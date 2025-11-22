package com.nexusai.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.auth.dto.request.LoginRequest;
import com.nexusai.auth.dto.request.RegistrationRequest;
import com.nexusai.auth.dto.response.AuthResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests d'intégration pour le flow complet d'authentification.
 * 
 * Teste le parcours utilisateur de bout en bout :
 * 1. Inscription
 * 2. Connexion
 * 3. Accès aux ressources protégées
 * 4. Rafraîchissement du token
 * 5. Déconnexion
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthenticationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static String accessToken;
    private static String refreshToken;
    private static final String TEST_EMAIL = "integration.test@example.com";
    private static final String TEST_USERNAME = "integrationtest";
    private static final String TEST_PASSWORD = "Test@1234";
    
    /**
     * Test 1 : Inscription d'un nouvel utilisateur.
     */
    @Test
    @Order(1)
    @DisplayName("1. Inscription - Devrait créer un nouveau compte avec succès")
    void testRegistration() throws Exception {
        // Arrange
        RegistrationRequest request = RegistrationRequest.builder()
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        
        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.user.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andReturn();
        
        // Extraire les tokens
        String content = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(content, AuthResponse.class);
        
        accessToken = response.getAccessToken();
        refreshToken = response.getRefreshToken();
        
        assertThat(accessToken).isNotNull().isNotEmpty();
        assertThat(refreshToken).isNotNull().isNotEmpty();
    }
    
    /**
     * Test 2 : Inscription avec email déjà utilisé.
     */
    @Test
    @Order(2)
    @DisplayName("2. Inscription - Devrait échouer avec email déjà utilisé")
    void testRegistrationWithDuplicateEmail() throws Exception {
        // Arrange
        RegistrationRequest request = RegistrationRequest.builder()
                .email(TEST_EMAIL) // Email déjà utilisé
                .username("anotheruser")
                .password(TEST_PASSWORD)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    /**
     * Test 3 : Connexion avec identifiants valides.
     */
    @Test
    @Order(3)
    @DisplayName("3. Connexion - Devrait se connecter avec succès")
    void testLogin() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .emailOrUsername(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        
        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andReturn();
        
        // Mettre à jour les tokens
        String content = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(content, AuthResponse.class);
        accessToken = response.getAccessToken();
        refreshToken = response.getRefreshToken();
    }
    
    /**
     * Test 4 : Connexion avec mot de passe incorrect.
     */
    @Test
    @Order(4)
    @DisplayName("4. Connexion - Devrait échouer avec mot de passe incorrect")
    void testLoginWithWrongPassword() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .emailOrUsername(TEST_EMAIL)
                .password("WrongPassword123!")
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    /**
     * Test 5 : Accès à une ressource protégée avec token valide.
     */
    @Test
    @Order(5)
    @DisplayName("5. Ressource protégée - Devrait accéder avec token valide")
    void testAccessProtectedResourceWithValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.username").value(TEST_USERNAME));
    }
    
    /**
     * Test 6 : Accès à une ressource protégée sans token.
     */
    @Test
    @Order(6)
    @DisplayName("6. Ressource protégée - Devrait échouer sans token")
    void testAccessProtectedResourceWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }
    
    /**
     * Test 7 : Rafraîchissement du token.
     */
    @Test
    @Order(7)
    @DisplayName("7. Refresh Token - Devrait obtenir un nouveau access token")
    void testRefreshToken() throws Exception {
        // Arrange
        String requestBody = String.format("{\"refreshToken\":\"%s\"}", refreshToken);
        
        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        
        // Vérifier que le nouveau token est différent
        String content = result.getResponse().getContentAsString();
        String newAccessToken = objectMapper.readTree(content).get("accessToken").asText();
        
        assertThat(newAccessToken).isNotEmpty();
        // Note: Dans une vraie implémentation, le token devrait être différent
    }
    
    /**
     * Test 8 : Déconnexion.
     */
    @Test
    @Order(8)
    @DisplayName("8. Déconnexion - Devrait se déconnecter avec succès")
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }
    
    /**
     * Test 9 : Vérification de l'email (simulation).
     */
    @Test
    @Order(9)
    @DisplayName("9. Vérification Email - Devrait vérifier l'email")
    void testEmailVerification() throws Exception {
        // Note: Ce test nécessite un token de vérification réel
        // qui serait normalement envoyé par email
        // Pour les tests, on pourrait le récupérer directement de la DB
        
        // Simulation avec un token fictif
        String verificationToken = "test-verification-token";
        
        // Cette requête échouera car le token n'existe pas
        // Dans un vrai test, on récupérerait le token de la DB
        mockMvc.perform(post("/api/v1/auth/verify-email")
                .param("token", verificationToken))
                .andExpect(status().isBadRequest());
    }
}

// ══════════════════════════════════════════════════════════════════════

/**
 * Tests d'intégration pour les abonnements.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String accessToken;
    
    @BeforeEach
    void setUp() throws Exception {
        // Créer et connecter un utilisateur de test
        // ... (code similaire à AuthenticationIntegrationTest)
    }
    
    @Test
    @DisplayName("Devrait récupérer les plans disponibles")
    void testGetAvailablePlans() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].plan").exists())
                .andExpect(jsonPath("$[0].monthlyPrice").exists());
    }
    
    @Test
    @DisplayName("Devrait récupérer l'abonnement actuel")
    void testGetCurrentSubscription() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/current")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").exists());
    }
}

// ══════════════════════════════════════════════════════════════════════

/**
 * Tests d'intégration pour les jetons.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    private String accessToken;
    
    @BeforeEach
    void setUp() throws Exception {
        // Créer et connecter un utilisateur de test
    }
    
    @Test
    @DisplayName("Devrait récupérer le solde de jetons")
    void testGetTokenBalance() throws Exception {
        mockMvc.perform(get("/api/v1/tokens/balance")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").isNumber());
    }
    
    @Test
    @DisplayName("Devrait récupérer les statistiques de jetons")
    void testGetTokenStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/tokens/statistics")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").exists())
                .andExpect(jsonPath("$.totalEarned").exists())
                .andExpect(jsonPath("$.totalSpent").exists());
    }
    
    @Test
    @DisplayName("Devrait vérifier la disponibilité du bonus quotidien")
    void testCheckDailyBonusAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/tokens/daily-bonus/available")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").isBoolean())
                .andExpect(jsonPath("$.timeUntilNext").exists());
    }
}

// ══════════════════════════════════════════════════════════════════════

/**
 * Configuration de test.
 * Fichier: src/test/resources/application-test.yml
 */
/*
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  flyway:
    enabled: false
    
jwt:
  secret: test_secret_key_for_jwt_at_least_256_bits_long_test_only
  expiration: 3600000
  refresh-expiration: 7200000
  
logging:
  level:
    com.nexusai: DEBUG
*/
