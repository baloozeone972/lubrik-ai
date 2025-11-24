# Architecture Technique Détaillée - NexusAI

## 1. Vue d'ensemble

### 1.1 Diagramme d'architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  Web App │  │ Mobile   │  │ Desktop  │  │   API    │  │  Admin   │      │
│  │  React   │  │React Nat.│  │ Electron │  │  Clients │  │  Panel   │      │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘      │
└───────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┘
        │              │              │              │              │
        └──────────────┴──────────────┼──────────────┴──────────────┘
                                      │ HTTPS/WSS
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           LOAD BALANCER                                      │
│                    (HAProxy / AWS ALB / Nginx)                               │
│              - SSL Termination  - Rate Limiting  - Health Checks             │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          API GATEWAY LAYER                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      nexus-api (Spring Boot)                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐             │   │
│  │  │   Auth   │  │Companion │  │  Conver- │  │  Media   │             │   │
│  │  │Controller│  │Controller│  │ sation   │  │Controller│             │   │
│  │  └──────────┘  └──────────┘  │Controller│  └──────────┘             │   │
│  │  ┌──────────┐  ┌──────────┐  └──────────┘  ┌──────────┐             │   │
│  │  │ Payment  │  │Analytics │  ┌──────────┐  │Moderation│             │   │
│  │  │Controller│  │Controller│  │   User   │  │Controller│             │   │
│  │  └──────────┘  └──────────┘  │Controller│  └──────────┘             │   │
│  │                              └──────────┘                            │   │
│  │  ┌──────────────────────────────────────────────────────────────┐   │   │
│  │  │         Security Filters (JWT, Rate Limit, CORS)              │   │   │
│  │  └──────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SERVICE LAYER                                       │
│                                                                              │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐                 │
│  │  nexus-auth    │  │nexus-companion │  │nexus-conversa- │                 │
│  │                │  │                │  │     tion       │                 │
│  │ - JWT Service  │  │ - Companion    │  │ - Message      │                 │
│  │ - User Service │  │   Service      │  │   Service      │                 │
│  │ - Email        │  │ - Personality  │  │ - Context      │                 │
│  │   Verification │  │   Config       │  │   Service      │                 │
│  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘                 │
│          │                   │                   │                          │
│  ┌───────┴────────┐  ┌───────┴────────┐  ┌───────┴────────┐                 │
│  │nexus-ai-engine │  │  nexus-media   │  │nexus-moderation│                 │
│  │                │  │                │  │                │                 │
│  │ - Ollama       │  │ - MinIO        │  │ - Content      │                 │
│  │   Service      │  │   Service      │  │   Filter       │                 │
│  │ - Prompt       │  │ - File         │  │ - GDPR         │                 │
│  │   Management   │  │   Validation   │  │   Service      │                 │
│  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘                 │
│          │                   │                   │                          │
│  ┌───────┴────────┐  ┌───────┴────────┐                                     │
│  │nexus-analytics │  │ nexus-payment  │                                     │
│  │                │  │                │                                     │
│  │ - Event        │  │ - Stripe       │                                     │
│  │   Service      │  │   Integration  │                                     │
│  │ - Metric       │  │ - Subscription │                                     │
│  │   Service      │  │   Management   │                                     │
│  └────────────────┘  └────────────────┘                                     │
│                                                                              │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATA ACCESS LAYER                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      nexus-core (Repositories)                       │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐             │   │
│  │  │   User   │  │Companion │  │Conversa- │  │ Message  │             │   │
│  │  │Repository│  │Repository│  │tion Repo │  │Repository│             │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE LAYER                                 │
│                                                                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │ PostgreSQL │  │   Redis    │  │   Kafka    │  │   MinIO    │            │
│  │    16      │  │     7      │  │  Confluent │  │  (S3-like) │            │
│  │            │  │            │  │            │  │            │            │
│  │ - Users    │  │ - Sessions │  │ - Events   │  │ - Avatars  │            │
│  │ - Compan-  │  │ - Cache    │  │ - Analyt-  │  │ - Media    │            │
│  │   ions     │  │ - Rate     │  │   ics      │  │ - Uploads  │            │
│  │ - Messages │  │   Limits   │  │ - Audit    │  │            │            │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘            │
│                                                                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                            │
│  │   Ollama   │  │ Prometheus │  │  Grafana   │                            │
│  │  (LLM AI)  │  │  (Metrics) │  │  (Dashb.)  │                            │
│  │            │  │            │  │            │                            │
│  │ - Llama3   │  │ - JVM      │  │ - Alerts   │                            │
│  │ - Mistral  │  │ - HTTP     │  │ - Graphs   │                            │
│  │ - Custom   │  │ - Custom   │  │            │                            │
│  └────────────┘  └────────────┘  └────────────┘                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. Modules détaillés

