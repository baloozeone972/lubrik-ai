# Documentation Technique - Application Compagnon Virtuel Commercial

## Vue d'ensemble du projet

Application commerciale de compagnon virtuel avec personnages IA diversifiés, conforme aux régulations internationales. Support multi-plateforme (Web, Mobile, VR) avec système d'abonnement et monétisation.

## Architecture technique globale

### Stack technologique complète (100% licences gratuites)

#### Backend Core
```yaml
Runtime & Framework:
  - Java 21 (OpenJDK) - LTS avec Virtual Threads
  - Spring Boot 3.2+
  - Spring Cloud 2023.x (microservices)
  - Spring Security 6.x (authentification/autorisation)
  - Spring WebFlux (programmation réactive)

Messaging & Events:
  - Apache Kafka 3.6+ (streaming événements)
  - Spring Kafka (intégration)
  - Redis Pub/Sub (temps réel)

Bases de données:
  - PostgreSQL 16+ (données relationnelles)
  - Redis 7+ (cache, sessions, rate limiting)
  - Elasticsearch 8.11+ (recherche, analytics)
  - MongoDB 7+ (historique conversationnel)

API & Communication:
  - gRPC (communication inter-services)
  - REST (API publique)
  - WebSocket (temps réel)
  - WebRTC (vidéo/audio)
```

#### Frontend Multi-plateforme
```yaml
Web:
  - React 18+ avec TypeScript 5+
  - Material-UI ou Tailwind CSS 3+
  - Redux Toolkit (state management)
  - React Query (data fetching)
  - Socket.io-client (temps réel)

Mobile:
  - React Native 0.73+ ou Flutter 3.16+
  - Expo SDK 50+ (déploiement simplifié)
  - Support iOS & Android
  - Notifications push natives

VR (Phase 3):
  - jMonkeyEngine 3.6+ (moteur 3D Java)
  - LWJGL OpenXR (standard VR)
  - Support Quest 2/3, PCVR
```

#### Infrastructure & DevOps
```yaml
Conteneurisation:
  - Docker 24+
  - Kubernetes 1.29+
  - Helm Charts (déploiement)

Reverse Proxy & Load Balancing:
  - NGINX 1.25+ (proxy inverse, SSL termination)
  - HAProxy (alternative)

Monitoring & Observability:
  - Prometheus (métriques)
  - Grafana (visualisation)
  - ELK Stack (Elasticsearch, Logstash, Kibana)
  - Jaeger (distributed tracing)

CI/CD:
  - Jenkins ou GitLab CI/CD
  - ArgoCD (GitOps)
  - SonarQube (qualité code)
```

#### IA & Machine Learning
```yaml
Frameworks ML Java:
  - DJL (Deep Java Library) 0.28+
  - DL4J (DeepLearning4J) 1.0.0-M2+
  - ONNX Runtime Java 1.16+

Modèles LLM locaux:
  - Llama 2 (7B/13B) via DJL
  - Mistral 7B via ONNX
  - GPT-J 6B (alternative légère)

NLP & Audio:
  - Apache OpenNLP 2.3+
  - Stanford CoreNLP 4.5+
  - Whisper (transcription vocale)
  - Coqui TTS (synthèse vocale)

Génération Vidéo:
  - JavaCV 1.5+ (wrapper FFmpeg)
  - jMonkeyEngine (rendu 3D)
  - Blender Python API (génération assets)
```

## Architecture Microservices

### Services principaux

#### 1. User Service
```java
Responsabilités:
  - Gestion comptes utilisateurs
  - Vérification d'âge/identité multi-niveaux
  - Préférences et paramètres
  - Profils utilisateur
  - Gestion consentements GDPR

Technologies:
  - Spring Boot + Spring Security
  - PostgreSQL (données utilisateur)
  - Redis (sessions)
  - JWT + OAuth2

Endpoints principaux:
  POST   /api/v1/users/register
  POST   /api/v1/users/login
  POST   /api/v1/users/verify-age
  GET    /api/v1/users/profile
  PATCH  /api/v1/users/preferences
  DELETE /api/v1/users/account
```

**Système de vérification d'âge**
```java
public enum AgeVerificationLevel {
    BASIC(13),      // Déclaration simple
    EMAIL(16),      // Email + téléphone vérifié
    DOCUMENT(18),   // Pièce d'identité
    BANKING(21);    // Vérification bancaire
    
    private final int minimumAge;
}

@Service
public class AgeVerificationService {
    
    public VerificationResult verifyAge(User user, 
                                       VerificationLevel level,
                                       VerificationData data) {
        // Niveau 1: Simple déclaration
        if (level == BASIC) {
            return verifyBirthdate(data.getBirthdate());
        }
        
        // Niveau 2: Email + SMS
        if (level == EMAIL) {
            verifyEmail(user.getEmail());
            verifySMS(user.getPhone());
        }
        
        // Niveau 3: Document d'identité (intégration API tierce)
        if (level == DOCUMENT) {
            return identityVerificationAPI.verify(data.getDocument());
        }
        
        // Niveau 4: Vérification bancaire
        if (level == BANKING) {
            return bankVerificationAPI.microDeposit(user.getBankAccount());
        }
    }
}
```

#### 2. Character Service
```java
Responsabilités:
  - CRUD personnages virtuels
  - Moteur de personnalités (Big Five traits)
  - Système d'apparence paramétrable
  - Configuration voix et comportement
  - Diversité culturelle/ethnique
  - Évolution adaptative

Technologies:
  - Spring Boot + JPA
  - PostgreSQL (données personnages)
  - Elasticsearch (recherche)
  - ML models (personnalité)

Endpoints:
  GET    /api/v1/characters (liste, filtres)
  GET    /api/v1/characters/{id}
  POST   /api/v1/characters (création custom)
  PATCH  /api/v1/characters/{id}/personality
  PATCH  /api/v1/characters/{id}/appearance
  GET    /api/v1/characters/recommended (IA)
```

**Modèle de personnalité**
```java
@Entity
public class VirtualCharacter {
    @Id
    private UUID id;
    private String name;
    private String description;
    
    @Embedded
    private PersonalityTraits personality;
    
    @Embedded
    private AppearanceConfig appearance;
    
    @Embedded
    private VoiceConfig voice;
    
    @Enumerated(EnumType.STRING)
    private Ethnicity ethnicity;
    
    @Enumerated(EnumType.STRING)
    private CulturalBackground culture;
    
    private LocalDateTime createdAt;
}

@Embeddable
public class PersonalityTraits {
    // Big Five Model (OCEAN)
    private Float openness;        // 0.0 - 1.0
    private Float conscientiousness;
    private Float extraversion;
    private Float agreeableness;
    private Float neuroticism;
    
    // Traits additionnels
    private Float humor;
    private Float empathy;
    private Float playfulness;
    private Float intelligence;
    
    // Styles conversationnels
    @ElementCollection
    private List<ConversationStyle> styles;
}
```

