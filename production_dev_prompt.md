# Prompt de Développement - Plateforme Chat IA Multi-Modèles (Production Ready)

## Contexte et Objectif

Vous êtes un architecte logiciel senior chargé de développer une **plateforme de chat IA de grade production** permettant d'interagir avec 1000+ modèles d'IA différents. Cette plateforme doit être **scalable, sécurisée, performante** et utiliser exclusivement des **technologies open-source** pour minimiser les coûts.

## Spécifications Techniques Obligatoires

### Architecture Globale
- **Langage principal**: Java 21 avec Spring Boot 3.2+
- **Architecture**: Microservices avec projet parent Maven multi-modules
- **Déploiement**: Kubernetes avec optimisation coûts
- **Base de données**: PostgreSQL (principal) + Redis (cache/sessions)
- **Message Queue**: Apache Kafka pour communication asynchrone
- **API Gateway**: Spring Cloud Gateway avec load balancing
- **Frontend**: Vue.js 3 + TypeScript + Quasar Framework

### Contraintes de Production
- **Sécurité**: OAuth2/JWT, chiffrement TLS, validation OWASP
- **Performance**: Temps réponse < 200ms, support 10K utilisateurs concurrents
- **Monitoring**: Prometheus + Grafana + ELK Stack
- **Observabilité**: Tracing distribué, métriques métier, alerting
- **Résilience**: Circuit breakers, retry policies, graceful degradation
- **Compliance**: RGPD, audit logs, data retention policies

## Modules à Développer

### 1. Module Core API (`core-api/`)
```java
// Structure attendue
src/main/java/com/chatai/core/
├── config/          // Configuration Spring, Security, Database
├── dto/            // Objects de transfert de données
├── entity/         // Entités JPA avec audit
├── exception/      // Gestion centralisée des erreurs
├── security/       // JWT, OAuth2, RBAC
└── utils/          // Utilitaires, validateurs, constants
```

**Fonctionnalités requises:**
- Configuration multi-environnements (dev, staging, prod)
- Gestion des erreurs avec codes standardisés
- Validation automatique des DTOs
- Audit trail complet (qui, quand, quoi)
- Rate limiting configurable par utilisateur/endpoint

### 2. Module Gateway (`gateway/`)
```java
// Fonctionnalités critiques
- Routage intelligent avec failover
- Authentication/Authorization centralisée  
- Rate limiting global et par utilisateur
- Request/Response logging et monitoring
- CORS configuration sécurisée
- Circuit breaker sur services downstream
```

**Configuration Kubernetes requise:**
- Ingress avec certificats SSL automatiques
- Service Mesh (Istio) pour observabilité
- Auto-scaling basé sur métriques custom
- Health checks et readiness probes

### 3. Module Model Manager (`model-manager/`)
```java
// Gestion intelligente des modèles IA
public interface ModelManagerService {
    // Catalogue dynamique 1000+ modèles
    List<ModelMetadata> getAvailableModels(String category);
    
    // Load balancing intelligent
    ModelInstance getOptimalModel(ModelRequest request);
    
    // Health monitoring temps réel
    ModelHealth checkModelHealth(String modelId);
    
    // Cache prédictif
    void preloadModel(String modelId, PredictionContext context);
}
```

**Intégrations requises:**
- **Ollama** (modèles locaux): Llama 2/3, Mistral, CodeLlama, Vicuna
- **APIs externes**: OpenAI, Anthropic, Cohere, Hugging Face
- **Modèles spécialisés**: StarCoder, mT5, Whisper, DALL-E
- **Custom models**: Support fine-tuning et déploiement

### 4. Module Chat Service (`chat-service/`)
```java
// Chat temps réel haute performance
@MessageMapping("/chat")
public class ChatController {
    // WebSocket avec Spring WebFlux
    // Historique optimisé avec pagination
    // Context management intelligent
    // Support multi-threading pour modèles
}
```

**Fonctionnalités avancées:**
- Messages streaming (Server-Sent Events)
- Context window management automatique
- Conversation branching et merging
- Export formats multiples (JSON, PDF, Markdown)
- Recherche full-text dans l'historique

