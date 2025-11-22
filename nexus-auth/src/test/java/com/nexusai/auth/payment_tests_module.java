/**
 * ============================================================================
 * MODULE: payment-tests
 * ============================================================================
 * Tests unitaires, d'intégration et end-to-end pour le module Payment.
 * 
 * DÉVELOPPEUR ASSIGNÉ: Developer 6 (Équipe Tests & QA)
 * 
 * STRUCTURE:
 * - Tests unitaires: test/java/com/nexusai/payment/unit/
 * - Tests intégration: test/java/com/nexusai/payment/integration/
 * - Tests E2E: test/java/com/nexusai/payment/e2e/
 * ============================================================================
 */

package com.nexusai.payment.unit.domain;

import com.nexusai.payment.domain.entity.*;
import com.nexusai.payment.api.exception.InsufficientTokensException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import java.util.UUID;

/**
 * Tests unitaires pour l'entité TokenWallet.
 * 
 * <p>Teste la logique métier de gestion des jetons.</p>
 * 
 * @author Developer 6
 * @since 1.0
 */
@DisplayName("TokenWallet - Tests Unitaires")
class TokenWalletTest {
    
    private TokenWallet wallet;
    
    @BeforeEach
    void setUp() {
        wallet = TokenWallet.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .balance(100)
            .totalEarned(0)
            .totalSpent(0)
            .build();
    }
    
    /**
     * Test: Ajout de jetons met à jour le solde et totalEarned.
     */
    @Test
    @DisplayName("Ajout de jetons doit augmenter le solde et totalEarned")
    void testAddTokens_ShouldIncreaseBalanceAndTotalEarned() {
        // Given
        Integer initialBalance = wallet.getBalance();
        Integer tokensToAdd = 50;
        
        // When
        wallet.addTokens(tokensToAdd);
        
        // Then
        assertThat(wallet.getBalance())
            .isEqualTo(initialBalance + tokensToAdd);
        assertThat(wallet.getTotalEarned())
            .isEqualTo(tokensToAdd);
    }
    
    /**
     * Test: Ajout de jetons négatifs lève une exception.
     */
    @Test
    @DisplayName("Ajout de jetons négatifs doit lever IllegalArgumentException")
    void testAddTokens_WithNegativeAmount_ShouldThrowException() {
        // Given
        Integer negativeAmount = -50;
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.addTokens(negativeAmount);
        });
    }
    
    /**
     * Test: Consommation de jetons avec solde suffisant.
     */
    @Test
    @DisplayName("Consommation de jetons doit diminuer le solde")
    void testConsumeTokens_WithSufficientBalance_ShouldDecreaseBalance() {
        // Given
        Integer initialBalance = wallet.getBalance();
        Integer tokensToConsume = 30;
        
        // When
        wallet.consumeTokens(tokensToConsume);
        
        // Then
        assertThat(wallet.getBalance())
            .isEqualTo(initialBalance - tokensToConsume);
        assertThat(wallet.getTotalSpent())
            .isEqualTo(tokensToConsume);
    }
    
    /**
     * Test: Consommation de jetons avec solde insuffisant.
     */
    @Test
    @DisplayName("Consommation avec solde insuffisant doit lever InsufficientTokensException")
    void testConsumeTokens_WithInsufficientBalance_ShouldThrowException() {
        // Given
        Integer tokensToConsume = 150; // Plus que le solde
        
        // When & Then
        InsufficientTokensException exception = assertThrows(
            InsufficientTokensException.class,
            () -> wallet.consumeTokens(tokensToConsume)
        );
        
        assertThat(exception.getRequired()).isEqualTo(tokensToConsume);
        assertThat(exception.getAvailable()).isEqualTo(wallet.getBalance());
    }
    
    /**
     * Test: Vérification de solde suffisant.
     */
    @Test
    @DisplayName("hasSufficientBalance doit retourner true si solde suffisant")
    void testHasSufficientBalance_ShouldReturnTrueIfSufficient() {
        // Given
        Integer requiredAmount = 50;
        
        // When
        boolean result = wallet.hasSufficientBalance(requiredAmount);
        
        // Then
        assertThat(result).isTrue();
    }
    
    /**
     * Test: Vérification de solde insuffisant.
     */
    @Test
    @DisplayName("hasSufficientBalance doit retourner false si solde insuffisant")
    void testHasSufficientBalance_ShouldReturnFalseIfInsufficient() {
        // Given
        Integer requiredAmount = 150;
        
        // When
        boolean result = wallet.hasSufficientBalance(requiredAmount);
        
        // Then
        assertThat(result).isFalse();
    }
}