#### 3. Conversation Service
```java
Responsabilités:
  - Historique conversations
  - Moteur de dialogue (LLM local)
  - Analyse sentiment temps réel
  - Mémoire conversationnelle
  - Contexte multi-sessions
  - Suggestions intelligentes

Technologies:
  - Spring Boot + WebFlux (réactivité)
  - MongoDB (stockage conversations)
  - Redis (cache contexte)
  - DJL + Llama 2 (génération)
  - Kafka (événements)

Endpoints:
  POST   /api/v1/conversations/start
  POST   /api/v1/conversations/{id}/message
  GET    /api/v1/conversations/{id}/history
  GET    /api/v1/conversations/active
  DELETE /api/v1/conversations/{id}
  
WebSocket:
  ws://domain/ws/conversation/{conversationId}
```

**Moteur de dialogue IA**
```java
@Service
public class DialogueEngine {
    
    private final DJLPredictor llmPredictor;
    private final ConversationMemory memory;
    private final ContentModerator moderator;
    
    public Mono<AIResponse> generateResponse(
            ConversationContext context,
            UserMessage message,
            VirtualCharacter character) {
        
        // 1. Modération entrée utilisateur
        if (!moderator.isAppropriate(message)) {
            return Mono.just(createModerationResponse());
        }
        
        // 2. Récupération mémoire conversationnelle
        List<Message> history = memory.getRecentHistory(
            context.getId(), 10);
        
        // 3. Construction prompt avec personnalité
        String prompt = buildPrompt(character, history, message);
        
        // 4. Génération avec LLM
        String aiResponse = llmPredictor.predict(prompt);
        
        // 5. Post-traitement et cohérence
        aiResponse = ensurePersonalityConsistency(
            aiResponse, character);
        
        // 6. Analyse sentiment
        SentimentScore sentiment = analyzeSentiment(aiResponse);
        
        // 7. Sauvegarde et événement Kafka
        return saveAndPublish(context, aiResponse, sentiment);
    }
    
    private String buildPrompt(VirtualCharacter character,
                               List<Message> history,
                               UserMessage message) {
        return String.format("""
            You are %s, a virtual companion with these traits:
            - Personality: %s
            - Background: %s
            - Speaking style: %s
            
            Recent conversation:
            %s
            
            User just said: "%s"
            
            Respond naturally in character, keeping your personality 
            consistent. Be empathetic, engaging, and appropriate.
            """,
            character.getName(),
            character.getPersonality().toString(),
            character.getBackground(),
            character.getSpeakingStyle(),
            formatHistory(history),
            message.getContent()
        );
    }
}
```

#### 4. Media Service
```java
Responsabilités:
  - Génération vidéo temps réel (avatars 3D)
  - Streaming audio/vidéo WebRTC
  - Lip-sync intelligent
  - Compression adaptative
  - Stockage média sécurisé (chiffré)
  - Enregistrement optionnel

Technologies:
  - Spring Boot + WebFlux
  - JavaCV + FFmpeg (encoding)
  - jMonkeyEngine (rendu 3D)
  - Kurento Media Server (WebRTC)
  - MinIO (stockage S3-compatible)

Endpoints:
  POST   /api/v1/media/video/start
  POST   /api/v1/media/audio/synthesize
  GET    /api/v1/media/stream/{sessionId}
  POST   /api/v1/media/recording/start
  GET    /api/v1/media/assets/{characterId}
```

**Architecture streaming vidéo**
```java
@Service
public class VideoStreamingService {
    
    private final JMonkeyEngineRenderer renderer;
    private final LipSyncEngine lipSync;
    private final FFmpegEncoder encoder;
    
    public void startVideoStream(StreamSession session) {
        // 1. Initialiser avatar 3D
        Avatar3D avatar = renderer.loadAvatar(
            session.getCharacterId());
        
        // 2. Pipeline de rendu temps réel
        Flux.interval(Duration.ofMillis(33)) // 30 FPS
            .flatMap(tick -> {
                // Récupérer audio de conversation
                AudioFrame audio = session.getCurrentAudio();
                
                // Synchronisation labiale
                BlendShapes lipShapes = lipSync.analyze(audio);
                avatar.applyFacialAnimation(lipShapes);
                
                // Rendu frame
                BufferedImage frame = renderer.render(avatar);
                
                // Encoding H.264
                byte[] encoded = encoder.encode(frame);
                
                // Streaming WebRTC
                return webrtc.sendFrame(session, encoded);
            })
            .subscribe();
    }
}
```

#### 5. Billing Service
```java
Responsabilités:
  - Gestion abonnements multi-niveaux
  - Intégration Stripe (paiements)
  - Facturation automatique
  - Webhooks paiement
  - Gestion essais gratuits
  - Rapports financiers

Technologies:
  - Spring Boot
  - PostgreSQL (transactions)
  - Stripe Java SDK 24+
  - Quartz Scheduler (tâches récurrentes)

Endpoints:
  POST   /api/v1/billing/subscribe
  POST   /api/v1/billing/upgrade
  POST   /api/v1/billing/cancel
  GET    /api/v1/billing/invoice/{id}
  POST   /api/v1/webhooks/stripe
```

**Système d'abonnements**
```java
public enum SubscriptionTier {
    FREE(0.0, new Features(
        textMessagesPerDay: 10,
        customCharacters: 0,
        voiceChat: false,
        videoChat: false,
        premiumContent: false,
        historyDays: 7
    )),
    
    STANDARD(9.99, new Features(
        textMessagesPerDay: -1, // illimité
        customCharacters: 3,
        voiceChat: true,
        videoChat: false,
        premiumContent: false,
        historyDays: 30
    )),
    
    PREMIUM(19.99, new Features(
        textMessagesPerDay: -1,
        customCharacters: 10,
        voiceChat: true,
        videoChat: true,
        premiumContent: true,
        historyDays: -1
    )),
    
    VIP(39.99, new Features(
        textMessagesPerDay: -1,
        customCharacters: -1, // illimité
        voiceChat: true,
        videoChat: true,
        premiumContent: true,
        historyDays: -1,
        exclusiveCharacters: true,
        apiAccess: true,
        prioritySupport: true
    ));
    
    private final Double monthlyPrice;
    private final Features features;
}

@Service
public class SubscriptionService {
    
    @Autowired
    private StripeClient stripeClient;
    
    public Subscription subscribe(User user, 
                                   SubscriptionTier tier) {
        // Créer Stripe Customer si nouveau
        if (user.getStripeCustomerId() == null) {
            Customer customer = stripeClient.createCustomer(user);
            user.setStripeCustomerId(customer.getId());
        }
        
        // Créer abonnement Stripe
        SubscriptionCreateParams params = 
            SubscriptionCreateParams.builder()
                .setCustomer(user.getStripeCustomerId())
                .addItem(
                    SubscriptionCreateParams.Item.builder()
                        .setPrice(tier.getStripePriceId())
                        .build()
                )
                .setTrialPeriodDays(7L)
                .build();
        
        com.stripe.model.Subscription stripeSubscription = 
            stripeClient.createSubscription(params);
        
        // Sauvegarder localement
        Subscription subscription = new Subscription();
        subscription.setUserId(user.getId());
        subscription.setTier(tier);
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setExpiresAt(calculateExpiry());
        
        return subscriptionRepository.save(subscription);
    }
}
```