### 5. Module Media Processor (`media-processor/`)
```java
// Traitement multimédia robuste
public interface MediaProcessorService {
    // Audio: MP3, WAV, AAC, FLAC
    TranscriptionResult transcribeAudio(AudioFile file);
    
    // Vidéo: MP4, WebM, AVI, MOV  
    VideoAnalysis processVideo(VideoFile file);
    
    // Images: JPEG, PNG, WebP, SVG
    ImageMetadata analyzeImage(ImageFile file);
    
    // Conversion et compression automatiques
    MediaFile optimizeMedia(MediaFile input, QualityProfile profile);
}
```

**Intégrations techniques:**
- **Whisper** pour transcription audio
- **OpenCV** pour traitement vidéo/images
- **FFmpeg** pour conversion formats
- **WebRTC** pour streaming temps réel

### 6. Module Frontend (`web-ui/`)
```vue
<!-- Architecture Vue.js 3 production -->
<template>
  <q-layout view="lHh Lpr lFf">
    <!-- Header avec navigation -->
    <AppHeader />
    
    <!-- Sidebar modèles -->
    <ModelSidebar 
      :models="availableModels"
      @model-selected="handleModelChange" 
    />
    
    <!-- Zone chat principale -->
    <ChatInterface 
      :messages="messages"
      :loading="isProcessing"
      @message-sent="sendMessage"
      @media-uploaded="handleMediaUpload"
    />
    
    <!-- Settings et profil -->
    <UserSettings />
  </q-layout>
</template>
```

**Composants requis:**
- **ModelSelector**: Catalogue avec filtres, recherche, favoris
- **ChatInterface**: Messages bubble, markdown, code highlighting
- **MediaUpload**: Drag & drop, preview, progress, validation
- **VoiceInput**: Recording, waveform, real-time transcription
- **SettingsPanel**: Thèmes, préférences, export/import

## Configuration Infrastructure

### Docker Compose (Développement)
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: chatai
      POSTGRES_USER: chatai
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_BROKER_ID: 1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
```

### Kubernetes Production
```yaml
# Namespace avec NetworkPolicies
apiVersion: v1
kind: Namespace
metadata:
  name: chat-ai-prod
  labels:
    name: chat-ai-prod

---
# ConfigMap environnement
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: chat-ai-prod
data:
  SPRING_PROFILES_ACTIVE: "production"
  DATABASE_URL: "postgresql://postgres-service:5432/chatai"
  REDIS_URL: "redis://redis-service:6379"
  KAFKA_BROKERS: "kafka-service:9092"
  LOG_LEVEL: "INFO"
  METRICS_ENABLED: "true"

---
# Secrets gestion
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
data:
  DB_PASSWORD: <base64-encoded>
  JWT_SECRET: <base64-encoded>
  OPENAI_API_KEY: <base64-encoded>
```

## Exigences de Qualité

### Tests (Couverture > 80%)
```java
// Tests unitaires avec JUnit 5 + Mockito
@ExtendWith(MockitoExtension.class)
class ModelManagerServiceTest {
    
    @Test
    @DisplayName("Should return optimal model based on context")
    void shouldReturnOptimalModel() {
        // Given: Context with performance requirements
        // When: Request optimal model
        // Then: Return best matching model
    }
}

// Tests d'intégration avec TestContainers
@SpringBootTest
@Testcontainers
class ChatServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void shouldPersistChatHistory() {
        // Test complet avec vraie base de données
    }
}

// Tests E2E avec Cypress
describe('Chat Flow', () => {
  it('should send message and receive AI response', () => {
    cy.visit('/chat')
    cy.get('[data-cy=message-input]').type('Hello AI')
    cy.get('[data-cy=send-button]').click()
    cy.get('[data-cy=ai-response]').should('be.visible')
  })
})
```

### Monitoring et Observabilité
```java
// Métriques custom avec Micrometer
@Component
public class ChatMetrics {
    private final Counter messagesCounter;
    private final Timer responseTimer;
    private final Gauge activeUsersGauge;
    
