# MODULE 2 : PAYMENT SYSTEM
## Guide d'Intégration pour les Autres Modules NexusAI

---

## 📋 TABLE DES MATIÈRES

1. [Vue d'Ensemble](#vue-densemble)
2. [APIs Exposées](#apis-exposées)
3. [Événements Kafka](#événements-kafka)
4. [Intégration Module par Module](#intégration-module-par-module)
5. [Exemples de Code](#exemples-de-code)
6. [Gestion des Erreurs](#gestion-des-erreurs)
7. [Best Practices](#best-practices)
8. [FAQ](#faq)

---

## 🔍 VUE D'ENSEMBLE

Le Module Payment expose deux types d'interfaces pour les autres modules :

### **1. APIs REST Synchrones**
Pour les opérations qui nécessitent une réponse immédiate :
- Vérification de quotas
- Consommation de jetons
- Vérification de statut d'abonnement

### **2. Événements Kafka Asynchrones**
Pour les notifications et synchronisations :
- Changement d'abonnement
- Achat de jetons
- Événements métier

---

## 🔌 APIS EXPOSÉES

### **Base URL**
```
Production:  https://api.nexusai.com/v1
Staging:     https://staging-api.nexusai.com/v1
Local:       http://localhost:8082/api/v1
```

### **Authentification**
Toutes les requêtes nécessitent un JWT token dans le header :
```
Authorization: Bearer <jwt_token>
```

---

## 📡 ENDPOINTS DISPONIBLES

### **1. Vérifier l'Abonnement Actif**

**Endpoint:** `GET /subscriptions/current`

**Usage:** Vérifier le plan et les quotas d'un utilisateur

**Requête:**
```bash
curl -X GET "https://api.nexusai.com/v1/subscriptions/current?userId=<uuid>" \
  -H "Authorization: Bearer <token>"
```

**Réponse:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "plan": "PREMIUM",
  "status": "ACTIVE",
  "startDate": "2025-01-01T00:00:00Z",
  "endDate": null,
  "autoRenewal": true,
  "monthlyPrice": 19.99
}
```

**Cas d'usage:**
- Module Companion: Vérifier combien de compagnons l'utilisateur peut créer
- Module Image: Vérifier si l'utilisateur peut générer des images HD
- Module Video: Vérifier si l'utilisateur peut générer des vidéos 4K

---

### **2. Vérifier le Solde de Jetons**

**Endpoint:** `GET /tokens/balance`

**Usage:** Consulter le solde de jetons avant une opération coûteuse

**Requête:**
```bash
curl -X GET "https://api.nexusai.com/v1/tokens/balance?userId=<uuid>" \
  -H "Authorization: Bearer <token>"
```

**Réponse:**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "balance": 450,
  "earnedToday": 10,
  "spentToday": 50
}
```

**Cas d'usage:**
- Afficher le solde dans l'UI
- Vérifier avant de lancer une génération
- Bloquer si solde insuffisant

---

### **3. Consommer des Jetons**

**Endpoint:** `POST /tokens/consume`

**Usage:** Débiter des jetons après une opération réussie

**Requête:**
```bash
curl -X POST "https://api.nexusai.com/v1/tokens/consume" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 50,
    "type": "SPENT_IMAGE",
    "description": "Génération image HD 1024x1024",
    "metadata": "{\"imageId\": \"img_123\", \"resolution\": \"1024x1024\"}"
  }'
```

**Réponse:**
```json
{
  "id": "tx_550e8400-e29b-41d4-a716-446655440000",
  "walletId": "wallet_123e4567-e89b-12d3-a456-426614174000",
  "type": "SPENT_IMAGE",
  "amount": -50,
  "description": "Génération image HD 1024x1024",
  "createdAt": "2025-10-18T10:30:00Z"
}
```

**Erreur si solde insuffisant:**
```json
{
  "timestamp": "2025-10-18T10:30:00Z",
  "status": 402,
  "error": "Insufficient Tokens",
  "message": "Jetons insuffisants. Requis: 50, Disponible: 10",
  "required": 50,
  "available": 10
}
```

**⚠️ IMPORTANT:** Toujours vérifier le solde AVANT de lancer l'opération coûteuse pour éviter la frustration utilisateur.

---

## 📊 COÛTS EN JETONS PAR OPÉRATION

### **Module Image Generation**
| Opération | Coût |
|-----------|------|
| Image SD 512x512 | 10 jetons |
| Image HD 1024x1024 | 30 jetons |
| Image 4K 2048x2048 | 100 jetons |

### **Module Video Generation**
| Opération | Coût |
|-----------|------|
| Vidéo courte (30s, 720p) | 100 jetons |
| Vidéo HD (2min, 1080p) | 300 jetons |
| Vidéo 4K (5min, 2160p) | 1000 jetons |

### **Module Conversation**
| Opération | Coût |
|-----------|------|
| Message texte simple | 1 jeton |
| Message avec analyse émotions | 3 jetons |
| Message vocal (STT + TTS) | 5 jetons |

---

## 🎧 ÉVÉNEMENTS KAFKA

### **Topics à Écouter**

Les autres modules peuvent s'abonner aux événements suivants :

#### **1. `payment.subscription.created`**
```json
{
  "subscriptionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "plan": "PREMIUM",
  "timestamp": "2025-10-18T10:00:00Z"
}
```

**Cas d'usage:**
- Module User: Mettre à jour le profil utilisateur
- Module Companion: Débloquer création de compagnons supplémentaires
- Module Analytics: Tracker conversions

---

#### **2. `payment.subscription.upgraded`**
```json
{
  "subscriptionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "oldPlan": "STANDARD",
  "newPlan": "PREMIUM",
  "timestamp": "2025-10-18T11:00:00Z"
}
```

**Cas d'usage:**
- Débloquer fonctionnalités premium immédiatement
- Notifier l'utilisateur
- Activer génération vidéos HD

---

#### **3. `payment.subscription.canceled`**
```json
{
  "subscriptionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "plan": "PREMIUM",
  "immediately": false,
  "timestamp": "2025-10-18T12:00:00Z"
}
```

**Cas d'usage:**
- Planifier désactivation des fonctionnalités premium
- Déclencher emails de rétention
- Logger pour analytics

---

#### **4. `payment.tokens.purchased`**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 500,
  "price": 19.99,
  "timestamp": "2025-10-18T13:00:00Z"
}
```

**Cas d'usage:**
- Notifier l'utilisateur
- Rafraîchir affichage du solde
- Tracking analytics

---

#### **5. `payment.tokens.consumed`**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "amount": 50,
  "type": "SPENT_IMAGE",
  "description": "Génération image HD",
  "timestamp": "2025-10-18T14:00:00Z"
}
```

**Cas d'usage:**
- Analytics usage
- Détecter patterns d'utilisation
- Alertes si consommation anormale

---

## 🔗 INTÉGRATION MODULE PAR MODULE

### **MODULE 3 : COMPANION MANAGEMENT**

#### **Vérifier Quotas de Compagnons**

```java
@Service
@RequiredArgsConstructor
public class CompanionQuotaService {
    
    private final PaymentClient paymentClient;
    
    public void validateCompanionCreation(UUID userId) {
        // 1. Récupérer abonnement
        SubscriptionDTO subscription = paymentClient
            .getCurrentSubscription(userId)
            .orElseThrow(() -> new ForbiddenException(
                "Abonnement requis pour créer un compagnon"
            ));
        
        // 2. Vérifier quotas selon plan
        int maxCompanions = switch (subscription.getPlan()) {
            case FREE -> 1;
            case STANDARD -> 3;
            case PREMIUM -> 10;
            case VIP_PLUS -> Integer.MAX_VALUE;
        };
        
        // 3. Compter compagnons existants
        int currentCount = companionRepository.countByUserId(userId);
        
        if (currentCount >= maxCompanions) {
            throw new QuotaExceededException(
                "Limite de compagnons atteinte (" + maxCompanions + ")"
            );
        }
    }
}
```

#### **Écouter Événements d'Abonnement**

```java
@Component
@Slf4j
public class SubscriptionEventListener {
    
    @KafkaListener(topics = "payment.subscription.upgraded")
    public void handleSubscriptionUpgraded(SubscriptionUpgradedEvent event) {
        log.info("Abonnement upgradé pour user {}: {} → {}", 
            event.getUserId(), event.getOldPlan(), event.getNewPlan());
        
        // Débloquer fonctionnalités immédiatement
        if (event.getNewPlan() == SubscriptionPlan.PREMIUM) {
            unlockPremiumFeatures(event.getUserId());
        }
    }
    
    private void unlockPremiumFeatures(UUID userId) {
        // Permettre création de plus de compagnons
        // Activer personnalisations avancées
        // etc.
    }
}
```

---

### **MODULE 5 : IMAGE GENERATION**

#### **Vérifier Jetons Avant Génération**

```java
@Service
@RequiredArgsConstructor
public class ImageGenerationService {
    
    private final PaymentClient paymentClient;
    private final ImageGenerator imageGenerator;
    
    public ImageDTO generateImage(ImageRequest request) {
        UUID userId = request.getUserId();
        
        // 1. Calculer coût selon résolution
        int cost = calculateCost(request.getResolution());
        
        // 2. Vérifier solde
        TokenBalanceResponse balance = paymentClient.getTokenBalance(userId);
        
        if (balance.getBalance() < cost) {
            throw new InsufficientTokensException(
                cost, 
                balance.getBalance()
            );
        }
        
        // 3. Générer l'image
        ImageDTO image = imageGenerator.generate(request);
        
        // 4. Consommer les jetons UNIQUEMENT si succès
        paymentClient.consumeTokens(ConsumeTokensRequest.builder()
            .userId(userId)
            .amount(cost)
            .type(TokenTransactionType.SPENT_IMAGE)
            .description("Génération image " + request.getResolution())
            .metadata(toJson(Map.of(
                "imageId", image.getId(),
                "resolution", request.getResolution(),
                "style", request.getStyle()
            )))
            .build()
        );
        
        return image;
    }
    
    private int calculateCost(String resolution) {
        return switch (resolution) {
            case "512x512" -> 10;
            case "1024x1024" -> 30;
            case "2048x2048" -> 100;
            default -> 10;
        };
    }
}
```

#### **Pattern Retry avec Compensation**

```java
@Service
public class ImageGenerationServiceWithRetry {
    
    @Retryable(
        value = {ImageGenerationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public ImageDTO generateImageWithRetry(ImageRequest request) {
        // Générer l'image
        return generateImage(request);
    }
    
    @Recover
    public ImageDTO recoverFromGenerationFailure(
            ImageGenerationException e, 
            ImageRequest request) {
        
        // Créditer les jetons si échec après consommation
        log.error("Échec génération après 3 tentatives, remboursement", e);
        
        paymentClient.consumeTokens(ConsumeTokensRequest.builder()
            .userId(request.getUserId())
            .amount(-calculateCost(request.getResolution())) // Négatif = remboursement
            .type(TokenTransactionType.REFUND)
            .description("Remboursement suite échec génération")
            .build()
        );
        
        throw new ImageGenerationFailedException(
            "Impossible de générer l'image après plusieurs tentatives"
        );
    }
}
```

---

### **MODULE 4 : CONVERSATION ENGINE**

#### **Consommer Jetons pour Messages**

```java
@Service
@RequiredArgsConstructor
public class ConversationService {
    
    private final PaymentClient paymentClient;
    
    public Message sendMessage(String userId, String content) {
        // Vérifier si message nécessite des jetons
        boolean requiresTokens = requiresAdvancedProcessing(content);
        
        if (requiresTokens) {
            // Vérifier solde
            TokenBalanceResponse balance = 
                paymentClient.getTokenBalance(UUID.fromString(userId));
            
            if (balance.getBalance() < 1) {
                throw new InsufficientTokensException(1, balance.getBalance());
            }
        }
        
        // Envoyer message
        Message message = processMessage(userId, content);
        
        // Consommer jeton si nécessaire
        if (requiresTokens) {
            paymentClient.consumeTokens(ConsumeTokensRequest.builder()
                .userId(UUID.fromString(userId))
                .amount(1)
                .type(TokenTransactionType.SPENT_MESSAGE)
                .description("Message avancé")
                .build()
            );
        }
        
        return message;
    }
}
```

---

### **MODULE 9 : MODERATION SYSTEM**

#### **Adapter Modération selon Abonnement**

```java
@Service
@RequiredArgsConstructor
public class ModerationService {
    
    private final PaymentClient paymentClient;
    
    public ModerationLevel getModerationLevel(UUID userId) {
        SubscriptionDTO subscription = paymentClient
            .getCurrentSubscription(userId)
            .orElse(null);
        
        if (subscription == null) {
            return ModerationLevel.STRICT; // Par défaut
        }
        
        return switch (subscription.getPlan()) {
            case FREE, STANDARD -> 
                ModerationLevel.STRICT; // Forcé
            
            case PREMIUM -> {
                // Vérifier préférences utilisateur
                UserPreferences prefs = getUserPreferences(userId);
                yield prefs.getModerationLevel();
            }
            
            case VIP_PLUS -> {
                // Vérifier KYC niveau 3 + consentement
                if (hasValidKYC(userId, 3) && hasConsent(userId)) {
                    yield ModerationLevel.OPTIONAL;
                } else {
                    yield ModerationLevel.LIGHT;
                }
            }
        };
    }
}
```

---

## 💻 CLIENT JAVA (Feign)

Pour faciliter l'intégration, voici un client Feign prêt à l'emploi :

```java
/**
 * Client Feign pour le Payment Service.
 * 
 * @author NexusAI Team
 */
@FeignClient(
    name = "payment-service",
    url = "${payment.service.url}",
    configuration = PaymentClientConfiguration.class
)
public interface PaymentClient {
    
    @GetMapping("/api/v1/subscriptions/current")
    Optional<SubscriptionDTO> getCurrentSubscription(
        @RequestParam("userId") UUID userId
    );
    
    @GetMapping("/api/v1/tokens/balance")
    TokenBalanceResponse getTokenBalance(
        @RequestParam("userId") UUID userId
    );
    
    @PostMapping("/api/v1/tokens/consume")
    TokenTransactionDTO consumeTokens(
        @RequestBody ConsumeTokensRequest request
    );
}

/**
 * Configuration du client Feign.
 */
@Configuration
public class PaymentClientConfiguration {
    
    @Bean
    public RequestInterceptor jwtInterceptor() {
        return template -> {
            String token = SecurityContextHolder.getContext()
                .getAuthentication()
                .getCredentials()
                .toString();
            
            template.header("Authorization", "Bearer " + token);
        };
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 402) {
                return new InsufficientTokensException(/* parse body */);
            }
            return new FeignException.errorStatus(methodKey, response);
        };
    }
}
```

**Dépendances Maven:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**Configuration:**
```yaml
payment:
  service:
    url: ${PAYMENT_SERVICE_URL:http://payment-service:8082}
```

---

## 🛡️ GESTION DES ERREURS

### **Codes d'Erreur HTTP**

| Code | Erreur | Signification |
|------|--------|---------------|
| 400 | Bad Request | Requête invalide |
| 401 | Unauthorized | Token manquant/invalide |
| 402 | Payment Required | Jetons insuffisants |
| 403 | Forbidden | Accès refusé |
| 404 | Not Found | Ressource introuvable |
| 409 | Conflict | Abonnement déjà actif |
| 500 | Internal Error | Erreur serveur |
| 503 | Service Unavailable | Service temporairement indisponible |

### **Stratégie de Retry**

```java
@Configuration
public class ResilienceConfiguration {
    
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(
            100,    // Délai initial (ms)
            5000,   // Délai max (ms)
            3       // Nombre de tentatives
        );
    }
    
    @Bean
    public CircuitBreaker paymentCircuitBreaker() {
        return CircuitBreaker.of(
            "payment-service",
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build()
        );
    }
}
```

---

## 🎯 BEST PRACTICES

### **1. Toujours Vérifier AVANT de Consommer**

❌ **Mauvais:**
```java
// Génère l'image PUIS vérifie jetons
ImageDTO image = generate(request);
paymentClient.consumeTokens(...); // Peut échouer!
```

✅ **Bon:**
```java
// Vérifie jetons AVANT de générer
TokenBalanceResponse balance = paymentClient.getTokenBalance(userId);
if (balance.getBalance() < cost) {
    throw new InsufficientTokensException();
}

ImageDTO image = generate(request);
paymentClient.consumeTokens(...);
```

### **2. Gérer les Transactions Distribuées**

```java
@Transactional
public ImageDTO generateImageSafely(ImageRequest request) {
    try {
        // 1. Réserver les jetons (status PENDING)
        reserveTokens(request.getUserId(), cost);
        
        // 2. Générer image
        ImageDTO image = imageGenerator.generate(request);
        
        // 3. Confirmer consommation
        confirmTokenConsumption(request.getUserId(), cost);
        
        return image;
        
    } catch (Exception e) {
        // 4. Rollback en cas d'échec
        cancelTokenReservation(request.getUserId(), cost);
        throw e;
    }
}
```

### **3. Implémenter Circuit Breaker**

```java
@CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackGetBalance")
public TokenBalanceResponse getTokenBalance(UUID userId) {
    return paymentClient.getTokenBalance(userId);
}

private TokenBalanceResponse fallbackGetBalance(UUID userId, Exception e) {
    log.warn("Payment service indisponible, utilisation cache", e);
    return cacheService.getCachedBalance(userId);
}
```

### **4. Logger les Consommations**

```java
@Aspect
@Component
public class TokenConsumptionAspect {
    
    @AfterReturning(
        pointcut = "execution(* com.nexusai..*.consumeTokens(..))",
        returning = "result"
    )
    public void logTokenConsumption(JoinPoint joinPoint, TokenTransactionDTO result) {
        log.info("Jetons consommés: userId={}, amount={}, type={}", 
            result.getUserId(), 
            result.getAmount(), 
            result.getType()
        );
    }
}
```

### **5. Mettre en Cache les Infos d'Abonnement**

```java
@Cacheable(value = "subscriptions", key = "#userId")
public SubscriptionDTO getSubscription(UUID userId) {
    return paymentClient.getCurrentSubscription(userId)
        .orElse(null);
}

@CacheEvict(value = "subscriptions", key = "#event.userId")
@KafkaListener(topics = "payment.subscription.upgraded")
public void handleSubscriptionUpgraded(SubscriptionUpgradedEvent event) {
    // Cache sera invalidé automatiquement
}
```

---

## ❓ FAQ

### **Q: Que faire si le Payment Service est down ?**

**R:** Implémenter un fallback avec cache :

```java
@Service
public class PaymentServiceWithFallback {
    
    @CircuitBreaker(name = "payment", fallbackMethod = "fallback")
    public TokenBalanceResponse getBalance(UUID userId) {
        return paymentClient.getTokenBalance(userId);
    }
    
    private TokenBalanceResponse fallback(UUID userId, Exception e) {
        // 1. Essayer le cache
        Optional<TokenBalanceResponse> cached = 
            cacheService.getCached(userId);
        
        if (cached.isPresent()) {
            return cached.get();
        }
        
        // 2. Mode dégradé: autoriser avec limite
        return TokenBalanceResponse.builder()
            .userId(userId)
            .balance(10) // Crédit temporaire
            .build();
    }
}
```

### **Q: Comment gérer les remboursements ?**

**R:** Utiliser un montant négatif :

```java
paymentClient.consumeTokens(ConsumeTokensRequest.builder()
    .userId(userId)
    .amount(-50) // Négatif = remboursement
    .type(TokenTransactionType.REFUND)
    .description("Remboursement suite erreur génération")
    .build()
);
```

### **Q: Les événements Kafka sont-ils garantis ?**

**R:** Oui, avec `acks=all` et retry configuré. Implémenter idempotence côté consommateur :

```java
@KafkaListener(topics = "payment.subscription.created")
public void handleSubscriptionCreated(SubscriptionCreatedEvent event) {
    // Vérifier si déjà traité (idempotence)
    if (processedEventRepository.exists(event.getSubscriptionId())) {
        log.debug("Événement déjà traité, skip");
        return;
    }
    
    // Traiter
    processSubscription(event);
    
    // Marquer comme traité
    processedEventRepository.save(event.getSubscriptionId());
}
```

### **Q: Comment tester l'intégration localement ?**

**R:** Utiliser WireMock pour mocker le Payment Service :

```java
@Test
public void testImageGeneration_withMockedPayment() {
    // Mock vérification solde
    wireMockServer.stubFor(get(urlEqualTo("/api/v1/tokens/balance?userId=" + userId))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"balance\": 100}")));
    
    // Mock consommation
    wireMockServer.stubFor(post(urlEqualTo("/api/v1/tokens/consume"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"amount\": -50}")));
    
    // Tester génération
    ImageDTO result = imageService.generateImage(request);
    
    assertThat(result).isNotNull();
}
```

---

## 📞 SUPPORT & CONTACT

**Pour toute question sur l'intégration :**
- 📧 Email: payment-team@nexusai.com
- 💬 Slack: #payment-integration
- 📚 Documentation: https://docs.nexusai.com/payment-api

**En cas de problème en production :**
- 🚨 PagerDuty: payment-service-oncall
- 📞 Hotline: +33 1 XX XX XX XX

---

## ✅ CHECKLIST D'INTÉGRATION

Avant de déployer votre module en production :

- [ ] Client Feign configuré et testé
- [ ] Vérification du solde AVANT opérations coûteuses
- [ ] Gestion des erreurs 402 (jetons insuffisants)
- [ ] Circuit breaker configuré
- [ ] Fallback implémenté
- [ ] Listeners Kafka configurés (si nécessaire)
- [ ] Tests d'intégration avec Payment Service
- [ ] Logs des consommations de jetons
- [ ] Monitoring des appels Payment API
- [ ] Documentation mise à jour

---

*Ce guide est maintenu par l'équipe Payment. Dernière mise à jour : 18 octobre 2025*