#### 6. Content Moderation Service
```java
Responsabilités:
  - Filtrage contenu inapproprié (texte/image)
  - Classification âge automatique
  - Détection comportements abusifs
  - Escalade modération humaine
  - Reporting utilisateur
  - Compliance régionale

Technologies:
  - Spring Boot
  - DJL + modèles ML (classification)
  - PostgreSQL (incidents)
  - Kafka (alertes)

Endpoints:
  POST   /api/v1/moderation/check-text
  POST   /api/v1/moderation/check-image
  POST   /api/v1/moderation/report
  GET    /api/v1/moderation/queue (admin)
```

**Système de modération automatique**
```java
@Service
public class ContentModerationService {
    
    private final TextClassifier textClassifier;
    private final ImageClassifier imageClassifier;
    private final List<String> blacklist;
    
    public ModerationResult moderateText(String content) {
        ModerationResult result = new ModerationResult();
        
        // 1. Blacklist rapide
        if (containsBlacklistedTerms(content)) {
            result.setBlocked(true);
            result.setReason("Blacklisted content");
            return result;
        }
        
        // 2. ML classification
        ContentCategory category = textClassifier.classify(content);
        Float confidence = category.getConfidence();
        
        // 3. Détection toxicité
        ToxicityScore toxicity = analyzeToxicity(content);
        
        // 4. Décision finale
        if (category == INAPPROPRIATE && confidence > 0.8) {
            result.setBlocked(true);
            result.setCategory(category);
        } else if (toxicity.getScore() > 0.7) {
            result.setBlocked(true);
            result.setReason("High toxicity detected");
        } else if (confidence > 0.5 && confidence < 0.8) {
            // Escalade vers modération humaine
            escalateToHumanReview(content);
            result.setNeedsReview(true);
        }
        
        // 5. Logging et analytics
        logModerationEvent(content, result);
        
        return result;
    }
    
    public ModerationResult moderateImage(byte[] imageData) {
        // Détection NSFW
        NSFWScore nsfw = imageClassifier.detectNSFW(imageData);
        
        // Détection mineurs
        boolean containsMinor = imageClassifier.detectMinor(imageData);
        
        ModerationResult result = new ModerationResult();
        
        if (nsfw.getScore() > 0.7 || containsMinor) {
            result.setBlocked(true);
            // Alerte immédiate si mineur détecté
            if (containsMinor) {
                alertLegalTeam(imageData);
            }
        }
        
        return result;
    }
}
```

#### 7. Gamification Service
```java
Responsabilités:
  - Système de points/XP
  - Achievements/déblocages
  - Défis quotidiens/hebdomadaires
  - Événements saisonniers
  - Leaderboards
  - Récompenses

Technologies:
  - Spring Boot
  - Redis (leaderboards, cache)
  - PostgreSQL (progression)
  - Kafka (événements gamification)

Endpoints:
  GET    /api/v1/gamification/progress
  POST   /api/v1/gamification/claim-reward
  GET    /api/v1/gamification/challenges
  GET    /api/v1/gamification/leaderboard
```

### Communication inter-services

**Apache Kafka Topics**
```yaml
Topics principaux:
  user-events:
    - user.registered
    - user.verified
    - user.subscription.changed
    
  conversation-events:
    - conversation.started
    - conversation.message.sent
    - conversation.ended
    
  moderation-events:
    - content.flagged
    - content.blocked
    - user.reported
    
  billing-events:
    - payment.succeeded
    - payment.failed
    - subscription.renewed
    - subscription.cancelled
```

**Architecture événementielle**
```java
@Service
public class EventPublisher {
    
    @Autowired
    private KafkaTemplate<String, Event> kafkaTemplate;
    
    public void publishUserRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(
            user.getId(),
            user.getEmail(),
            user.getRegistrationDate()
        );
        
        kafkaTemplate.send("user-events", 
                          "user.registered", 
                          event);
    }
}

@Service
public class EventConsumer {
    
    @KafkaListener(topics = "user-events", 
                   groupId = "gamification-service")
    public void handleUserEvent(UserEvent event) {
        if (event.getType() == "user.registered") {
            // Créer progression gamification
            gamificationService.initializeUser(event.getUserId());
        }
    }
}
```

## Schéma de données complet

### PostgreSQL - Base principale