/**
 * Tests unitaires pour l'entité Subscription.
 * 
 * @author Developer 6
 * @since 1.0
 */
@DisplayName("Subscription - Tests Unitaires")
class SubscriptionTest {
    
    private Subscription subscription;
    
    @BeforeEach
    void setUp() {
        subscription = Subscription.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .plan(SubscriptionPlan.STANDARD)
            .status(SubscriptionStatus.ACTIVE)
            .monthlyPrice(SubscriptionPlan.STANDARD.getMonthlyPrice())
            .autoRenewal(true)
            .startDate(Instant.now())
            .build();
    }
    
    /**
     * Test: Annulation immédiate met fin à l'abonnement maintenant.
     */
    @Test
    @DisplayName("Annulation immédiate doit mettre endDate à maintenant")
    void testCancel_Immediately_ShouldSetEndDateToNow() {
        // When
        subscription.cancel(true);
        
        // Then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(subscription.getEndDate()).isNotNull();
        assertThat(subscription.getAutoRenewal()).isFalse();
    }
    
    /**
     * Test: Annulation différée garde l'abonnement actif.
     */
    @Test
    @DisplayName("Annulation différée doit garder le statut CANCELED sans endDate")
    void testCancel_Deferred_ShouldSetStatusCanceledButKeepActive() {
        // When
        subscription.cancel(false);
        
        // Then
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(subscription.getAutoRenewal()).isFalse();
    }
    
    /**
     * Test: Upgrade vers un plan supérieur.
     */
    @Test
    @DisplayName("Upgrade doit changer le plan et le prix")
    void testUpgradeTo_ShouldChangePlanAndPrice() {
        // Given
        SubscriptionPlan newPlan = SubscriptionPlan.PREMIUM;
        
        // When
        subscription.upgradeTo(newPlan);
        
        // Then
        assertThat(subscription.getPlan()).isEqualTo(newPlan);
        assertThat(subscription.getMonthlyPrice()).isEqualTo(newPlan.getMonthlyPrice());
    }
    
    /**
     * Test: Upgrade vers un plan inférieur lève une exception.
     */
    @Test
    @DisplayName("Upgrade vers plan inférieur doit lever IllegalArgumentException")
    void testUpgradeTo_LowerPlan_ShouldThrowException() {
        // Given
        SubscriptionPlan lowerPlan = SubscriptionPlan.FREE;
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            subscription.upgradeTo(lowerPlan);
        });
    }
    
    /**
     * Test: isActive retourne true si abonnement actif.
     */
    @Test
    @DisplayName("isActive doit retourner true si statut ACTIVE et pas expiré")
    void testIsActive_ShouldReturnTrueIfActive() {
        // When
        boolean result = subscription.isActive();
        
        // Then
        assertThat(result).isTrue();
    }
    
    /**
     * Test: isActive retourne false si abonnement annulé.
     */
    @Test
    @DisplayName("isActive doit retourner false si annulé")
    void testIsActive_ShouldReturnFalseIfCanceled() {
        // Given
        subscription.cancel(true);
        
        // When
        boolean result = subscription.isActive();
        
        // Then
        assertThat(result).isFalse();
    }
}

// ============================================================================
// TESTS D'INTÉGRATION
// ============================================================================

package com.nexusai.payment.integration.service;

import com.nexusai.payment.application.service.SubscriptionService;
import com.nexusai.payment.application.service.TokenService;
import com.nexusai.payment.api.dto.*;
import com.nexusai.payment.api.request.*;
import com.nexusai.payment.infrastructure.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.UUID;

