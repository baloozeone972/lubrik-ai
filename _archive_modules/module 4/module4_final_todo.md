# MODULE 4 - TODO DÉTAILLÉ
## Ce qu'il reste VRAIMENT à faire (20%)

---

## 🎯 STATUT ACTUEL

### ✅ CE QUI EST FAIT (80%)
- ✅ Architecture complète (6 modules Maven)
- ✅ Toutes les classes principales (50+ classes)
- ✅ DTOs, Entities, Repositories
- ✅ Services métier de base
- ✅ Controllers REST & WebSocket
- ✅ Intégration LLM (OpenAI, Anthropic)
- ✅ Système mémoire (Redis, Pinecone)
- ✅ Tests de base (structure)
- ✅ CI/CD pipelines
- ✅ Documentation complète
- ✅ Générateur automatique
- ✅ Scripts d'automatisation
- ✅ Classes sécurité critiques

### ⚠️ CE QUI RESTE (20%)
Le code est là, mais il manque :
1. **Implémentations concrètes** de certaines méthodes
2. **Dépendances Maven** complètes
3. **Configuration** détaillée
4. **Tests réels** (pas juste la structure)
5. **Intégrations** entre modules
6. **Données de test**

---

## 🔴 PRIORITÉ CRITIQUE (Bloquant MVP)

### 1. Compléter les Dépendances Maven (1 jour) ⚡

**Fichiers à modifier:**

```xml
<!-- conversation-api/pom.xml -->
<dependencies>
    <!-- AJOUTER -->
    <dependency>
        <groupId>com.nexusai</groupId>
        <artifactId>conversation-common</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>com.nexusai</groupId>
        <artifactId>conversation-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <dependency>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-annotations</artifactId>
        <version>2.2.19</version>
    </dependency>
</dependencies>
```

```xml
<!-- conversation-core/pom.xml -->
<dependencies>
    <!-- AJOUTER -->
    <dependency>
        <groupId>com.nexusai</groupId>
        <artifactId>conversation-common</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>com.nexusai</groupId>
        <artifactId>conversation-persistence</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>com.nexusai</groupId>
        <artifactId>conversation-llm</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>com.nexusai</groupId>
        <artifactId>conversation-memory</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

```xml
<!-- conversation-llm/pom.xml -->
<dependencies>
    <!-- AJOUTER -->
    <dependency>
        <groupId>com.theokanning.openai-gpt3-java</groupId>
        <artifactId>service</artifactId>
        <version>0.18.0</version>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

```xml
<!-- conversation-memory/pom.xml -->
<dependencies>
    <!-- AJOUTER -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
    
    <!-- Pinecone (via Maven Central ou repository custom) -->
    <dependency>
        <groupId>io.pinecone</groupId>
        <artifactId>pinecone-client</artifactId>
        <version>0.7.0</version>
    </dependency>
</dependencies>
```

**Action:** Copier-coller ces dépendances dans les POMs respectifs

---

### 2. Implémenter les Méthodes Manquantes (2 jours) ⚡

**Fichiers à compléter:**

#### A. `conversation-core/ConversationService.java`

```java
// LIGNE ~85 - Méthode à compléter
private CreateConversationRequest createRequestFromUserId(String userId) {
    // TODO: Implémenter
    return CreateConversationRequest.builder()
        .userId(userId)
        .build();
}

// LIGNE ~150 - Méthode à compléter
private int getMaxConversationsForUser(String userId) {
    // TODO: Appeler le module User/Payment
    // Pour l'instant, valeur par défaut
    return 50;
}
```

#### B. `conversation-llm/LLMService.java`

```java
// LIGNE ~120 - Méthode manquante
private Mono<CompanionProfile> getCompanionProfile(String companionId) {
    // TODO: Appeler le module Companion
    // Pour l'instant, mock
    return Mono.just(CompanionProfile.builder()
        .companionId(companionId)
        .name("Compagnon")
        .personality(new Personality())
        .build());
}
```

#### C. `conversation-memory/EmbeddingService.java`

```java
// LIGNE ~60 - Implémenter le cache LRU
import java.util.LinkedHashMap;

class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    
    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

**Action:** 
1. Ouvrir chaque fichier mentionné
2. Chercher `// TODO` ou `return null`
3. Remplacer par implémentation réelle ou mock

---

### 3. Configuration Complète (1 jour) ⚡

**Fichier: `conversation-api/src/main/resources/application.yml`**