```sql
-- ============================================
-- USERS & AUTH
-- ============================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    
    -- Vérification d'âge
    birthdate DATE NOT NULL,
    age_verified BOOLEAN DEFAULT FALSE,
    verification_level VARCHAR(20) DEFAULT 'BASIC',
    verified_at TIMESTAMP,
    
    -- Abonnement
    subscription_tier VARCHAR(20) DEFAULT 'FREE',
    stripe_customer_id VARCHAR(100),
    
    -- Localisation
    country VARCHAR(2), -- ISO code
    timezone VARCHAR(50),
    language VARCHAR(5) DEFAULT 'en',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    
    -- Soft delete
    deleted_at TIMESTAMP
);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    
    -- Filtres contenu
    content_filter_level VARCHAR(20) DEFAULT 'MODERATE',
    nsfw_enabled BOOLEAN DEFAULT FALSE,
    
    -- Interactions
    voice_enabled BOOLEAN DEFAULT TRUE,
    video_enabled BOOLEAN DEFAULT FALSE,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    
    -- Préférences personnages
    preferred_genders TEXT[], -- array
    preferred_personalities JSONB,
    
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE legal_compliance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    
    jurisdiction VARCHAR(50) NOT NULL, -- US-CA, EU-FR, etc.
    consent_version VARCHAR(10) NOT NULL,
    consent_given_at TIMESTAMP NOT NULL,
    
    -- GDPR
    gdpr_consent BOOLEAN DEFAULT FALSE,
    marketing_consent BOOLEAN DEFAULT FALSE,
    data_sharing_consent BOOLEAN DEFAULT FALSE,
    
    -- Vérifications légales
    identity_verified BOOLEAN DEFAULT FALSE,
    identity_document_type VARCHAR(50),
    identity_verified_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CHARACTERS
-- ============================================

CREATE TABLE virtual_characters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Ownership
    creator_id UUID REFERENCES users(id),
    is_public BOOLEAN DEFAULT TRUE,
    is_premium BOOLEAN DEFAULT FALSE,
    
    -- Personnalité (OCEAN model)
    openness FLOAT CHECK (openness BETWEEN 0 AND 1),
    conscientiousness FLOAT CHECK (conscientiousness BETWEEN 0 AND 1),
    extraversion FLOAT CHECK (extraversion BETWEEN 0 AND 1),
    agreeableness FLOAT CHECK (agreeableness BETWEEN 0 AND 1),
    neuroticism FLOAT CHECK (neuroticism BETWEEN 0 AND 1),
    
    -- Traits additionnels
    humor FLOAT CHECK (humor BETWEEN 0 AND 1),
    empathy FLOAT CHECK (empathy BETWEEN 0 AND 1),
    playfulness FLOAT CHECK (playfulness BETWEEN 0 AND 1),
    
    -- Apparence
    appearance_config JSONB NOT NULL, -- JSON avec paramètres 3D
    ethnicity VARCHAR(50),
    cultural_background VARCHAR(50),
    
    -- Voix
    voice_config JSONB NOT NULL,
    voice_model_id VARCHAR(100),
    
    -- Métadonnées
    tags TEXT[],
    age_rating VARCHAR(20) DEFAULT 'PG13',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_characters_public ON virtual_characters(is_public);
CREATE INDEX idx_characters_premium ON virtual_characters(is_premium);
CREATE INDEX idx_characters_creator ON virtual_characters(creator_id);

-- ============================================
-- SUBSCRIPTIONS
-- ============================================

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    
    tier VARCHAR(20) NOT NULL, -- FREE, STANDARD, PREMIUM, VIP
    status VARCHAR(20) NOT NULL, -- ACTIVE, CANCELLED, EXPIRED, SUSPENDED
    
    -- Stripe
    stripe_subscription_id VARCHAR(100) UNIQUE,
    stripe_price_id VARCHAR(100),
    
    -- Dates
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    trial_ends_at TIMESTAMP,
    
    -- Facturation
    amount DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',
    billing_period VARCHAR(20), -- MONTHLY, YEARLY
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID REFERENCES subscriptions(id),
    user_id UUID REFERENCES users(id),
    
    stripe_invoice_id VARCHAR(100) UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL, -- PAID, PENDING, FAILED
    
    invoice_date DATE NOT NULL,
    due_date DATE,
    paid_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CONVERSATIONS
-- ============================================

CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    character_id UUID REFERENCES virtual_characters(id),
    
    title VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, ENDED, ARCHIVED
    
    -- Métriques
    message_count INT DEFAULT 0,
    total_duration_seconds INT DEFAULT 0,
    
    -- Dates
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    last_message_at TIMESTAMP
);

CREATE INDEX idx_conversations_user ON conversations(user_id);
CREATE INDEX idx_conversations_character ON conversations(character_id);
CREATE INDEX idx_conversations_status ON conversations(status);

-- Messages stockés dans MongoDB pour performance
-- Référence uniquement conversation_id

-- ============================================
-- MODERATION
-- ============================================

CREATE TABLE moderation_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    content_type VARCHAR(20) NOT NULL, -- TEXT, IMAGE, VIDEO
    content_hash VARCHAR(64), -- SHA-256
    
    reported_by UUID REFERENCES users(id),
    user_id UUID REFERENCES users(id), -- utilisateur concerné
    conversation_id UUID REFERENCES conversations(id),
    
    -- Classification
    category VARCHAR(50) NOT NULL, -- INAPPROPRIATE, HARASSMENT, ILLEGAL, etc.
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    confidence FLOAT,
    
    -- Statut
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, REVIEWED, RESOLVED
    automated BOOLEAN DEFAULT TRUE,
    
    -- Modération humaine
    reviewed_by UUID REFERENCES users(id), -- moderator
    reviewed_at TIMESTAMP,
    action_taken VARCHAR(100),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_moderation_status ON moderation_incidents(status);
CREATE INDEX idx_moderation_severity ON moderation_incidents(severity);

CREATE TABLE user_warnings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    incident_id UUID REFERENCES moderation_incidents(id),
    
    warning_type VARCHAR(50) NOT NULL,
    description TEXT,
    expires_at TIMESTAMP,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- GAMIFICATION
-- ============================================

CREATE TABLE user_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    
    level INT DEFAULT 1,
    experience_points INT DEFAULT 0,
    
    total_conversations INT DEFAULT 0,
    total_messages_sent INT DEFAULT 0,
    total_time_spent_seconds BIGINT DEFAULT 0,
    
    streak_days INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    last_activity_date DATE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    
    category VARCHAR(50), -- SOCIAL, ENGAGEMENT, MILESTONE
    rarity VARCHAR(20), -- COMMON, RARE, EPIC, LEGENDARY
    
    reward_xp INT DEFAULT 0,
    reward_items JSONB,
    
    requirements JSONB NOT NULL, -- conditions pour débloquer
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    achievement_id UUID REFERENCES achievements(id),
    
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, achievement_id)
);

-- ============================================
-- VR SESSIONS
-- ============================================

CREATE TABLE vr_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES conversations(id),
    
    device_type VARCHAR(50), -- QUEST_2, QUEST_3, PCVR, etc.
    
    -- Métriques qualité
    average_fps FLOAT,
    average_latency_ms INT,
    dropped_frames INT,
    
    -- Durée
    duration_seconds INT,
    
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP
);

-- ============================================
-- ANALYTICS
-- ============================================

CREATE TABLE user_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    event_date DATE NOT NULL,
    
    -- Métriques quotidiennes
    sessions_count INT DEFAULT 0,
    messages_sent INT DEFAULT 0,
    time_spent_seconds INT DEFAULT 0,
    
    -- Engagement
    characters_interacted TEXT[],
    features_used JSONB,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, event_date)
);

-- ============================================
-- INDEXES & CONSTRAINTS
-- ============================================

-- Performance indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_subscription ON users(subscription_tier);
CREATE INDEX idx_users_country ON users(country);

-- Composite indexes
CREATE INDEX idx_conversations_user_status 
    ON conversations(user_id, status);
CREATE INDEX idx_conversations_recent 
    ON conversations(user_id, last_message_at DESC);
```

### MongoDB - Conversations & Messages