/**
 * Tests d'intégration pour SubscriptionService.
 * 
 * <p>Teste l'intégration entre les services, repositories et la base de données.</p>
 * 
 * @author Developer 6
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("SubscriptionService - Tests Intégration")
class SubscriptionServiceIntegrationTest {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @MockBean
    private StripeService stripeService;
    
    /**
     * Test: Création d'un abonnement persiste en base de données.
     */
    @Test
    @DisplayName("Création abonnement doit persister en base")
    void testCreateSubscription_ShouldPersistInDatabase() {
        // Given
        UUID userId = UUID.randomUUID();
        
        // Mock Stripe
        when(stripeService.createSubscription(anyString(), any(), anyString()))
            .thenReturn("sub_stripe_123");
        
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
            .userId(userId)
            .plan(SubscriptionPlan.PREMIUM)
            .paymentMethodId("pm_test")
            .build();
        
        // When
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        
        // Then
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getSubscription()).isNotNull();
        
        // Vérifier en base
        Optional<Subscription> saved = subscriptionRepository
            .findActiveByUserId(userId);
        
        assertThat(saved).isPresent();
        assertThat(saved.get().getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
    }
    
    /**
     * Test: Création d'un second abonnement actif échoue.
     */
    @Test
    @DisplayName("Création second abonnement actif doit échouer")
    void testCreateSubscription_WithExistingActive_ShouldFail() {
        // Given
        UUID userId = UUID.randomUUID();
        
        // Créer premier abonnement
        when(stripeService.createSubscription(anyString(), any(), anyString()))
            .thenReturn("sub_stripe_123");
        
        CreateSubscriptionRequest request1 = CreateSubscriptionRequest.builder()
            .userId(userId)
            .plan(SubscriptionPlan.STANDARD)
            .paymentMethodId("pm_test")
            .build();
        
        subscriptionService.createSubscription(request1);
        
        // When & Then
        CreateSubscriptionRequest request2 = CreateSubscriptionRequest.builder()
            .userId(userId)
            .plan(SubscriptionPlan.PREMIUM)
            .paymentMethodId("pm_test")
            .build();
        
        assertThatThrownBy(() -> subscriptionService.createSubscription(request2))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("déjà un abonnement actif");
    }
}

/**
 * Tests d'intégration pour TokenService.
 * 
 * @author Developer 6
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("TokenService - Tests Intégration")
class TokenServiceIntegrationTest {
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private TokenWalletRepository walletRepository;
    
    @Autowired
    private TokenTransactionRepository transactionRepository;
    
    @MockBean
    private StripeService stripeService;
    
    /**
     * Test: Achat de jetons crée wallet et transaction.
     */
    @Test
    @DisplayName("Achat jetons doit créer wallet et transaction")
    void testPurchaseTokens_ShouldCreateWalletAndTransaction() {
        // Given
        UUID userId = UUID.randomUUID();
        
        when(stripeService.createPayment(anyString(), any(), anyString()))
            .thenReturn("pi_test_123");
        
        PurchaseTokensRequest request = PurchaseTokensRequest.builder()
            .userId(userId)
            .tokenAmount(100)
            .paymentMethodId("pm_test")
            .build();
        
        // When
        TokenTransactionDTO transaction = tokenService.purchaseTokens(request);
        
        // Then
        assertThat(transaction).isNotNull();
        
        // Vérifier wallet créé
        Optional<TokenWallet> wallet = walletRepository.findByUserId(userId);
        assertThat(wallet).isPresent();
        assertThat(wallet.get().getBalance()).isEqualTo(100);
        
        // Vérifier transaction enregistrée
        List<TokenTransaction> transactions = 
            transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.get().getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAmount()).isEqualTo(100);
    }
    
    /**
     * Test: Consommation avec solde insuffisant lève exception.
     */
    @Test
    @DisplayName("Consommation avec solde insuffisant doit lever exception")
    void testConsumeTokens_InsufficientBalance_ShouldThrowException() {
        // Given
        UUID userId = UUID.randomUUID();
        
        // Créer wallet avec 10 jetons
        TokenWallet wallet = TokenWallet.builder()
            .userId(userId)
            .balance(10)
            .build();
        walletRepository.save(wallet);
        
        // Tenter de consommer 50 jetons
        ConsumeTokensRequest request = ConsumeTokensRequest.builder()
            .userId(userId)
            .amount(50)
            .type(TokenTransactionType.SPENT_IMAGE)
            .description("Test")
            .build();
        
        // When & Then
        assertThatThrownBy(() -> tokenService.consumeTokens(request))
            .isInstanceOf(InsufficientTokensException.class);
    }
}

// ============================================================================
// TESTS END-TO-END
// ============================================================================

package com.nexusai.payment.e2e;

import com.nexusai.payment.api.request.*;
import com.nexusai.payment.api.response.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import java.util.UUID;