### 2.1 nexus-commons

**Responsabilité**: Utilitaires et classes partagées

```
nexus-commons/
├── exception/
│   ├── BaseException.java        # Exception de base
│   ├── ResourceNotFoundException.java
│   ├── BusinessException.java
│   └── ValidationException.java
├── dto/
│   └── ApiResponse.java          # Réponse API standard
└── util/
    └── DateUtils.java            # Utilitaires dates
```

**Dépendances**: Aucune

### 2.2 nexus-core

**Responsabilité**: Entités JPA et repositories

```
nexus-core/
├── entity/
│   ├── BaseEntity.java           # Audit fields (createdAt, updatedAt)
│   ├── User.java                 # Utilisateur
│   ├── Companion.java            # Compagnon IA
│   ├── Conversation.java         # Conversation
│   └── Message.java              # Message
├── repository/
│   ├── UserRepository.java
│   ├── CompanionRepository.java
│   ├── ConversationRepository.java
│   └── MessageRepository.java
└── enums/
    ├── UserRole.java             # USER, ADMIN, MODERATOR
    ├── SubscriptionType.java     # FREE, STANDARD, PREMIUM, VIP
    ├── CompanionStatus.java      # ACTIVE, INACTIVE, DELETED
    ├── ConversationStatus.java   # ACTIVE, ARCHIVED
    └── MessageRole.java          # USER, ASSISTANT, SYSTEM
```

**Dépendances**: nexus-commons

### 2.3 nexus-auth

**Responsabilité**: Authentification et autorisation

```
nexus-auth/
├── security/
│   ├── JwtService.java           # Génération/validation JWT
│   ├── JwtAuthFilter.java        # Filtre d'authentification
│   ├── SecurityConfig.java       # Configuration Spring Security
│   └── UserPrincipal.java        # Principal utilisateur
├── service/
│   ├── AuthenticationService.java # Login/Register/Refresh
│   ├── UserService.java          # CRUD utilisateurs
│   └── EmailVerificationService.java
└── dto/
    ├── AuthRequest.java
    ├── AuthResponse.java
    └── RegisterRequest.java
```

**Dépendances**: nexus-core

**Flux d'authentification**:
```
Client -> Login Request -> AuthController
                              │
                              ▼
                    AuthenticationService
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    ▼                   ▼
              UserRepository      PasswordEncoder
                    │                   │
                    └─────────┬─────────┘
                              │
                              ▼
                         JwtService
                              │
                              ▼
                    JWT Token Response
```

### 2.4 nexus-companion

**Responsabilité**: Gestion des compagnons IA

```
nexus-companion/
├── service/
│   └── CompanionService.java     # CRUD, limites, visibilité
├── dto/
│   ├── CompanionCreateRequest.java
│   ├── CompanionUpdateRequest.java
│   └── CompanionResponse.java
└── validation/
    └── CompanionValidator.java
```

**Dépendances**: nexus-core, nexus-auth

### 2.5 nexus-conversation

**Responsabilité**: Moteur de conversation

```
nexus-conversation/
├── service/
│   ├── ConversationService.java  # Gestion conversations
│   ├── MessageService.java       # Messages et génération IA
│   └── ContextService.java       # Gestion du contexte
├── dto/
│   ├── ConversationDTO.java
│   ├── MessageDTO.java
│   ├── SendMessageRequest.java
│   └── StreamChunk.java          # SSE streaming
└── websocket/
    └── ConversationWebSocketHandler.java
```

**Dépendances**: nexus-core, nexus-ai-engine, nexus-moderation