```javascript
// Collection: messages
{
  _id: ObjectId(),
  conversation_id: UUID,
  
  sender_type: "user" | "ai",
  content: String,
  content_type: "text" | "voice" | "video",
  
  // Métadonnées
  sentiment_score: Float, // -1.0 to 1.0
  toxicity_score: Float,
  moderated: Boolean,
  
  // Audio/Vidéo
  media_url: String,
  media_duration_seconds: Int,
  
  // Timestamps
  created_at: ISODate(),
  
  // Indexes
  indexes: [
    { conversation_id: 1, created_at: -1 },
    { conversation_id: 1, sender_type: 1 }
  ]
}

// Collection: conversation_memory
{
  _id: ObjectId(),
  conversation_id: UUID,
  user_id: UUID,
  character_id: UUID,
  
  // Mémoire à long terme
  key_facts: [String], // Facts appris sur l'utilisateur
  preferences: Object,
  important_moments: [
    {
      summary: String,
      timestamp: ISODate(),
      emotional_weight: Float
    }
  ],
  
  // Embeddings pour recherche sémantique
  conversation_embedding: [Float], // Vector 768D
  
  updated_at: ISODate()
}
```

### Redis - Cache & Sessions

```
// Sessions utilisateur
user:session:{userId} -> {
  token: JWT,
  expires_at: timestamp,
  device_info: Object,
  location: String
}
TTL: 24 hours

// Cache personnages
character:details:{characterId} -> JSON
TTL: 1 hour

// Rate limiting
ratelimit:api:{userId}:{endpoint} -> count
TTL: selon endpoint

// Leaderboards
leaderboard:xp -> Sorted Set (userId, score)
leaderboard:messages -> Sorted Set
leaderboard:streak -> Sorted Set

// Active conversations
active:conversations:{userId} -> Set of conversation IDs

// WebSocket sessions
ws:session:{sessionId} -> {
  userId: UUID,
  conversationId: UUID,
  connected_at: timestamp
}
```

### Elasticsearch - Recherche & Analytics

```json
// Index: characters
{
  "mappings": {
    "properties": {
      "id": {"type": "keyword"},
      "name": {"type": "text"},
      "description": {"type": "text"},
      "tags": {"type": "keyword"},
      "personality_traits": {"type": "object"},
      "age_rating": {"type": "keyword"},
      "popularity_score": {"type": "float"},
      "created_at": {"type": "date"}
    }
  }
}

// Index: user_analytics_daily
{
  "mappings": {
    "properties": {
      "user_id": {"type": "keyword"},
      "date": {"type": "date"},
      "sessions_count": {"type": "integer"},
      "messages_sent": {"type": "integer"},
      "time_spent_seconds": {"type": "integer"},
      "subscription_tier": {"type": "keyword"},
      "retention_day": {"type": "integer"}
    }
  }
}
```

## Conformité légale et sécurité

### Système de vérification d'âge multi-niveaux

**Implémentation complète**

```java
@Service
public class ComplianceService {
    
    // Niveau 1: Déclaration (13+)
    public VerificationResult verifyBasic(User user, LocalDate birthdate) {
        int age = Period.between(birthdate, LocalDate.now()).getYears();
        
        if (age < 13) {
            throw new UnderageException("Minimum age requirement not met");
        }
        
        user.setBirthdate(birthdate);
        user.setVerificationLevel(AgeVerificationLevel.BASIC);
        user.setAgeVerified(true);
        
        // Restrictions: Pas de contenu mature
        applyContentRestrictions(user, ContentRating.GENERAL);
        
        return VerificationResult.success(AgeVerificationLevel.BASIC);
    }
    
    // Niveau 2: Email + Téléphone (16+)
    public VerificationResult verifyEmail(User user) {
        // Envoi code vérification email
        emailService.sendVerificationCode(user.getEmail());
        
        // Envoi SMS
        smsService.sendVerificationCode(user.getPhone());
        
        // Attente confirmation...
        
        user.setVerificationLevel(AgeVerificationLevel.EMAIL);
        applyContentRestrictions(user, ContentRating.TEEN);
        
        return VerificationResult.success(AgeVerificationLevel.EMAIL);
    }
    
    // Niveau 3: Document d'identité (18+)
    public VerificationResult verifyDocument(User user, 
                                            MultipartFile idDocument) {
        // Intégration API tierce (ex: Jumio, Onfido)
        IdentityVerificationResult result = 
            identityAPI.verifyDocument(idDocument);
        
        if (!result.isSuccess()) {
            throw new VerificationFailedException(result.getReason());
        }
        
        // Vérifier âge extrait
        if (result.getAge() < 18) {
            throw new UnderageException("Must be 18+ for this tier");
        }
        
        LegalCompliance compliance = new LegalCompliance();
        compliance.setUserId(user.getId());
        compliance.setIdentityVerified(true);
        compliance.setIdentityDocumentType(result.getDocumentType());
        compliance.setIdentityVerifiedAt(LocalDateTime.now());
        complianceRepository.save(compliance);
        
        user.setVerificationLevel(AgeVerificationLevel.DOCUMENT);
        applyContentRestrictions(user, ContentRating.MATURE);
        
        return VerificationResult.success(AgeVerificationLevel.DOCUMENT);
    }
    
    // Niveau 4: Vérification bancaire (21+)
    public VerificationResult verifyBanking(User user, 
                                           BankAccount account) {
        // Micro-dépôt pour vérification
        BankVerificationResult result = 
            bankAPI.initiateMicroDeposit(account);
        
        // L'utilisateur devra confirmer montants reçus
        // Process asynchrone...
        
        return VerificationResult.pending(AgeVerificationLevel.BANKING);
    }
}
```

### Adaptation géographique (Geo-compliance)