```yaml
# AJOUTER les configurations manquantes

server:
  port: 8080
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,application/json
  http2:
    enabled: true

spring:
  application:
    name: conversation-service
  
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/nexusai_conversations}
      database: nexusai_conversations
      auto-index-creation: true
    
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

# OpenAI Configuration
openai:
  api-key: ${OPENAI_API_KEY}
  timeout-seconds: 30

# Anthropic Configuration  
anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  api-url: https://api.anthropic.com/v1/messages
  timeout-seconds: 30

# Pinecone Configuration
pinecone:
  api-key: ${PINECONE_API_KEY}
  environment: ${PINECONE_ENVIRONMENT:us-west1-gcp}
  index-name: nexusai-conversations

# Logging
logging:
  level:
    root: INFO
    com.nexusai: DEBUG
```

**Action:** Créer ou compléter ce fichier

---

### 4. Créer Classes Manquantes Simples (1 jour) ⚡

#### A. CompanionProfile (conversation-common)

```java
package com.nexusai.conversation.common.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionProfile {
    private String companionId;
    private String name;
    private String backstory;
    private Personality personality;
    private VoiceSettings voice;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Personality {
    private Map<String, Integer> traits;
    private List<String> interests;
    private String communicationStyle;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class VoiceSettings {
    private String voiceId;
    private float pitch;
    private float speed;
}
```

#### B. Event Models (conversation-common)

```java
package com.nexusai.conversation.common.events;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEvent {
    private String eventType;
    private String conversationId;
    private String userId;
    private Instant timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {
    private String eventType;
    private String conversationId;
    private String messageId;
    private String sender;
    private Instant timestamp;
}
```

**Action:** Créer ces fichiers dans conversation-common

---

### 5. Tests Basiques Fonctionnels (2 jours) ⚡

**Remplacer les mocks par de vrais tests:**

```java
// conversation-api/src/test/java/ConversationControllerTest.java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/test_db",
    "openai.api-key=test-key",
    "anthropic.api-key=test-key"
})
class ConversationControllerIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private ConversationRepository repository;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
    }
    
    @Test
    void testCreateConversation() {
        CreateConversationRequest request = CreateConversationRequest.builder()
            .userId("test-user")
            .companionId("test-companion")
            .title("Test Conversation")
            .build();
        
        webTestClient.post()
            .uri("/api/v1/conversations")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isNotEmpty()
            .jsonPath("$.userId").isEqualTo("test-user")
            .jsonPath("$.title").isEqualTo("Test Conversation");
    }
    
    @Test
    void testGetConversation() {
        // Créer une conversation de test
        ConversationEntity entity = ConversationEntity.builder()
            .userId("test-user")
            .companionId("test-companion")
            .messages(new ArrayList<>())
            .createdAt(Instant.now())
            .build();
        
        ConversationEntity saved = repository.save(entity).block();
        
        // Tester GET
        webTestClient.get()
            .uri("/api/v1/conversations/" + saved.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(saved.getId())
            .jsonPath("$.userId").isEqualTo("test-user");
    }
}
```

**Action:** Créer 10-15 tests similaires qui testent vraiment les endpoints

---

## 🟠 PRIORITÉ HAUTE (1 semaine)

### 6. Intégrations Entre Modules (3 jours)

**A. Créer un Mock du Module User**

```java
// conversation-core/src/main/java/integration/UserServiceClient.java

@Service
@Slf4j
public class UserServiceClient {
    
    private final WebClient webClient;
    
    @Value("${services.user.url:http://localhost:8081}")
    private String userServiceUrl;
    
    public UserServiceClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }
    
    /**
     * Vérifie si un utilisateur existe
     */
    public Mono<Boolean> userExists(String userId) {
        return webClient.get()
            .uri(userServiceUrl + "/api/v1/users/{id}", userId)
            .retrieve()
            .toBodilessEntity()
            .map(response -> response.getStatusCode().is2xxSuccessful())
            .onErrorReturn(false);
    }
    
    /**
     * Récupère le quota de conversations d'un utilisateur
     */
    public Mono<Integer> getUserConversationQuota(String userId) {
        return webClient.get()
            .uri(userServiceUrl + "/api/v1/users/{id}/quota", userId)
            .retrieve()
            .bodyToMono(UserQuotaResponse.class)
            .map(UserQuotaResponse::getMaxConversations)
            .onErrorReturn(20); // Valeur par défaut
    }
}

@Data
class UserQuotaResponse {
    private Integer maxConversations;
    private Integer maxMessagesPerDay;
}
```

**B. Créer un Mock du Module Companion**