    public ChatMetrics(MeterRegistry meterRegistry) {
        this.messagesCounter = Counter.builder("chat.messages.total")
            .description("Total messages processed")
            .tag("model_type", "unknown")
            .register(meterRegistry);
    }
}

// Tracing distribué avec Spring Cloud Sleuth
@RestController
@Slf4j
public class ChatController {
    
    @NewSpan("chat-message-processing")
    public ResponseEntity<ChatResponse> processMessage(
        @SpanTag("model_id") String modelId,
        ChatRequest request
    ) {
        // Processing avec tracing automatique
    }
}
```

### Sécurité Production
```java
// Configuration Security complète
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/chat/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .build();
    }
}

// Validation et sanitization
@RestController
@Validated
public class ChatController {
    
    public ResponseEntity<ChatResponse> sendMessage(
        @Valid @RequestBody ChatMessageDto message,
        @RequestHeader("Authorization") String token
    ) {
        // Validation automatique + sanitization
    }
}
```

## Livrables Attendus

### 1. Code Source Production Ready
- **Projet Maven** multi-modules complet
- **Documentation** technique (README, API docs, architecture)
- **Scripts** de déploiement et migration
- **Tests** automatisés (unit, integration, e2e)

### 2. Infrastructure as Code
- **Kubernetes manifests** pour tous les environnements
- **Helm charts** paramétrés
- **Docker images** optimisées (multi-stage builds)
- **CI/CD pipelines** (GitHub Actions ou GitLab CI)

### 3. Monitoring et Ops
- **Dashboards Grafana** pour métriques métier
- **Alerting rules** Prometheus
- **Log aggregation** avec ELK Stack
- **Documentation** opérationnelle (runbooks)

### 4. Sécurité et Compliance
- **Scan sécurité** automatisé (OWASP ZAP, Snyk)
- **Audit logs** structurés
- **Backup/Restore** procedures
- **Disaster recovery** plan

## Approche de Développement

### Phase 1: Infrastructure et Core (Semaines 1-2)
1. Setup projet Maven avec modules
2. Configuration Spring Boot + Kubernetes
3. Base de données avec migrations Flyway
4. API Gateway avec authentification JWT
5. Tests infrastructure et CI/CD

### Phase 2: Services Backend (Semaines 3-4)  
1. Model Manager avec intégration Ollama
2. Chat Service avec WebSocket
3. Media Processor avec Whisper
4. Tests d'intégration complets
5. Monitoring et métriques

### Phase 3: Frontend et UX (Semaines 5-6)
1. Interface Vue.js responsive
2. Intégration WebSocket temps réel  
3. Upload média avec preview
4. Tests E2E avec Cypress
5. Performance optimization

### Phase 4: Production Hardening (Semaines 7-8)
1. Sécurité renforcée et audit
2. Load testing et optimization
3. Documentation complète
4. Déploiement production
5. Monitoring et alerting

## Critères d'Acceptation

### Performance
- ✅ Temps de réponse API < 200ms (p95)
- ✅ Support 10K utilisateurs concurrents
- ✅ Uptime > 99.9%
- ✅ Démarrage application < 30s

### Sécurité  
- ✅ Scan sécurité OWASP sans vulnérabilités HIGH
- ✅ Chiffrement TLS 1.3 obligatoire
- ✅ Authentification multi-facteurs
- ✅ Audit trail complet

### Qualité Code
- ✅ Couverture tests > 80%
- ✅ Debt technique < 5% (SonarQube)
- ✅ Documentation API complète
- ✅ Standards code respectés (Checkstyle)

## Instructions Finales

Développez cette application en suivant les **best practices Java enterprise**, en privilégiant la **lisibilité du code**, la **testabilité** et la **maintenabilité**. Chaque module doit être **auto-documenté** avec des tests complets et une architecture claire.

**Important**: Cette application sera utilisée en production avec des milliers d'utilisateurs. La qualité, la sécurité et les performances sont critiques. Implémentez tous les patterns enterprise nécessaires (Repository, Service, Factory, Observer, etc.) et assurez-vous que le code soit prêt pour une montée en charge.