```java
@Service
public class GeoComplianceService {
    
    private final Map<String, ComplianceRules> rules = Map.of(
        "US", new USComplianceRules(),
        "EU", new EUComplianceRules(),
        "UK", new UKComplianceRules(),
        "AU", new AustraliaComplianceRules(),
        "JP", new JapanComplianceRules()
    );
    
    public ComplianceCheck checkCompliance(User user, String content) {
        String jurisdiction = detectJurisdiction(user);
        ComplianceRules rule = rules.get(jurisdiction);
        
        ComplianceCheck check = new ComplianceCheck();
        
        // Vérifications spécifiques
        check.setAgeRequirementMet(
            rule.meetsAgeRequirement(user.getBirthdate()));
        check.setContentAllowed(
            rule.isContentAllowed(content, user.getVerificationLevel()));
        check.setFeatureAllowed(
            rule.isFeatureAllowed(FeatureType.VIDEO_CHAT, user));
        
        // Restrictions locales
        if (jurisdiction.equals("DE")) {
            // Allemagne: Restrictions strictes sur contenu
            check.setContentRestrictionsApplied(true);
        }
        
        if (jurisdiction.equals("CN")) {
            // Chine: Service non disponible
            check.setServiceBlocked(true);
            check.setBlockReason("Service not available in this region");
        }
        
        return check;
    }
    
    private String detectJurisdiction(User user) {
        // 1. Country déclaré par utilisateur
        if (user.getCountry() != null) {
            return user.getCountry();
        }
        
        // 2. IP geolocation
        String ip = requestContext.getClientIP();
        return geoIPService.getCountry(ip);
    }
}

// Règles spécifiques USA
public class USComplianceRules implements ComplianceRules {
    
    @Override
    public boolean isContentAllowed(String content, 
                                    AgeVerificationLevel level) {
        // COPPA: < 13 ans interdit
        // Section 230: Responsabilité limitée plateforme
        // State laws (CA, TX, etc.)
        
        if (level == AgeVerificationLevel.BASIC) {
            return contentRating.isGeneralAudience(content);
        }
        
        return true;
    }
    
    @Override
    public boolean requiresParentalConsent(int age) {
        return age < 18; // Varie par état
    }
}

// Règles GDPR (Europe)
public class EUComplianceRules implements ComplianceRules {
    
    @Override
    public boolean meetsDataProtectionRequirements(User user) {
        LegalCompliance compliance = 
            complianceService.getCompliance(user.getId());
        
        // GDPR requirements
        return compliance.getGdprConsent() &&
               compliance.getConsentVersion().equals(CURRENT_VERSION) &&
               hasRightToErasure(user) &&
               hasDataPortability(user);
    }
    
    @Override
    public void enforceDataRetention(User user) {
        // Max 30 jours sans activité pour données non essentielles
        if (user.getLastLoginAt().isBefore(
                LocalDateTime.now().minusDays(30))) {
            archiveNonEssentialData(user);
        }
    }
}
```

### Modération de contenu renforcée

```java
@Service
public class AdvancedModerationService {
    
    @Autowired
    private DJLPredictor nsfw Classifier;
    
    @Autowired
    private ToxicityDetector toxicityDetector;
    
    public ModerationDecision moderate(UserInput input) {
        ModerationDecision decision = new ModerationDecision();
        
        // 1. Pre-filters rapides
        if (containsBlacklistedPatterns(input.getContent())) {
            decision.setAction(ModerationAction.BLOCK);
            decision.setReason("Blacklisted content detected");
            logIncident(input, decision, Severity.HIGH);
            return decision;
        }
        
        // 2. ML Classification
        Classification classification = 
            nsfwClassifier.classify(input.getContent());
        
        if (classification.getCategory() == Category.NSFW) {
            // Vérifier si utilisateur autorisé
            if (!user.canAccessNSFW()) {
                decision.setAction(ModerationAction.BLOCK);
                decision.setReason("NSFW content blocked");
                return decision;
            }
        }
        
        // 3. Toxicity scoring
        ToxicityScore toxicity = 
            toxicityDetector.analyze(input.getContent());
        
        if (toxicity.getScore() > 0.8) {
            decision.setAction(ModerationAction.BLOCK);
            decision.setReason("High toxicity");
            issueWarning(input.getUserId());
        } else if (toxicity.getScore() > 0.6) {
            decision.setAction(ModerationAction.WARN);
            decision.setWarningMessage(
                "Please keep interactions respectful");
        }
        
        // 4. Pattern detection (grooming, harassment)
        PatternMatch pattern = detectHarmfulPatterns(
            input.getUserId(), 
            input.getContent());
        
        if (pattern.isMatch()) {
            decision.setAction(ModerationAction.ESCALATE);
            escalateToHumanReview(input, pattern);
        }
        
        // 5. Legal compliance check
        if (violatesLocalLaws(input, user.getJurisdiction())) {
            decision.setAction(ModerationAction.BLOCK);
            decision.setReason("Content violates local regulations");
            alertLegalTeam(input);
        }
        
        logModerationDecision(input, decision);
        return decision;
    }
    
    private void escalateToHumanReview(UserInput input, 
                                       PatternMatch pattern) {
        ModerationIncident incident = new ModerationIncident();
        incident.setContentHash(hashContent(input.getContent()));
        incident.setUserId(input.getUserId());
        incident.setCategory(pattern.getCategory());
        incident.setSeverity(pattern.getSeverity());
        incident.setStatus(IncidentStatus.PENDING);
        incident.setAutomated(true);
        
        moderationRepository.save(incident);
        
        // Notification équipe modération
        notifyModerationTeam(incident);
        
        // Si critique, suspension temporaire
        if (pattern.getSeverity() == Severity.CRITICAL) {
            temporarilySuspendUser(input.getUserId(), 
                                   Duration.ofHours(24));
        }
    }
}
```

### Système de consentement GDPR

```java
@Service
public class ConsentManagementService {
    
    public ConsentRecord recordConsent(User user, 
                                      ConsentType type,
                                      boolean granted) {
        ConsentRecord record = new ConsentRecord();
        record.setUserId(user.getId());
        record.setConsentType(type);
        record.setGranted(granted);
        record.setVersion(CURRENT_CONSENT_VERSION);
        record.setTimestamp(LocalDateTime.now());
        record.setIpAddress(requestContext.getClientIP());
        record.setUserAgent(requestContext.getUserAgent());
        
        consentRepository.save(record);
        
        // Publier événement
        eventPublisher.publishConsentChanged(record);
        
        return record;
    }
    
    public void enforceRightToErasure(User user) {
        // GDPR Article 17 - Right to be forgotten
        
        // 1. Anonymiser données personnelles
        user.setEmail(anonymize(user.getEmail()));
        user.setUsername("deleted_" + UUID.randomUUID());
        user.setDeletedAt(LocalDateTime.now());
        
        // 2. Supprimer conversations
        conversationService.deleteUserConversations(user.getId());
        
        // 3. Conserver données légales (fraud prevention)
        retainLegalData(user.getId());
        
        // 4. Notification partenaires
        notifyDataProcessors(user.getId(), 
                            DataOperation.ERASURE);
        
        // 5. Confirmation utilisateur
        emailService.sendErasureConfirmation(user.getEmail());
    }
    
    public DataExport exportUserData(User user) {
        // GDPR Article 20 - Right to data portability
        
        DataExport export = new DataExport();
        export.setUser(sanitizeUserData(user));
        export.setConversations(
            conversationService.exportConversations(user.getId()));
        export.setPreferences(
            preferencesService.exportPreferences(user.getId()));
        export.setSubscriptions(
            billingService.exportSubscriptionHistory(user.getId()));
        
        // Format JSON structuré
        String json = objectMapper.writeValueAsString(export);
        
        // Génération fichier téléchargeable
        return createDownloadableExport(json);
    }
}
```

## Tests et qualité

### Framework de tests complet