**Flux de message**:
```
User Message -> MessageService
                    │
    ┌───────────────┼───────────────┐
    │               │               │
    ▼               ▼               ▼
ContentFilter  ContextService  MessageRepository
    │               │               │
    │               │               │
    └───────────────┼───────────────┘
                    │
                    ▼
            AIProviderService
                    │
    ┌───────────────┴───────────────┐
    │                               │
    ▼                               ▼
OllamaService               (OpenAI future)
    │
    ▼
AI Response -> MessageRepository -> Response
```

### 2.6 nexus-ai-engine

**Responsabilité**: Intégration LLM

```
nexus-ai-engine/
├── service/
│   ├── AIProviderService.java    # Interface provider
│   └── OllamaService.java        # Implémentation Ollama
├── dto/
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   └── OllamaChatRequest.java
└── config/
    └── AIConfig.java
```

**Dépendances**: nexus-core

### 2.7 nexus-media

**Responsabilité**: Stockage fichiers (MinIO)

```
nexus-media/
├── service/
│   └── MediaService.java         # Upload/download
├── config/
│   └── MinioConfig.java          # Configuration MinIO
├── dto/
│   ├── MediaMetadata.java
│   └── MediaUploadResponse.java
└── validation/
    └── FileValidator.java        # Types/tailles autorisés
```

**Dépendances**: nexus-commons, nexus-core

**Types supportés**:
- Images: jpg, jpeg, png, gif, webp (max 10MB)
- Vidéos: mp4, webm, mov (max 50MB)
- Audio: mp3, wav, ogg, m4a (max 20MB)

### 2.8 nexus-moderation

**Responsabilité**: Filtrage contenu et RGPD

```
nexus-moderation/
├── service/
│   ├── ContentFilterService.java # Analyse contenu
│   ├── ModerationService.java    # Actions modération
│   ├── IncidentService.java      # Gestion incidents
│   └── GDPRService.java          # Export/suppression données
└── dto/
    └── FilterResult.java
```

**Dépendances**: nexus-core, nexus-auth

**Patterns bloqués par défaut**:
- Discours haineux
- Contenu illégal
- Self-harm
- Spam (URLs excessives, caps, répétitions)

### 2.9 nexus-analytics

**Responsabilité**: Événements et métriques

```
nexus-analytics/
├── service/
│   ├── EventService.java         # Tracking événements
│   └── MetricService.java        # Agrégation métriques
├── entity/
│   └── AnalyticsEvent.java
├── repository/
│   └── AnalyticsEventRepository.java
└── kafka/
    └── EventProducer.java        # Publication Kafka
```

**Dépendances**: nexus-core

### 2.10 nexus-payment

**Responsabilité**: Paiements Stripe

```
nexus-payment/
├── service/
│   └── PaymentService.java       # Stripe integration
├── dto/
│   ├── CreateSubscriptionRequest.java
│   ├── SubscriptionResponse.java
│   ├── InvoiceDTO.java
│   └── PaymentMethodDTO.java
└── webhook/
    └── StripeWebhookHandler.java
```

**Dépendances**: nexus-core

### 2.11 nexus-api

**Responsabilité**: REST API et OpenAPI

```
nexus-api/
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── CompanionController.java
│   ├── ConversationController.java
│   ├── MediaController.java
│   ├── ModerationController.java
│   ├── AnalyticsController.java
│   └── PaymentController.java
├── security/
│   ├── ratelimit/
│   │   ├── RateLimitService.java
│   │   └── RateLimitAspect.java
│   └── audit/
│       └── AuditAspect.java
└── config/
    └── OpenApiConfig.java
```

**Dépendances**: Tous les modules

## 3. Base de données

### 3.1 Schéma PostgreSQL