```java
// conversation-llm/src/main/java/integration/CompanionServiceClient.java

@Service
@Slf4j
public class CompanionServiceClient {
    
    private final WebClient webClient;
    
    @Value("${services.companion.url:http://localhost:8082}")
    private String companionServiceUrl;
    
    public CompanionServiceClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }
    
    /**
     * Récupère le profil d'un compagnon
     */
    public Mono<CompanionProfile> getCompanionProfile(String companionId) {
        return webClient.get()
            .uri(companionServiceUrl + "/api/v1/companions/{id}", companionId)
            .retrieve()
            .bodyToMono(CompanionProfile.class)
            .onErrorResume(error -> {
                log.warn("Erreur récupération compagnon, utilisation mock");
                return Mono.just(createMockProfile(companionId));
            });
    }
    
    private CompanionProfile createMockProfile(String companionId) {
        return CompanionProfile.builder()
            .companionId(companionId)
            .name("Compagnon Mock")
            .backstory("Un compagnon de test")
            .personality(Personality.builder()
                .traits(Map.of(
                    "openness", 70,
                    "empathy", 80
                ))
                .build())
            .build();
    }
}
```

**Action:** Créer ces clients et les injecter dans les services

---

### 7. Données de Test & Seeds (2 jours)

**Créer un service d'initialisation:**

```java
// conversation-core/src/main/java/seed/DataSeeder.java

@Component
@Slf4j
public class DataSeeder implements ApplicationRunner {
    
    private final ConversationRepository repository;
    
    @Value("${app.seed-data:false}")
    private boolean shouldSeedData;
    
    public DataSeeder(ConversationRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public void run(ApplicationArguments args) {
        if (shouldSeedData) {
            seedTestData();
        }
    }
    
    private void seedTestData() {
        log.info("Création de données de test...");
        
        // Créer 3 conversations de test
        List<ConversationEntity> conversations = List.of(
            createTestConversation("user-1", "companion-1", "Première conversation"),
            createTestConversation("user-1", "companion-2", "Deuxième conversation"),
            createTestConversation("user-2", "companion-1", "Conversation utilisateur 2")
        );
        
        Flux.fromIterable(conversations)
            .flatMap(repository::save)
            .doOnNext(conv -> log.info("Conversation créée: {}", conv.getId()))
            .blockLast();
        
        log.info("✅ Données de test créées");
    }
    
    private ConversationEntity createTestConversation(
            String userId, 
            String companionId,
            String title) {
        
        List<MessageEntity> messages = List.of(
            MessageEntity.builder()
                .id(UUID.randomUUID().toString())
                .sender("USER")
                .content("Bonjour!")
                .type("TEXT")
                .timestamp(Instant.now())
                .reactions(new ArrayList<>())
                .build(),
            MessageEntity.builder()
                .id(UUID.randomUUID().toString())
                .sender("COMPANION")
                .content("Bonjour! Comment puis-je vous aider?")
                .type("TEXT")
                .timestamp(Instant.now().plusSeconds(2))
                .reactions(new ArrayList<>())
                .build()
        );
        
        return ConversationEntity.builder()
            .userId(userId)
            .companionId(companionId)
            .title(title)
            .messages(messages)
            .context(ContextEntity.builder()
                .topics(List.of("greeting"))
                .emotionalTone("NEUTRAL")
                .messageCount(2)
                .build())
            .createdAt(Instant.now())
            .lastMessageAt(Instant.now())
            .build();
    }
}
```

**Dans application.yml:**
```yaml
app:
  seed-data: true  # Mettre à false en prod
```

**Action:** Créer ce seeder pour avoir des données de test

---

## 🟡 PRIORITÉ MOYENNE (Optionnel)

### 8. Métriques Prometheus (1 jour)

```java
// conversation-core/src/main/java/metrics/ConversationMetrics.java

@Component
public class ConversationMetrics {
    
    private final Counter messagesCounter;
    private final Timer responseTimer;
    
    public ConversationMetrics(MeterRegistry registry) {
        this.messagesCounter = Counter.builder("conversation.messages.total")
            .description("Total messages sent")
            .register(registry);
        
        this.responseTimer = Timer.builder("conversation.response.time")
            .description("LLM response time")
            .register(registry);
    }
    
    public void recordMessage() {
        messagesCounter.increment();
    }
    
    public void recordResponseTime(Duration duration) {
        responseTimer.record(duration);
    }
}
```

### 9. Healthchecks Custom (1 jour)