```java
// Tests unitaires avec JUnit 5
@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
class ConversationServiceTest {
    
    @Autowired
    private ConversationService conversationService;
    
    @MockBean
    private DialogueEngine dialogueEngine;
    
    @Test
    @DisplayName("Should create new conversation with valid inputs")
    void testCreateConversation() {
        // Given
        User user = TestDataFactory.createUser();
        VirtualCharacter character = TestDataFactory.createCharacter();
        
        // When
        Conversation conversation = conversationService
            .startConversation(user.getId(), character.getId());
        
        // Then
        assertNotNull(conversation.getId());
        assertEquals(user.getId(), conversation.getUserId());
        assertEquals(ConversationStatus.ACTIVE, conversation.getStatus());
    }
    
    @Test
    @DisplayName("Should block inappropriate content")
    void testContentModeration() {
        // Given
        String inappropriateMessage = "Inappropriate content here";
        when(moderator.moderate(any()))
            .thenReturn(ModerationResult.blocked());
        
        // When/Then
        assertThrows(ContentBlockedException.class, () -> {
            conversationService.sendMessage(
                conversationId, 
                inappropriateMessage);
        });
    }
}

// Tests d'intégration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Testcontainers
class UserApiIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testUserRegistrationFlow() {
        // 1. Register
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setBirthdate(LocalDate.of(1990, 1, 1));
        
        ResponseEntity<UserResponse> response = restTemplate
            .postForEntity("/api/v1/users/register", 
                          request, 
                          UserResponse.class);
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        // 2. Verify email
        String verificationToken = extractTokenFromEmail();
        restTemplate.postForEntity(
            "/api/v1/users/verify-email?token=" + verificationToken,
            null,
            Void.class);
        
        // 3. Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        
        ResponseEntity<AuthResponse> authResponse = restTemplate
            .postForEntity("/api/v1/auth/login", 
                          loginRequest, 
                          AuthResponse.class);
        
        assertNotNull(authResponse.getBody().getAccessToken());
    }
}

// Tests de charge avec Gatling
class ConversationLoadTest extends Simulation {
    
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("https://api.virtualcompanion.com")
        .header("Authorization", "Bearer ${accessToken}");
    
    ScenarioBuilder conversationScenario = scenario("Conversation Load Test")
        .exec(session -> {
            // Simulate realistic user behavior
            return session;
        })
        .exec(http("Start Conversation")
            .post("/api/v1/conversations/start")
            .body(StringBody("""
                {"characterId": "${characterId}"}
                """))
            .check(status().is(200))
            .check(jsonPath("$.id").saveAs("conversationId")))
        .pause(2, 5)
        .repeat(10, "i").on(
            exec(http("Send Message")
                .post("/api/v1/conversations/${conversationId}/message")
                .body(StringBody("""
                    {"content": "Test message ${i}"}
                    """))
                .check(status().is(200)))
            .pause(3, 8)
        );
    
    {
        setUp(
            conversationScenario.injectOpen(
                rampUsersPerSec(1).to(100).during(Duration.ofMinutes(5)),
                constantUsersPerSec(100).during(Duration.ofMinutes(10))
            )
        ).protocols(httpProtocol)
         .assertions(
             global().responseTime().percentile3().lt(2000),
             global().successfulRequests().percent().gt(95.0)
         );
    }
}
```

### Tests de sécurité automatisés

```java
@SpringBootTest
class SecurityTest {
    
    @Test
    void testSQLInjectionPrevention() {
        String maliciousInput = "admin' OR '1'='1";
        
        assertThrows(ValidationException.class, () -> {
            userService.findByEmail(maliciousInput);
        });
    }
    
    @Test
    void testXSSPrevention() {
        String xssPayload = "<script>alert('XSS')</script>";
        
        String sanitized = sanitizer.sanitize(xssPayload);
        
        assertFalse(sanitized.contains("<script>"));
    }
    
    @Test
    void testRateLimiting() {
        // Attempt 101 requests (limit is 100/hour)
        for (int i = 0; i < 101; i++) {
            if (i < 100) {
                ResponseEntity<?> response = makeRequest();
                assertEquals(HttpStatus.OK, response.getStatusCode());
            } else {
                assertThrows(RateLimitExceededException.class, 
                           this::makeRequest);
            }
        }
    }
}
```

## Déploiement et infrastructure

### Architecture Kubernetes

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: conversation-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: conversation-service
  template:
    metadata:
      labels:
        app: conversation-service
    spec:
      containers:
      - name: conversation-service
        image: myregistry/conversation-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: conversation-service
spec:
  selector:
    app: conversation-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: conversation-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: conversation-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Monitoring avec Prometheus + Grafana

```yaml
# prometheus-config.yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'user-service:8080'
        - 'conversation-service:8080'
        - 'media-service:8080'
        - 'billing-service:8080'

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

### CI/CD Pipeline (GitLab CI)

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - security
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

build:
  stage: build
  image: maven:3.9-openjdk-21
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar
  cache:
    paths:
      - .m2/repository

test:unit:
  stage: test
  image: maven:3.9-openjdk-21
  script:
    - mvn test
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml

test:integration:
  stage: test
  image: maven:3.9-openjdk-21
  services:
    - postgres:16
    - redis:7
  variables:
    POSTGRES_DB: testdb
    POSTGRES_USER: test
    POSTGRES_PASSWORD: test
  script:
    - mvn verify -P integration-tests

security:sast:
  stage: security
  image: returntocorp/semgrep
  script:
    - semgrep --config=auto --json -o semgrep-results.json
  artifacts:
    reports:
      sast: semgrep-results.json

security:dependency-check:
  stage: security
  image: maven:3.9-openjdk-21
  script:
    - mvn dependency-check:check
  artifacts:
    reports:
      dependency_scanning: target/dependency-check-report.json

deploy:production:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/conversation-service 
        conversation-service=myregistry/conversation-service:$CI_COMMIT_SHA
    - kubectl rollout status deployment/conversation-service
  only:
    - main
  when: manual
```

## Budget et timeline

### Équipe recommandée (Phase MVP - 6 mois)

```yaml
Équipe Core (7 personnes):
  - 1x Tech Lead / Architect Senior
    Salaire: €7,000-10,000/mois
    
  - 2x Backend Developers (Java/Spring)
    Salaire: €5,000-7,000/mois chacun
    
  - 1x Frontend Developer (React/React Native)
    Salaire: €4,500-6,500/mois
    
  - 1x ML/AI Engineer
    Salaire: €6,000-8,000/mois
    
  - 1x DevOps Engineer
    Salaire: €5,500-7,500/mois
    
  - 1x QA Engineer
    Salaire: €4,000-5,500/mois

Équipe Support (3 personnes):
  - 1x UI/UX Designer
    Salaire: €4,000-6,000/mois
    
  - 1x Legal/Compliance Specialist
    Salaire: €5,000-7,000/mois
    
  - 1x Community/Content Moderator
    Salaire: €3,000-4,000/mois

Total salaires mensuel: €48,000-71,000
Total 6 mois: €288,000-426,000
```