```sql
-- Table users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    avatar_url VARCHAR(500),
    role VARCHAR(20) DEFAULT 'USER',
    subscription_type VARCHAR(20) DEFAULT 'FREE',
    tokens_remaining INTEGER DEFAULT 100,
    account_status VARCHAR(20) DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table companions
CREATE TABLE companions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    style VARCHAR(20) DEFAULT 'REALISTIC',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    avatar_url VARCHAR(500),
    personality_traits JSONB,
    appearance_config JSONB,
    voice_config JSONB,
    system_prompt TEXT,
    model_provider VARCHAR(50) DEFAULT 'ollama',
    model_name VARCHAR(100) DEFAULT 'llama3',
    total_messages BIGINT DEFAULT 0,
    total_tokens_used BIGINT DEFAULT 0,
    likes_count INTEGER DEFAULT 0,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table conversations
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    companion_id UUID NOT NULL,
    title VARCHAR(200),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    message_count INTEGER DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    last_activity_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table messages
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    type VARCHAR(20) DEFAULT 'TEXT',
    content TEXT NOT NULL,
    tokens_used INTEGER DEFAULT 0,
    media_url VARCHAR(500),
    media_type VARCHAR(50),
    parent_message_id UUID,
    is_edited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_companions_user ON companions(user_id);
CREATE INDEX idx_companions_status ON companions(status);
CREATE INDEX idx_conversations_user ON conversations(user_id);
CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_created ON messages(created_at DESC);
```

### 3.2 Structure Redis

```
# Sessions utilisateur
session:{userId}:token -> JWT refresh token (TTL: 7 days)

# Cache
user:{userId} -> JSON User object (TTL: 1 hour)
companion:{companionId} -> JSON Companion object (TTL: 1 hour)

# Rate limiting
ratelimit:{userId}:{endpoint} -> counter (TTL: 1 minute)

# Moderation cache
moderation:filter_cache:{hash} -> "true"/"false" (TTL: 1 hour)
moderation:blocked_patterns -> Set of patterns

# Analytics counters
analytics:count:{eventType}:{date} -> counter (TTL: 7 days)
```

### 3.3 Topics Kafka

```
# Événements analytics
analytics-events:
  - partition: 0-5
  - retention: 7 days
  - format: JSON

# Événements d'audit
audit-events:
  - partition: 0-2
  - retention: 30 days
  - format: JSON

# Notifications (future)
notifications:
  - partition: 0-2
  - format: JSON
```

## 4. Sécurité

### 4.1 Authentification JWT

```
Access Token:
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1700000000,
  "exp": 1700086400  // 24h
}

Refresh Token:
{
  "sub": "user-uuid",
  "type": "refresh",
  "iat": 1700000000,
  "exp": 1700604800  // 7 days
}
```

### 4.2 Rate Limiting

| Endpoint | Limite | Fenêtre |
|----------|--------|---------|
| /auth/login | 5 | 1 min |
| /auth/register | 3 | 1 min |
| /conversations/*/generate | 20 | 1 min |
| /media/upload | 10 | 1 min |
| Global API | 100 | 1 min |

### 4.3 Validation des entrées

- Toutes les entrées sont validées via Bean Validation (Jakarta)
- HTML/scripts sont échappés
- SQL injection prévenue par JPA/Hibernate
- XSS prévenu par échappement automatique

## 5. Performance

### 5.1 Caching

```java
// Cache utilisateur
@Cacheable(value = "users", key = "#userId")
public User findById(UUID userId) { ... }

// Cache invalidation
@CacheEvict(value = "users", key = "#user.id")
public User update(User user) { ... }
```

### 5.2 Connection Pooling

```yaml
hikari:
  minimum-idle: 10
  maximum-pool-size: 50
  connection-timeout: 30000
  idle-timeout: 300000
```

### 5.3 Pagination

Toutes les listes utilisent Spring Data Pageable:
```java
Page<Conversation> findByUserId(UUID userId, Pageable pageable);
```

## 6. Scalabilité

### 6.1 Horizontale

- Application stateless (sessions Redis)
- Load balancer avec sticky sessions pour WebSocket
- Kafka pour distribution des événements
- MinIO cluster pour stockage

### 6.2 Verticale

- JVM G1GC optimisé
- Connection pooling adaptatif
- Batch processing pour écritures

## 7. Monitoring

### 7.1 Endpoints Actuator

- `/actuator/health` - État de santé
- `/actuator/metrics` - Métriques JVM/HTTP
- `/actuator/prometheus` - Format Prometheus
- `/actuator/info` - Info application

### 7.2 Métriques custom

```java
@Timed(name = "ai.response.time", description = "AI response generation time")
public String generateResponse(...) { ... }
```