/**
 * Tests End-to-End pour les APIs Payment.
 * 
 * <p>Teste le flux complet depuis l'API REST jusqu'à la base de données.</p>
 * 
 * @author Developer 6
 * @since 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Payment API - Tests E2E")
class PaymentAPIE2ETest {
    
    @LocalServerPort
    private int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }
    
    /**
     * Test E2E: Flux complet de création d'abonnement.
     * 
     * <p><b>Scénario:</b></p>
     * <ol>
     *   <li>Créer un abonnement PREMIUM</li>
     *   <li>Vérifier que l'abonnement est actif</li>
     *   <li>Changer pour VIP+</li>
     *   <li>Annuler l'abonnement</li>
     * </ol>
     */
    @Test
    @DisplayName("E2E: Flux complet création → upgrade → annulation")
    void testCompleteSubscriptionFlow() {
        UUID userId = UUID.randomUUID();
        
        // 1. Créer abonnement PREMIUM
        CreateSubscriptionRequest createRequest = CreateSubscriptionRequest.builder()
            .userId(userId)
            .plan(SubscriptionPlan.PREMIUM)
            .paymentMethodId("pm_card_visa_test")
            .build();
        
        String subscriptionId = 
            given()
                .contentType(ContentType.JSON)
                .body(createRequest)
            .when()
                .post("/subscriptions/subscribe")
            .then()
                .statusCode(200)
                .body("success", is(true))
                .body("subscription.plan", is("PREMIUM"))
                .extract()
                .path("subscription.id");
        
        // 2. Vérifier abonnement actif
        given()
            .queryParam("userId", userId)
        .when()
            .get("/subscriptions/current")
        .then()
            .statusCode(200)
            .body("plan", is("PREMIUM"))
            .body("status", is("ACTIVE"));
        
        // 3. Upgrade vers VIP+
        given()
            .contentType(ContentType.JSON)
            .queryParam("newPlan", "VIP_PLUS")
        .when()
            .put("/subscriptions/" + subscriptionId + "/plan")
        .then()
            .statusCode(200)
            .body("plan", is("VIP_PLUS"));
        
        // 4. Annuler abonnement
        CancelSubscriptionRequest cancelRequest = CancelSubscriptionRequest.builder()
            .subscriptionId(UUID.fromString(subscriptionId))
            .immediately(false)
            .build();
        
        given()
            .contentType(ContentType.JSON)
            .body(cancelRequest)
        .when()
            .post("/subscriptions/cancel")
        .then()
            .statusCode(200)
            .body("status", is("CANCELED"));
    }
    
    /**
     * Test E2E: Flux complet de gestion de jetons.
     * 
     * <p><b>Scénario:</b></p>
     * <ol>
     *   <li>Acheter 500 jetons</li>
     *   <li>Vérifier le solde</li>
     *   <li>Consommer 100 jetons</li>
     *   <li>Vérifier le nouveau solde</li>
     * </ol>
     */
    @Test
    @DisplayName("E2E: Flux complet achat → consommation de jetons")
    void testCompleteTokenFlow() {
        UUID userId = UUID.randomUUID();
        
        // 1. Acheter 500 jetons
        PurchaseTokensRequest purchaseRequest = PurchaseTokensRequest.builder()
            .userId(userId)
            .tokenAmount(500)
            .paymentMethodId("pm_card_visa_test")
            .build();
        
        given()
            .contentType(ContentType.JSON)
            .body(purchaseRequest)
        .when()
            .post("/tokens/purchase")
        .then()
            .statusCode(200)
            .body("amount", is(500));
        
        // 2. Vérifier solde
        given()
            .queryParam("userId", userId)
        .when()
            .get("/tokens/balance")
        .then()
            .statusCode(200)
            .body("balance", is(500));
        
        // 3. Consommer 100 jetons
        ConsumeTokensRequest consumeRequest = ConsumeTokensRequest.builder()
            .userId(userId)
            .amount(100)
            .type(TokenTransactionType.SPENT_IMAGE)
            .description("Génération image")
            .build();
        
        given()
            .contentType(ContentType.JSON)
            .body(consumeRequest)
        .when()
            .post("/tokens/consume")
        .then()
            .statusCode(200);
        
        // 4. Vérifier nouveau solde
        given()
            .queryParam("userId", userId)
        .when()
            .get("/tokens/balance")
        .then()
            .statusCode(200)
            .body("balance", is(400));
    }
}

// ============================================================================
// CONFIGURATION TESTS
// ============================================================================

// application-test.yml
/**
 * Configuration pour les tests.
 * Utilise une base H2 en mémoire et un Kafka embarqué.
 */
/*
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop
  
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}

stripe:
  api:
    key: sk_test_fake_key
  webhook:
    secret: whsec_test_secret

logging:
  level:
    com.nexusai.payment: DEBUG
*/