### Coûts infrastructure mensuels (Production)

```yaml
Cloud Hosting (AWS/GCP/Azure):
  - Kubernetes Cluster (3 nodes): €800-1,500
  - Load Balancers: €200-400
  - Database (PostgreSQL managed): €500-1,000
  - Redis (managed): €200-400
  - Elasticsearch (managed): €400-800
  - Kafka (managed): €300-600
  - GPU instances (ML inference): €1,500-3,000
  Subtotal: €3,900-7,700

CDN & Storage:
  - CDN (Cloudflare/CloudFront): €200-500
  - Object Storage (S3): €300-800
  - Video streaming CDN: €500-1,500
  Subtotal: €1,000-2,800

Services Tiers:
  - Stripe (paiements): 2.9% + €0.25/transaction
  - Twilio (SMS vérification): €200-500
  - Identity verification API: €500-1,500
  - Email service (SendGrid): €100-300
  - Monitoring (Datadog/New Relic): €300-600
  Subtotal: €1,100-3,900

Sécurité:
  - WAF (Web Application Firewall): €200-500
  - DDoS protection: €300-600
  - SSL certificates (Let's Encrypt): €0
  - Security monitoring: €200-400
  Subtotal: €700-1,500

Total infrastructure mensuel: €6,700-15,900
Total annuel: €80,400-190,800
```

### Coûts développement additionnels

```yaml
Outils & Licenses:
  - JetBrains licenses: €150/mois
  - GitHub/GitLab: €100/mois
  - Design tools (Figma): €45/mois
  - Project management (Jira): €140/mois
  - Analytics (Mixpanel): €200/mois
  Total: €635/mois

Légal & Compliance:
  - Privacy policy rédaction: €3,000-5,000 (one-time)
  - Terms of Service: €2,000-4,000 (one-time)
  - GDPR compliance audit: €5,000-10,000 (one-time)
  - Legal counsel (retainer): €2,000-5,000/mois

Marketing & Launch:
  - Landing page: €3,000-8,000
  - Brand identity: €5,000-15,000
  - Marketing materials: €2,000-5,000
  - Initial ad spend: €10,000-50,000
```

### Timeline détaillée (12 mois)

```yaml
Phase 1: MVP (Mois 1-4)
  Mois 1:
    - Setup infrastructure & CI/CD
    - Architecture microservices
    - User service (auth, registration)
    - Database schema design
  
  Mois 2:
    - Character service
    - Conversation service (texte uniquement)
    - IA conversationnelle basique (GPT-J)
    - Frontend web (React)
  
  Mois 3:
    - Billing service + Stripe
    - Content moderation basique
    - System d'abonnements
    - Tests unitaires & intégration
  
  Mois 4:
    - Compliance & vérification d'âge
    - Gamification basique
    - Beta testing (100 users)
    - Bug fixes & optimisations

  Livrable: Application web fonctionnelle avec chat texte

Phase 2: Mobile & Audio (Mois 5-7)
  Mois 5:
    - Application mobile (React Native)
    - Chat vocal (WebRTC audio)
    - Synthèse vocale (Coqui TTS)
  
  Mois 6:
    - Transcription vocale (Whisper)
    - Optimisation mobile
    - Notifications push
    - Analytics avancés
  
  Mois 7:
    - Tests utilisateurs (1,000 users)
    - Performance optimizations
    - A/B testing features
    - Soft launch (limited regions)

  Livrable: Apps mobile iOS/Android avec chat vocal

Phase 3: Vidéo & Premium (Mois 8-10)
  Mois 8:
    - Chat vidéo (avatars 3D)
    - Lip-sync intelligent
    - Animation faciale temps réel
  
  Mois 9:
    - Personnalisation avancée
    - Contenu premium (compliance)
    - Marketplace personnages
    - Integrations tierces
  
  Mois 10:
    - VR support basique (Quest)
    - Tests charge & scaling
    - Security audit complet
    - Préparation lancement public

  Livrable: Plateforme complète avec vidéo temps réel

Phase 4: VR & Polish (Mois 11-12)
  Mois 11:
    - VR expérience complète
    - Environnements 3D
    - Hand tracking
    - Spatial audio
  
  Mois 12:
    - Marketing campaign
    - Lancement public
    - Customer support setup
    - Monitoring & incident response
    - Documentation utilisateur

  Livrable: Lancement public avec toutes fonctionnalités
```

## Stratégie de monétisation

### Prévisions revenus (12 mois post-lancement)

```yaml
Hypothèses conservatrices:
  Mois 1: 1,000 users
    - 70% Free
    - 20% Standard (€9.99)
    - 8% Premium (€19.99)
    - 2% VIP (€39.99)
    Revenu: €3,598

  Mois 6: 10,000 users (croissance 50%/mois)
    Revenu: €35,980
  
  Mois 12: 50,000 users
    Revenu: €179,900

Année 1 total: ~€600,000-800,000
Coûts année 1: ~€450,000-550,000
Profit potentiel: €50,000-350,000

Année 2 projection: 200,000 users
  Revenu mensuel: €719,600
  Revenu annuel: €8,635,200
  Profit (après coûts): €6,000,000-7,000,000
```

### Revenus additionnels

```yaml
Marketplace (commission 30%):
  - Personnages communautaires
  - Tenues/accessoires virtuels
  - Voix personnalisées
  Potentiel: €50,000-200,000/an

API Developer Tier:
  - €199/mois par développeur
  - 100 développeurs = €19,900/mois
  Potentiel: €240,000/an

Partenariats B2B:
  - White-label solutions
  - Enterprise licenses
  Potentiel: €100,000-500,000/an

Publicité (Free tier):
  - €2-5 CPM
  - 100,000 free users * 10 sessions/mois
  Potentiel: €20,000-50,000/mois
```

## Conclusion

Cette architecture complète fournit:

✅ **Stack 100% open source** (coût licences: €0)
✅ **Multi-plateforme** (Web, Mobile, VR)
✅ **Conformité légale** stricte (GDPR, COPPA, etc.)
✅ **Scalabilité** (architecture microservices)
✅ **Sécurité renforcée** (modération IA, encryption)
✅ **Monétisation viable** (ROI positif année 2)
✅ **Technologies modernes** (Java 21, Spring Boot 3, React 18)

**Prochaines étapes:**
1. Validation concept avec prototypes
2. Consultation légale approfondie
3. Recherche financement (€300-500k seed)
4. Recrutement équipe core
5. Développement MVP (4 mois)
6. Beta testing et itérations
7. Lancement progressif par région
8. Scaling et expansion features