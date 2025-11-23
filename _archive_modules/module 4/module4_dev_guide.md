# MODULE 4 : CONVERSATION ENGINE
## Guide Complet de Développement & Répartition des Tâches

---

## 📋 TABLE DES MATIÈRES

1. [Vue d'Ensemble](#vue-densemble)
2. [Architecture du Module](#architecture-du-module)
3. [Répartition des Tâches](#répartition-des-tâches)
4. [Planning de Développement](#planning-de-développement)
5. [Standards de Code](#standards-de-code)
6. [Procédures de Test](#procédures-de-test)
7. [Déploiement](#déploiement)
8. [Documentation API](#documentation-api)

---

## 🎯 VUE D'ENSEMBLE

### Objectif du Module
Le Module 4 - Conversation Engine gère l'ensemble du système de chat entre les utilisateurs et leurs compagnons IA, incluant :
- Chat textuel temps réel via WebSocket
- Intégration avec les LLMs (OpenAI, Anthropic)
- Système de mémoire court et long terme
- Historique et recherche dans les conversations
- Détection d'émotions

### Technologies Principales
- **Backend**: Java 21, Spring Boot 3.2, WebFlux (Reactive)
- **Base de données**: MongoDB 7 (historique), Redis 7 (cache)
- **Vector DB**: Pinecone (mémoire sémantique)
- **Messaging**: Kafka (événements)
- **LLM**: OpenAI GPT-4, Anthropic Claude

### Statistiques Cibles
- **Temps de réponse API**: < 100ms (P95)
- **Latence WebSocket**: < 50ms
- **Génération LLM**: 2-5 secondes
- **Throughput**: 10,000 messages/seconde

---

## 🏗️ ARCHITECTURE DU MODULE

### Structure Multi-Module Maven

```
nexusai-conversation-module/
├── pom.xml (parent)
│
├── conversation-common/         [Shared DTOs, Enums, Exceptions]
│   ├── dto/
│   ├── enums/
│   └── exceptions/
│
├── conversation-api/            [REST & WebSocket Controllers]
│   ├── controller/
│   ├── websocket/
│   └── exception-handler/
│
├── conversation-core/           [Business Logic]
│   ├── service/
│   ├── orchestration/
│   └── events/
│
├── conversation-llm/            [LLM Integration]
│   ├── provider/
│   ├── prompt/
│   └── emotion/
│
├── conversation-memory/         [Memory System]
│   ├── short-term/
│   ├── long-term/
│   ├── embedding/
│   └── extraction/
│
└── conversation-persistence/    [Data Access Layer]
    ├── entity/
    ├── repository/
    └── mapper/
```

### Flux de Données

```
┌──────────────────────────────────────────────────────────────┐
│                    MESSAGE FLOW                              │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. User sends message via WebSocket                        │
│     ↓                                                        │
│  2. API Layer validates & routes                            │
│     ↓                                                        │
│  3. Core Service orchestrates:                              │
│     ├─→ Save user message (Persistence)                     │
│     ├─→ Get context (Memory Service)                        │
│     ├─→ Call LLM (LLM Service)                              │
│     └─→ Save companion response (Persistence)               │
│     ↓                                                        │
│  4. Emit Kafka event                                        │
│     ↓                                                        │
│  5. Send response via WebSocket                             │
│     ↓                                                        │
│  6. Update memory (async)                                   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 👥 RÉPARTITION DES TÂCHES

### DÉVELOPPEUR 1: conversation-api

**Responsable**: Exposition des APIs REST et WebSocket

#### Tâches Principales
- [ ] Créer les controllers REST pour CRUD conversations
- [ ] Implémenter le handler WebSocket pour chat temps réel
- [ ] Créer les DTOs de requête/réponse avec validation
- [ ] Implémenter le global exception handler
- [ ] Documenter les APIs avec Swagger/OpenAPI
- [ ] Créer les tests E2E des endpoints

#### Livrables
- `ConversationController.java`
- `MessageController.java`
- `ConversationWebSocketHandler.java`
- `WebSocketConfig.java`
- `GlobalExceptionHandler.java`
- Tests E2E complets

#### Dépendances
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### Timeline
- **Semaine 1**: Setup + Controllers REST basiques
- **Semaine 2**: WebSocket handler + tests
- **Semaine 3**: Exception handling + documentation
- **Semaine 4**: Tests E2E + optimisation
- **Semaine 5**: Intégration finale

---

### DÉVELOPPEUR 2: conversation-core

**Responsable**: Logique métier et orchestration

#### Tâches Principales
- [ ] Implémenter ConversationService (CRUD + orchestration)
- [ ] Créer ContextService pour gestion du contexte
- [ ] Implémenter EventPublisher pour Kafka
- [ ] Gérer les règles business (quotas, validations)
- [ ] Créer le système de résumés automatiques
- [ ] Implémenter les tâches planifiées (cleanup)

#### Livrables
- `ConversationService.java`
- `ContextService.java`
- `EventPublisher.java`
- `ScheduledTasks.java`
- Tests unitaires complets

#### Dépendances
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

#### Timeline
- **Semaine 1**: ConversationService basique
- **Semaine 2**: ContextService + Redis
- **Semaine 3**: EventPublisher + Kafka
- **Semaine 4**: Règles business + validation
- **Semaine 5**: Scheduled tasks + intégration

---

### DÉVELOPPEUR 3: conversation-llm

**Responsable**: Intégration des LLMs

#### Tâches Principales
- [ ] Implémenter LLMService principal
- [ ] Créer OpenAIProvider (GPT-4)
- [ ] Créer AnthropicProvider (Claude)
- [ ] Implémenter PromptBuilder
- [ ] Créer EmotionDetectionService
- [ ] Gérer le fallback automatique entre providers

#### Livrables
- `LLMService.java`
- `OpenAIProvider.java`
- `AnthropicProvider.java`
- `PromptBuilder.java`
- `EmotionDetectionService.java`
- Tests unitaires + intégration avec APIs

#### Dépendances
```xml
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

#### Configuration Requise
```yaml
openai:
  api-key: ${OPENAI_API_KEY}
  timeout-seconds: 30

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  api-url: https://api.anthropic.com/v1/messages
  timeout-seconds: 30
```

#### Timeline
- **Semaine 1**: LLMService + OpenAIProvider
- **Semaine 2**: AnthropicProvider + fallback
- **Semaine 3**: PromptBuilder + personnalisation
- **Semaine 4**: EmotionDetectionService
- **Semaine 5**: Tests + optimisation

---

### DÉVELOPPEUR 4: conversation-memory

**Responsable**: Système de mémoire

#### Tâches Principales
- [ ] Implémenter MemoryService principal
- [ ] Créer ShortTermMemoryService (Redis)
- [ ] Créer LongTermMemoryService (Pinecone)
- [ ] Implémenter EmbeddingService (OpenAI)
- [ ] Créer MemoryExtractionService
- [ ] Gérer la recherche sémantique

#### Livrables
- `MemoryService.java`
- `ShortTermMemoryService.java`
- `LongTermMemoryService.java`
- `EmbeddingService.java`
- `MemoryExtractionService.java`
- `PineconeClient.java`
- Tests unitaires + intégration

#### Dépendances
```xml
<dependency>
    <groupId>io.pinecone</groupId>
    <artifactId>pinecone-client</artifactId>
    <version>0.7.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

#### Configuration Pinecone
```yaml
pinecone:
  api-key: ${PINECONE_API_KEY}
  environment: us-west1-gcp
  index-name: nexusai-conversations
  namespace-prefix: conv
```

#### Timeline
- **Semaine 1**: Setup + ShortTermMemoryService
- **Semaine 2**: LongTermMemoryService + Pinecone
- **Semaine 3**: EmbeddingService
- **Semaine 4**: MemoryExtractionService
- **Semaine 5**: Tests + optimisation recherche

---

### DÉVELOPPEUR 5: conversation-persistence

**Responsable**: Couche d'accès aux données

#### Tâches Principales
- [ ] Créer les entités MongoDB
- [ ] Implémenter les repositories réactifs
- [ ] Créer les index pour optimisation
- [ ] Implémenter les requêtes custom
- [ ] Créer les mappers (Entity ↔ DTO)
- [ ] Gérer les migrations de données

#### Livrables
- `ConversationEntity.java`
- `MessageEntity.java`
- `ContextEntity.java`
- `ConversationRepository.java`
- `ConversationCustomRepository.java`
- `ConversationMapper.java`
- Tests d'intégration MongoDB

#### Dépendances
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

#### Index MongoDB
```javascript
// conversations collection
db.conversations.createIndex({ userId: 1, lastMessageAt: -1 });
db.conversations.createIndex({ companionId: 1 });
db.conversations.createIndex({ tags: 1 });
db.conversations.createIndex({ isEphemeral: 1, expiresAt: 1 });
```

#### Timeline
- **Semaine 1**: Entités + repositories basiques
- **Semaine 2**: Repositories custom + requêtes complexes
- **Semaine 3**: Mappers + validation
- **Semaine 4**: Optimisation + index
- **Semaine 5**: Tests intégration + migrations

---

## 📅 PLANNING DE DÉVELOPPEMENT

### Vue d'Ensemble (5 Semaines)

| Semaine | Dev 1 (API) | Dev 2 (Core) | Dev 3 (LLM) | Dev 4 (Memory) | Dev 5 (Persistence) |
|---------|-------------|--------------|-------------|----------------|---------------------|
| **S1** | Controllers REST | ConversationService | OpenAI Provider | Short-term Memory | Entités + Repos |
| **S2** | WebSocket | ContextService | Anthropic + Fallback | Long-term + Pinecone | Custom Repos |
| **S3** | Exception Handler | Event Publisher | PromptBuilder | EmbeddingService | Mappers |
| **S4** | Tests E2E | Business Rules | EmotionDetection | MemoryExtraction | Optimisation |
| **S5** | Intégration | Scheduled Tasks | Tests + Optim | Tests + Optim | Tests + Migration |

### Jalons (Milestones)

#### 🎯 Milestone 1 - Fin Semaine 2
**Objectif**: MVP fonctionnel du chat basique
- ✅ API REST opérationnelle
- ✅ Service de conversation avec sauvegarde MongoDB
- ✅ Intégration OpenAI fonctionnelle
- ✅ Mémoire court terme (Redis)
- ✅ Entités et repositories complets

**Critère de succès**: Pouvoir créer une conversation et envoyer/recevoir des messages

#### 🎯 Milestone 2 - Fin Semaine 4
**Objectif**: Fonctionnalités avancées
- ✅ WebSocket temps réel opérationnel
- ✅ Système de contexte + résumés
- ✅ Fallback LLM fonctionnel
- ✅ Mémoire long terme + recherche sémantique
- ✅ Tous les index optimisés

**Critère de succès**: Chat temps réel avec mémoire persistante et recherche

#### 🎯 Milestone 3 - Fin Semaine 5
**Objectif**: Production-ready
- ✅ Tous les tests passent (unitaires + intégration + E2E)
- ✅ Documentation complète
- ✅ Métriques et monitoring configurés
- ✅ Performance validée (targets atteintes)
- ✅ Déploiement Docker fonctionnel

**Critère de succès**: Module déployable en production

---

## 📝 STANDARDS DE CODE

### Conventions Java

#### Nommage
```java
// Classes: PascalCase
public class ConversationService { }

// Méthodes: camelCase
public Mono<ConversationDTO> createConversation() { }

// Constantes: UPPER_SNAKE_CASE
public static final int MAX_MESSAGES = 10_000;

// Variables: camelCase
private String conversationId;
```

#### Documentation JavaDoc
```java
/**
 * Crée une nouvelle conversation entre un utilisateur et un compagnon
 * 
 * Ce service orchestre:
 * - La création de l'entité conversation
 * - L'initialisation du contexte
 * - L'émission d'événements Kafka
 * 
 * @param request Données de création (userId, companionId, title)
 * @return Mono contenant la conversation créée
 * @throws QuotaExceededException si l'utilisateur a dépassé son quota
 */
public Mono<ConversationDTO> createConversation(CreateConversationRequest request) {
    // Implementation
}
```

#### Gestion d'Erreurs
```java
// Utiliser des exceptions custom explicites
public class ConversationNotFoundException extends RuntimeException {
    private final String conversationId;
    
    public ConversationNotFoundException(String conversationId) {
        super("Conversation non trouvée: " + conversationId);
        this.conversationId = conversationId;
    }
}

// Gérer les erreurs dans les Mono/Flux
return conversationRepository.findById(id)
    .switchIfEmpty(Mono.error(new ConversationNotFoundException(id)))
    .doOnError(error -> log.error("Erreur récupération conversation", error));
```

#### Logging
```java
@Slf4j
public class ConversationService {
    
    public Mono<ConversationDTO> createConversation(CreateConversationRequest request) {
        log.info("Création conversation pour userId={}", request.getUserId());
        
        return // implementation
            .doOnSuccess(conv -> log.info("Conversation créée: {}", conv.getId()))
            .doOnError(error -> log.error("Erreur création", error));
    }
}
```

### Git Workflow

#### Branches
```bash
# Format: type/description-courte
feature/websocket-handler
fix/mongodb-index-issue
refactor/llm-service-cleanup
```

#### Commits
```bash
# Format: type(scope): description
git commit -m "feat(api): add WebSocket support for real-time chat"
git commit -m "fix(memory): resolve Redis connection leak"
git commit -m "docs(readme): update installation guide"
git commit -m "test(core): add unit tests for ConversationService"
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `chore`

#### Pull Requests
- Titre descriptif
- Description détaillée des changements
- Lien vers le ticket Jira/issue
- Screenshots si UI
- Tests ajoutés/modifiés
- Review required: 2 développeurs minimum

---

## 🧪 PROCÉDURES DE TEST

### Pyramide de Tests

```
              ┌────────┐
              │  E2E   │  10%  (Tests complets API)
              └────────┘
           ┌──────────────┐
           │ Intégration  │  30%  (Tests avec BD réelles)
           └──────────────┘
      ┌──────────────────────┐
      │    Unitaires         │  60%  (Tests isolés avec mocks)
      └──────────────────────┘
```

### Tests Unitaires

**Objectif**: 80% de couverture minimum

```java
@Test
@DisplayName("Création d'une conversation réussie")
void testCreateConversation_Success() {
    // Given
    CreateConversationRequest request = // ...
    when(repository.save(any())).thenReturn(Mono.just(entity));
    
    // When & Then
    StepVerifier.create(service.createConversation(request))
        .assertNext(result -> {
            assertThat(result.getId()).isNotNull();
            assertThat(result.getUserId()).isEqualTo("user-123");
        })
        .verifyComplete();
    
    verify(repository, times(1)).save(any());
}
```

### Tests d'Intégration

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/nexusai_test"
})
class ConversationRepositoryIntegrationTest {
    
    @Autowired
    private ConversationRepository repository;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
    }
    
    @Test
    void testSaveAndFind_Success() {
        // Test avec vraie base de données
    }
}
```

### Tests E2E

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ConversationControllerE2ETest {
    
    @Autowired
    private WebTestClient webClient;
    
    @Test
    void testCreateConversation_E2E() {
        webClient.post()
            .uri("/api/v1/conversations")
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isNotEmpty();
    }
}
```

### Exécution des Tests

```bash
# Tous les tests
mvn test

# Tests d'un module spécifique
mvn test -pl conversation-core

# Tests avec couverture
mvn test jacoco:report

# Tests d'intégration seulement
mvn verify -Pintegration-tests

# Tests de performance (manuels)
mvn test -Dtest=PerformanceTest
```

---

## 🚀 DÉPLOIEMENT

### Environnements

#### Développement Local
```bash
# 1. Démarrer l'infrastructure
docker-compose up -d mongodb redis kafka

# 2. Build le projet
mvn clean package

# 3. Lancer l'application
java -jar conversation-api/target/conversation-api-1.0.0.jar

# Ou via Maven
mvn spring-boot:run -pl conversation-api
```

#### Docker
```bash
# Build image
docker build -t nexusai/conversation-service:1.0.0 .

# Run container
docker run -d \
  -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e MONGODB_URI=mongodb://... \
  nexusai/conversation-service:1.0.0
```

#### Kubernetes
```bash
# Apply configurations
kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml
kubectl apply -f k8s/ingress.yml

# Check status
kubectl get pods -n nexusai
kubectl logs -f conversation-service-xxx

# Scale
kubectl scale deployment conversation-service --replicas=5
```

### Variables d'Environnement

**Requises**:
```bash
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
PINECONE_API_KEY=...
PINECONE_ENVIRONMENT=us-west1-gcp
MONGODB_URI=mongodb://...
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

**Optionnelles**:
```bash
SPRING_PROFILES_ACTIVE=prod
LLM_DEFAULT_PROVIDER=openai
LLM_MAX_TOKENS=1000
LLM_TEMPERATURE=0.8
LOG_LEVEL=INFO
```

### Monitoring

#### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Liveness (Kubernetes)
GET /actuator/health/liveness

# Readiness (Kubernetes)
GET /actuator/health/readiness
```

#### Métriques Prometheus
```bash
# Metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Grafana dashboards disponibles dans /monitoring/grafana/
```

---

## 📚 DOCUMENTATION API

### Swagger UI

Accessible à: `http://localhost:8080/swagger-ui.html`

### Endpoints Principaux

#### Conversations

```http
POST /api/v1/conversations
Content-Type: application/json

{
  "userId": "user-123",
  "companionId": "companion-456",
  "title": "Ma conversation",
  "isEphemeral": false,
  "tags": ["work"]
}

Response 201:
{
  "id": "conv-789",
  "userId": "user-123",
  "companionId": "companion-456",
  "title": "Ma conversation",
  "messages": [],
  "createdAt": "2025-01-15T10:00:00Z"
}
```

```http
GET /api/v1/conversations/{id}

Response 200:
{
  "id": "conv-789",
  "title": "Ma conversation",
  "messages": [...],
  "context": {...}
}
```

#### Messages

```http
POST /api/v1/conversations/{id}/messages
Content-Type: application/json

{
  "content": "Bonjour!",
  "type": "TEXT"
}

Response 200:
{
  "id": "msg-456",
  "sender": "COMPANION",
  "content": "Bonjour! Comment vas-tu?",
  "timestamp": "2025-01-15T10:01:00Z",
  "detectedEmotion": "JOY"
}
```

### WebSocket

```javascript
// Connexion
const ws = new WebSocket('ws://localhost:8080/ws/conversations/conv-789');

// Envoyer message
ws.send(JSON.stringify({
  type: 'MESSAGE',
  content: 'Bonjour!',
  metadata: {}
}));

// Recevoir message
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log(message.content);
};
```

---

## ✅ CHECKLIST AVANT MERGE

- [ ] Tous les tests passent (unitaires + intégration)
- [ ] Couverture de code > 80%
- [ ] JavaDoc complet sur les méthodes publiques
- [ ] Pas de code commenté inutile
- [ ] Logging approprié (INFO, DEBUG, ERROR)
- [ ] Gestion d'erreurs complète
- [ ] Performance validée (pas de régression)
- [ ] Documentation API mise à jour
- [ ] Variables sensibles en environnement (pas en dur)
- [ ] Code review approuvée par 2 développeurs

---

## 📞 CONTACTS & SUPPORT

**Lead Tech**: lead@nexusai.com  
**DevOps**: devops@nexusai.com  
**Documentation**: https://docs.nexusai.com

---

*Guide créé le 2025-01-15 - Version 1.0.0*