```java
// conversation-api/src/main/java/health/ConversationHealthIndicator.java

@Component
public class ConversationHealthIndicator implements ReactiveHealthIndicator {
    
    private final ConversationRepository repository;
    private final ReactiveRedisTemplate redisTemplate;
    
    @Override
    public Mono<Health> health() {
        return Mono.zip(
            checkMongoDB(),
            checkRedis(),
            checkKafka()
        ).map(tuple -> {
            boolean mongoUp = tuple.getT1();
            boolean redisUp = tuple.getT2();
            boolean kafkaUp = tuple.getT3();
            
            if (mongoUp && redisUp && kafkaUp) {
                return Health.up()
                    .withDetail("mongodb", "UP")
                    .withDetail("redis", "UP")
                    .withDetail("kafka", "UP")
                    .build();
            } else {
                return Health.down()
                    .withDetail("mongodb", mongoUp ? "UP" : "DOWN")
                    .withDetail("redis", redisUp ? "UP" : "DOWN")
                    .withDetail("kafka", kafkaUp ? "UP" : "DOWN")
                    .build();
            }
        });
    }
    
    private Mono<Boolean> checkMongoDB() {
        return repository.count()
            .map(count -> true)
            .onErrorReturn(false);
    }
    
    private Mono<Boolean> checkRedis() {
        return redisTemplate.hasKey("health-check")
            .onErrorReturn(false);
    }
    
    private Mono<Boolean> checkKafka() {
        // TODO: Implémenter vérification Kafka
        return Mono.just(true);
    }
}
```

---

## 📋 CHECKLIST FINALE

### Avant de considérer le module "DONE"

#### Code & Build
- [ ] Toutes les dépendances Maven ajoutées
- [ ] Aucun `// TODO` dans le code critique
- [ ] Aucune méthode qui retourne `null` par défaut
- [ ] `mvn clean install` passe sans erreur
- [ ] Aucun warning de compilation

#### Configuration
- [ ] `application.yml` complet
- [ ] `.env.example` avec toutes les variables
- [ ] `docker-compose.yml` fonctionnel
- [ ] Variables d'environnement documentées

#### Tests
- [ ] Au moins 15 tests d'intégration fonctionnels
- [ ] Tests des endpoints principaux (CRUD conversations)
- [ ] Tests WebSocket basiques
- [ ] `mvn test` passe avec >70% success
- [ ] Couverture de code >70%

#### Infrastructure
- [ ] MongoDB se lance et accepte connexions
- [ ] Redis se lance et accepte connexions
- [ ] Kafka se lance (même si non utilisé pour MVP)
- [ ] `docker-compose up` sans erreurs

#### Intégrations
- [ ] Clients créés pour modules externes (User, Companion)
- [ ] Fallback/mocks en place si services indisponibles
- [ ] Kafka events émis correctement
- [ ] WebSocket fonctionne en local

#### Documentation
- [ ] README avec instructions claires
- [ ] API documentée (Swagger)
- [ ] Variables d'environnement expliquées
- [ ] Guide de déploiement

#### Sécurité (MVP)
- [ ] Rate limiting activé
- [ ] Input validation sur endpoints
- [ ] CORS configuré
- [ ] Headers sécurité (HTTPS ready)

---

## ⏱️ PLANNING RÉALISTE

### Semaine 1 (Sprint MVP)
**Lundi-Mardi:** Priorité Critique (tâches 1-3)
- Dépendances Maven
- Méthodes manquantes
- Configuration

**Mercredi-Vendredi:** Tests & Intégrations (tâches 4-5)
- Tests basiques
- Data seeder

### Semaine 2 (Sprint Production-Ready)
**Lundi-Mercredi:** Intégrations (tâche 6-7)
- Clients modules externes
- Données de test complètes

**Jeudi-Vendredi:** Polish & Optionnel (tâches 8-9)
- Métriques
- Healthchecks
- Documentation finale

---

## 🎯 ESTIMATION GLOBALE

| Priorité | Tâches | Temps | Effort |
|----------|--------|-------|---------|
| **Critique** | 1-5 | 7 jours | 1 dev full-time |
| **Haute** | 6-7 | 5 jours | 1 dev full-time |
| **Moyenne** | 8-9 | 2 jours | 1 dev part-time |
| **TOTAL** | 9 tâches | **14 jours** | **1 développeur** |

## 💰 COÛT ESTIMÉ

- 14 jours × €500/jour = **€7,000**

---

## 🚀 DÉMARRAGE IMMÉDIAT

Pour commencer MAINTENANT:

```bash
# 1. Générer le projet
./generate-and-deploy.sh ./nexusai-conversation-module

# 2. Ouvrir dans IDE
cd nexusai-conversation-module
idea . # ou code .

# 3. Commencer par les dépendances
# Ouvrir chaque pom.xml et ajouter les dépendances listées ci-dessus

# 4. Compléter les TODO
# Rechercher "// TODO" et implémenter

# 5. Tester
mvn clean test

# 6. Run
mvn spring-boot:run -pl conversation-api
```

---

**Le module est à 80% ✅**  
**Il reste 20% de travail concret = 14 jours = 1 développeur**  
**Après ça: 100% PRODUCTION READY! 🚀**
