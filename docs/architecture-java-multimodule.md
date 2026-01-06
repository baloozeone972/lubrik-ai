# Architecture Java Multi-Module - NexusAI

## 1. Vue d'ensemble

### 1.1 Principes d'architecture
Cette architecture définit la structure d'un projet Java multi-module pour NexusAI, en suivant les principes:
- **Modularité**: Séparation claire des responsabilités
- **Faible couplage**: Minimiser dépendances entre modules
- **Haute cohésion**: Regrouper fonctionnalités liées
- **Maintenabilité**: Structure simple à comprendre et faire évoluer
- **Testabilité**: Faciliter tests unitaires et d'intégration
- **Évolutivité**: Permettre ajout progressif des fonctionnalités

### 1.2 Structure générale
L'application est organisée en projet Maven multi-modules avec structure arborescente:

```
nexus-ai-parent/
├── nexus-core/                  # Core du système et modèles communs
├── nexus-auth/                  # Authentification et gestion utilisateurs
├── nexus-companion/             # Gestion des modèles de compagnons
├── nexus-conversation/          # Moteur de conversation
├── nexus-ai-engine/             # Intégration moteurs IA
├── nexus-media/                 # Gestion audio et vidéo
├── nexus-api/                   # API REST
├── nexus-web/                   # Interface web
├── nexus-commons/               # Utilitaires partagés
└── nexus-deployment/            # Scripts et configs déploiement
```

### 1.3 Technologies principales
- **JDK**: Java 21 (utilisation features modernes: records, pattern matching)
- **Build**: Maven 3.9+
- **Framework**: Spring Boot 3.2+
- **Persistence**: JPA/Hibernate avec Spring Data
- **API**: REST avec Spring Web + OpenAPI
- **Web**: Thymeleaf + Bootstrap + JavaScript
- **Testing**: JUnit 5, Mockito, Testcontainers
- **Documentation**: Javadoc, Swagger/OpenAPI

## 2. Détail des modules et leurs responsabilités

### 2.1 Module: nexus-core

#### 2.1.1 Responsabilités
- Définition des entités et modèles de domaine
- Interfaces des services principaux
- Gestion des événements du système
- Configuration commune
- Exceptions et erreurs standardisées
- Utilités centrales

#### 2.1.2 Structure interne
```
nexus-core/
├── src/main/java/com/nexusai/core/
│   ├── model/                   # Entités et modèles domaine
│   │   ├── user/                # Modèles relatifs aux utilisateurs
│   │   ├── companion/           # Modèles relatifs aux compagnons
│   │   ├── conversation/        # Modèles relatifs aux conversations
│   │   └── media/               # Modèles relatifs aux médias
│   ├── exception/               # Exceptions personnalisées
│   ├── service/                 # Interfaces des services
│   ├── event/                   # Événements système
│   │   ├── model/               # Modèles d'événements
│   │   └── publisher/           # Publication d'événements
│   ├── config/                  # Configuration partagée
│   └── util/                    # Utilitaires communs
└── src/test/java/...            # Tests unitaires
```

#### 2.1.3 Classes principales
```java
// Exemple d'entité principale
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String email;
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionType subscriptionType;
    
    private LocalDateTime subscriptionEndDate;
    private Integer tokensRemaining;
    
    // getters, setters, etc.
}

// Exemple d'interface service
public interface UserService {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User create(User user);
    User update(User user);
    void delete(Long userId);
    boolean consumeTokens(Long userId, int amount);
    void addTokens(Long userId, int amount);
}
```

#### 2.1.4 Dépendances externes
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- lombok
- slf4j

### 2.2 Module: nexus-auth

#### 2.2.1 Responsabilités
- Authentification et gestion des sessions
- Autorisation et contrôle d'accès
- Gestion des profils utilisateurs
- Abonnements et plans tarifaires
- Gestion des jetons

#### 2.2.2 Structure interne
```
nexus-auth/
├── src/main/java/com/nexusai/auth/
│   ├── config/                  # Config Spring Security
│   ├── controller/              # Endpoints auth (interne)
│   ├── service/                 # Services implémentation
│   │   ├── UserServiceImpl.java
│   │   ├── AuthenticationService.java
│   │   ├── TokenService.java
│   │   └── SubscriptionService.java
│   ├── repository/              # Repositories JPA
│   ├── security/                # Classes sécurité
│   │   ├── JwtTokenProvider.java
│   │   └── UserDetailsServiceImpl.java
│   └── util/                    # Utilitaires auth
└── src/test/java/...            # Tests
```

#### 2.2.3 Classes principales
```java
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Authenticate user and generate tokens
    }
    
    public AuthenticationResponse register(RegistrationRequest request) {
        // Register new user
    }
    
    public void validateEmail(String token) {
        // Email validation logic
    }
    
    public AuthenticationResponse refreshToken(String refreshToken) {
        // Token refresh logic
    }
}

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    public SubscriptionDetails updateSubscription(Long userId, SubscriptionType type, int months) {
        // Update subscription logic
    }
    
    public void cancelSubscription(Long userId) {
        // Cancel subscription logic
    }
    
    public boolean checkFeatureAccess(Long userId, FeatureType feature) {
        // Check if user has access to specific feature
    }
}
```

#### 2.2.4 Dépendances
- nexus-core
- spring-boot-starter-security
- jjwt
- spring-boot-starter-mail (notifications)

### 2.3 Module: nexus-companion

#### 2.3.1 Responsabilités
- Gestion des modèles de compagnons
- Personnalisation des compagnons
- Évolution des compagnons
- Templates et présets
- Stockage et récupération des compagnons

#### 2.3.2 Structure interne
```
nexus-companion/
├── src/main/java/com/nexusai/companion/
│   ├── config/
│   ├── controller/              # API interne
│   ├── service/
│   │   ├── CompanionService.java
│   │   ├── PersonalityService.java
│   │   ├── TemplateService.java
│   │   └── CompanionEvolutionService.java
│   ├── repository/
│   ├── mapper/                  # Mappers DTO/Entité
│   └── util/
└── src/test/java/...
```

#### 2.3.3 Classes principales
```java
@Service
@RequiredArgsConstructor
public class CompanionServiceImpl implements CompanionService {
    private final CompanionRepository companionRepository;
    private final PersonalityService personalityService;
    private final UserService userService;

    @Override
    public Companion createCompanion(Long userId, CompanionCreationRequest request) {
        // Verify user permissions
        User user = userService.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
            
        // Check limits based on subscription
        if (!userService.canCreateMoreCompanions(userId)) {
            throw new LimitExceededException("companion.creation.limit");
        }
        
        // Create companion logic
        Companion companion = new Companion();
        // Set properties
        
        // Create personality
        Personality personality = personalityService.createPersonality(request.getPersonalityParams());
        companion.setPersonality(personality);
        
        return companionRepository.save(companion);
    }
    
    // Other methods
}

@Service
@RequiredArgsConstructor
public class PersonalityServiceImpl implements PersonalityService {
    private final PersonalityRepository personalityRepository;
    
    @Override
    public Personality createPersonality(PersonalityParams params) {
        // Create personality based on params
    }
    
    @Override
    public Personality evolvePersonality(Long personalityId, InteractionData interactionData) {
        // Logic to evolve personality based on interactions
    }
}
```

#### 2.3.4 Dépendances
- nexus-core
- nexus-commons
- spring-boot-starter-data-jpa

### 2.4 Module: nexus-conversation

#### 2.4.1 Responsabilités
- Gestion des conversations et messages
- Contexte et mémoire des conversations
- Analyse de sentiment basique
- Formatage et prétraitement des messages
- Système de templating pour réponses

#### 2.4.2 Structure interne
```
nexus-conversation/
├── src/main/java/com/nexusai/conversation/
│   ├── config/
│   ├── controller/
│   ├── service/
│   │   ├── ConversationService.java
│   │   ├── MessageService.java
│   │   ├── ContextService.java
│   │   └── MemoryService.java
│   ├── repository/
│   ├── processor/               # Traitement messages
│   │   ├── MessagePreProcessor.java
│   │   ├── MessagePostProcessor.java
│   │   └── SentimentAnalyzer.java
│   └── util/
└── src/test/java/...
```

#### 2.4.3 Classes principales
```java
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageService messageService;
    private final ContextService contextService;
    private final AIProviderService aiProviderService;
    private final MessagePreProcessor preProcessor;
    private final MessagePostProcessor postProcessor;
    
    @Override
    @Transactional
    public Message sendMessage(Long conversationId, MessageRequest request) {
        Conversation conversation = getConversation(conversationId);
        
        // Pre-process message
        String processedContent = preProcessor.process(request.getContent(), 
                                                     conversation.getCompanion().getPersonality());
        
        // Create user message
        Message userMessage = messageService.createMessage(
            conversation, request.getUserId(), processedContent, MessageType.USER);
            
        // Get context for AI response
        Context context = contextService.buildContext(conversation);
        
        // Get AI response
        String aiResponse = aiProviderService.generateResponse(
            processedContent, context, conversation.getCompanion());
            
        // Post-process AI response
        String processedResponse = postProcessor.process(
            aiResponse, conversation.getCompanion().getPersonality());
            
        // Create AI message
        Message aiMessage = messageService.createMessage(
            conversation, null, processedResponse, MessageType.AI);
            
        // Update conversation last activity
        conversation.setLastActivity(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        return aiMessage;
    }
    
    // Other methods
}

@Service
@RequiredArgsConstructor
public class ContextServiceImpl implements ContextService {
    private final MessageRepository messageRepository;
    private final MemoryService memoryService;
    
    @Override
    public Context buildContext(Conversation conversation) {
        // Get recent messages
        List<Message> recentMessages = messageRepository
            .findRecentByConversationId(conversation.getId(), 20);
            
        // Get relevant memories
        List<Memory> relevantMemories = memoryService
            .findRelevantMemories(conversation.getId(), 5);
            
        // Build context
        Context context = new Context();
        context.setRecentMessages(recentMessages);
        context.setMemories(relevantMemories);
        context.setCompanionPersonality(conversation.getCompanion().getPersonality());
        
        return context;
    }
}
```

#### 2.4.4 Dépendances
- nexus-core
- nexus-commons
- nexus-companion (pour info personnalité)
- spring-boot-starter-data-jpa

### 2.5 Module: nexus-ai-engine

#### 2.5.1 Responsabilités
- Intégration des modèles IA locaux et API externes
- Génération de texte
- Analyse de messages
- Gestion des requêtes et réponses IA
- Optimisation et caching des requêtes

#### 2.5.2 Structure interne
```
nexus-ai-engine/
├── src/main/java/com/nexusai/ai/
│   ├── config/
│   ├── service/
│   │   ├── AIProviderService.java      # Interface principale
│   │   ├── LocalAIService.java         # Implémentation locale
│   │   └── ExternalAIService.java      # Implémentation API externe
│   ├── model/                     # Modèles spécifiques IA
│   ├── adapter/                   # Adaptateurs pour différents modèles
│   │   ├── llama/
│   │   ├── falcon/
│   │   └── openai/
│   ├── prompt/                    # Templates de prompts
│   ├── cache/                     # Système de cache
│   └── util/
└── src/test/java/...
```

#### 2.5.3 Classes principales
```java
@Service
@RequiredArgsConstructor
public class AIProviderServiceImpl implements AIProviderService {
    private final LocalAIService localAIService;
    private final ExternalAIService externalAIService;
    private final AIResponseCache responseCache;
    private final PromptTemplateService promptTemplateService;
    
    @Override
    public String generateResponse(String userMessage, Context context, Companion companion) {
        // Check cache first
        String cacheKey = generateCacheKey(userMessage, context, companion);
        Optional<String> cachedResponse = responseCache.get(cacheKey);
        
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        
        // Prepare prompt
        String prompt = promptTemplateService.createPrompt(userMessage, context, companion);
        
        // Try local model first for efficiency
        try {
            String response = localAIService.generateResponse(prompt, companion.getPersonality());
            responseCache.put(cacheKey, response);
            return response;
        } catch (AIServiceException e) {
            // Fallback to external service
            log.warn("Local AI service failed, falling back to external: {}", e.getMessage());
            String response = externalAIService.generateResponse(prompt, companion.getPersonality());
            responseCache.put(cacheKey, response);
            return response;
        }
    }
    
    private String generateCacheKey(String userMessage, Context context, Companion companion) {
        // Generate unique hash for caching
    }
}

@Service
@RequiredArgsConstructor
public class LocalAIService {
    private final AIModelManager modelManager;
    
    public String generateResponse(String prompt, Personality personality) {
        // Get appropriate local model
        AIModel model = modelManager.getModelForPersonality(personality);
        
        // Generate response using DJL or other library
        return model.infer(prompt);
    }
}
```

#### 2.5.4 Dépendances
- nexus-core
- nexus-commons
- nexus-companion (pour personnalité)
- ai-djl (Deep Java Library)
- spring-boot-starter-cache
- caffeine (cache in-memory)

### 2.6 Module: nexus-media

#### 2.6.1 Responsabilités
- Gestion audio et voix
- Gestion vidéo
- Stockage médias
- Streaming
- Intégration WebRTC

#### 2.6.2 Structure interne
```
nexus-media/
├── src/main/java/com/nexusai/media/
│   ├── config/
│   ├── controller/
│   ├── service/
│   │   ├── AudioService.java
│   │   ├── VideoService.java
│   │   ├── StorageService.java
│   │   └── StreamingService.java
│   ├── handler/                  # Handlers WebRTC/WebSocket
│   ├── repository/
│   └── util/
└── src/test/java/...
```

#### 2.6.3 Classes principales
```java
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {
    private final StorageService storageService;
    private final UserService userService;
    
    @Override
    public AudioMessage createAudioMessage(Long conversationId, Long userId, MultipartFile audioFile) {
        // Check user permissions and limits
        User user = userService.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
            
        if (!userService.canUseAudioFeature(userId)) {
            throw new FeatureNotAvailableException("audio.feature.unavailable");
        }
        
        // Check file format and size
        validateAudioFile(audioFile);
        
        // Store audio file
        String audioUrl = storageService.storeAudio(audioFile, userId, conversationId);
        
        // Create audio message
        AudioMessage audioMessage = new AudioMessage();
        audioMessage.setConversationId(conversationId);
        audioMessage.setUserId(userId);
        audioMessage.setAudioUrl(audioUrl);
        audioMessage.setDuration(calculateAudioDuration(audioFile));
        audioMessage.setTimestamp(LocalDateTime.now());
        
        return audioMessageRepository.save(audioMessage);
    }
    
    // Other methods
}

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    
    @Override
    public String storeAudio(MultipartFile file, Long userId, Long conversationId) {
        try {
            String filename = generateUniqueFilename(file.getOriginalFilename());
            String bucketPath = String.format("users/%d/conversations/%d/audio/", 
                                          userId, conversationId);
                                          
            String contentType = file.getContentType();
            
            // Upload to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(bucketPath + filename)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
            
            return bucketPath + filename;
        } catch (Exception e) {
            throw new StorageException("Failed to store audio file", e);
        }
    }
    
    // Other methods
}
```

#### 2.6.4 Dépendances
- nexus-core
- nexus-commons
- spring-boot-starter-websocket
- minio-java (stockage objet)
- jcodec (processing audio/vidéo)

### 2.7 Module: nexus-api

#### 2.7.1 Responsabilités
- Exposition des API REST publiques
- Documentation OpenAPI
- Validation requêtes
- Sécurité API
- Mapping entités/DTOs

#### 2.7.2 Structure interne
```
nexus-api/
├── src/main/java/com/nexusai/api/
│   ├── config/
│   │   └── OpenApiConfig.java
│   ├── controller/
│   │   ├── UserController.java
│   │   ├── AuthController.java
│   │   ├── CompanionController.java
│   │   ├── ConversationController.java
│   │   └── MediaController.java
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── mapper/
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   ├── security/
│   └── validation/
└── src/test/java/...
```

#### 2.7.3 Classes principales
```java
@RestController
@RequestMapping("/api/v1/companions")
@RequiredArgsConstructor
public class CompanionController {
    private final CompanionService companionService;
    private final CompanionMapper companionMapper;
    
    @GetMapping
    @Operation(summary = "Get all companions for user")
    public List<CompanionDto> getUserCompanions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) boolean includeSystem) {
        
        Long userId = ((CustomUserDetails) userDetails).getUserId();
        List<Companion> companions = companionService.findByUserId(userId, includeSystem);
        
        return companions.stream()
            .map(companionMapper::toDto)
            .collect(Collectors.toList());
    }
    
    @PostMapping
    @Operation(summary = "Create new companion")
    public CompanionDto createCompanion(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanionCreationRequest request) {
        
        Long userId = ((CustomUserDetails) userDetails).getUserId();
        Companion companion = companionService.createCompanion(userId, request);
        
        return companionMapper.toDto(companion);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get companion by ID")
    public CompanionDto getCompanion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        Long userId = ((CustomUserDetails) userDetails).getUserId();
        Companion companion = companionService.findById(id, userId);
        
        return companionMapper.toDto(companion);
    }
    
    // Other endpoints
}

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    
    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send message to conversation")
    public MessageDto sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long conversationId,
            @Valid @RequestBody MessageRequest request) {
        
        Long userId = ((CustomUserDetails) userDetails).getUserId();
        request.setUserId(userId);
        
        Message message = conversationService.sendMessage(conversationId, request);
        
        return messageMapper.toDto(message);
    }
    
    // Other endpoints
}
```

#### 2.7.4 Dépendances
- Tous les autres modules fonctionnels
- spring-boot-starter-web
- springdoc-openapi-ui
- modelmapper

### 2.8 Module: nexus-web

#### 2.8.1 Responsabilités
- Interface web utilisateur
- Templates Thymeleaf
- Assets statiques (JS, CSS, images)
- Frontend interactif
- WebSockets pour communication temps réel

#### 2.8.2 Structure interne
```
nexus-web/
├── src/main/java/com/nexusai/web/
│   ├── config/
│   ├── controller/
│   │   ├── HomeController.java
│   │   ├── UserViewController.java
│   │   ├── CompanionViewController.java
│   │   └── ConversationViewController.java
│   ├── security/
│   ├── interceptor/
│   ├── util/
│   └── websocket/
└── src/main/resources/
    ├── static/
    │   ├── css/
    │   ├── js/
    │   ├── images/
    │   └── webfonts/
    └── templates/
        ├── layout/
        ├── home/
        ├── user/
        ├── companion/
        └── conversation/
```

#### 2.8.3 Classes principales
```java
@Controller
@RequiredArgsConstructor
public class ConversationViewController {
    private final ConversationService conversationService;
    private final CompanionService companionService;
    
    @GetMapping("/conversations/{id}")
    public String viewConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        
        Long userId = ((CustomUserDetails) userDetails).getUserId();
        
        // Get conversation (with authorization check)
        Conversation conversation = conversationService.findById(id, userId);
        
        // Get recent messages
        List<Message> messages = conversationService.getRecentMessages(id, 50);
        
        // Get companion details
        Companion companion = conversation.getCompanion();
        
        // Add to model
        model.addAttribute("conversation", conversation);
        model.addAttribute("messages", messages);
        model.addAttribute("companion", companion);
        model.addAttribute("user", userDetails);
        
        return "conversation/view";
    }
    
    // Other methods
}

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .withSockJS();
    }
}

@Controller
public class ChatWebSocketController {
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat/{conversationId}")
    public void processMessage(@DestinationVariable Long conversationId, 
                             ChatMessageDTO message, 
                             Principal principal) {
        // Process message
        // ...
        
        // Send response
        messagingTemplate.convertAndSendToUser(
            principal.getName(),
            "/topic/chat/" + conversationId,
            responseMessage);
    }
}
```

#### 2.8.4 Dépendances
- nexus-api (pour accès services)
- spring-boot-starter-thymeleaf
- spring-boot-starter-security
- spring-boot-starter-websocket
- bootstrap
- thymeleaf-layout-dialect

### 2.9 Module: nexus-commons

#### 2.9.1 Responsabilités
- Utilitaires partagés
- Validateurs communs
- Helper classes
- Fonctions d'extension
- Convertisseurs communs

#### 2.9.2 Structure interne
```
nexus-commons/
├── src/main/java/com/nexusai/commons/
│   ├── util/
│   │   ├── StringUtils.java
│   │   ├── DateTimeUtils.java
│   │   └── SecurityUtils.java
│   ├── validation/
│   │   └── validators/
│   ├── converter/
│   ├── exception/
│   └── logging/
└── src/test/java/...
```

#### 2.9.3 Classes principales
```java
public final class StringUtils {
    private StringUtils() {
        // Prevent instantiation
    }
    
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        
        return str.length() <= maxLength 
            ? str 
            : str.substring(0, maxLength) + "...";
    }
    
    public static String sanitizeInput(String input) {
        // Sanitize input to prevent XSS
    }
    
    // Other utility methods
}

public final class DateTimeUtils {
    private DateTimeUtils() {
        // Prevent instantiation
    }
    
    public static String formatRelative(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);
        
        // Return formatted relative time
    }
    
    // Other datetime utility methods
}
```

#### 2.9.4 Dépendances
- commons-lang3
- jackson-databind
- slf4j

## 3. Interactions entre modules

### 3.1 Diagramme de dépendances
```
  ┌─────────────┐
  │nexus-commons◄──────────────────┐
  └─────▲───────┘                  │
        │                          │
        │       ┌─────────────┐    │
        └───────►   nexus-core◄────┼───────────────┐
                └─────▲───────┘    │               │
                      │            │               │
┌─────────────┐       │            │    ┌──────────▼─────────┐
│nexus-deployment     │            │    │     nexus-media    │
└─────────────┘       │            │    └──────────▲─────────┘
                      │            │               │
     ┌────────────────┼────────────┼───────┐       │
     │                │            │       │       │
┌────▼──────┐  ┌──────▼────┐ ┌─────▼───┐ ┌─▼───────▼──┐
│nexus-auth ◄──►nexus-companion nexus-ai│ │nexus-convers◄───┐
└────▲──────┘  └──────▲────┘ └─────────┘ └────────────┘     │
     │                │                         ▲            │
     │                │                         │            │
┌────┴────────────────┴─────────────────────────┴────┐      │
│                nexus-api                            ◄──────┘
└────▲─────────────────────────────────────────────┬─┘
     │                                             │
     │                                             │
┌────┴─────────────────────────────────────────────▼─┐
│                    nexus-web                        │
└──────────────────────────────────────────────────┬──┘
```

### 3.2 Exemples d'interactions typiques

#### 3.2.1 Flux d'authentification
1. Web UI (nexus-web) → Formulaire login
2. Controller web → Controller API (nexus-api)
3. AuthController → AuthService (nexus-auth)
4. AuthService → UserService → UserRepository
5. Validation credentials → Génération token JWT
6. Retour token et user info → Session utilisateur

#### 3.2.2 Flux conversation
1. Web UI → Envoi message (form ou WebSocket)
2. ConversationViewController → ConversationController (API)
3. ConversationController → ConversationService (nexus-conversation)
4. Récupération contexte → ContextService
5. Appel IA → AIProviderService (nexus-ai-engine)
6. Génération réponse → Post-processing
7. Sauvegarde messages → Notification WebSocket
8. Retour réponse → Affichage UI

## 4. Architecture détaillée par module clé

### 4.1 Architecture conversation et IA
```
┌───────────────────────────────┐
│      ConversationController   │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│      ConversationService      │
└───────┬──────────────┬────────┘
        │              │
        ▼              ▼
┌───────────────┐ ┌────────────────┐
│ MessageService│ │ContextService  │
└───────┬───────┘ └────────┬───────┘
        │                  │
        ▼                  ▼
┌───────────────┐  ┌───────────────┐
│MessageRepository MemoryService   │
└───────────────┘  └───────┬───────┘
                          │
                          ▼
┌─────────────────────────────────────┐
│          AIProviderService          │
└───────┬─────────────────────┬───────┘
        │                     │
        ▼                     ▼
┌───────────────┐     ┌───────────────┐
│ LocalAIService│     │ExternalAIService
└───────┬───────┘     └───────┬───────┘
        │                     │
        ▼                     ▼
┌───────────────┐     ┌───────────────┐
│ AIModelManager│     │   OpenAI API  │
└───────────────┘     └───────────────┘
```

### 4.2 Architecture compagnon
```
┌───────────────────────────────┐
│      CompanionController      │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│       CompanionService        │
└───────┬──────────────┬────────┘
        │              │
        ▼              ▼
┌───────────────┐ ┌────────────────┐
│PersonalityServ│ │TemplateService │
└───────┬───────┘ └────────┬───────┘
        │                  │
        ▼                  ▼
┌───────────────┐  ┌───────────────┐
│PersonalityRepo│  │TemplateRepo   │
└───────────────┘  └───────────────┘
```

### 4.3 Architecture média
```
┌───────────────────────────────┐
│        MediaController        │
└───────────────┬───────────────┘
                │
        ┌───────┴───────┐
        │               │
        ▼               ▼
┌───────────────┐ ┌────────────────┐
│ AudioService  │ │ VideoService   │
└───────┬───────┘ └────────┬───────┘
        │                  │
        └────────┬─────────┘
                 │
                 ▼
        ┌─────────────────┐
        │ StorageService  │
        └────────┬────────┘
                 │
                 ▼
        ┌─────────────────┐
        │   MinIO Client  │
        └─────────────────┘
```

## 5. Extensibilité et évolution

### 5.1 Points d'extension principaux

#### 5.1.1 Fournisseurs IA
Extension simple via pattern adaptateur:
- Création nouvelle implémentation IAProvider
- Configuration dans AIConfig
- Transparence pour reste application

#### 5.1.2 Nouvelles fonctionnalités médias
- Ajout services dans module nexus-media
- Exposition via API REST
- Intégration dans UI existante

#### 5.1.3 Nouvelles capacités compagnon
- Extension modèle Personality
- Nouveaux services et repositories
- Exposition via API existante

### 5.2 Futures évolutions anticipées

#### 5.2.1 Modèle avancé de mémoire et contexte
- Extension module conversation
- Système de mémoire à long terme sémantique
- Compatibilité arrière assurée par interfaces existantes

#### 5.2.2 Analyse émotionnelle
- Nouveau module nexus-emotion (future)
- Intégration services existants via interfaces événements
- Intégration avec Python via services REST

#### 5.2.3 Réalité augmentée basique
- Nouveau module frontal nexus-ar
- Réutilisation des services médias et compagnon
- Intégration WebXR via UI web

## 6. Bonnes pratiques de développement

### 6.1 Standards de code
- Code style Google Java Style
- Javadoc obligatoire pour classes publiques et méthodes
- Tests unitaires pour logique métier
- Logs structurés avec niveaux appropriés
- Traitement exceptions centralisé

### 6.2 Gestion des exceptions
- Hiérarchie exceptions métier
- Exceptions techniques vs fonctionnelles
- GlobalExceptionHandler pour API
- Circuit breakers pour appels externes

### 6.3 Logging
- SLF4J + Logback
- Format JSON pour intégration avec ELK
- Niveaux appropriés selon contexte
- MDC pour contextualisation logs

### 6.4 Testing
- Tests unitaires avec JUnit 5
- Tests intégration avec Testcontainers
- Architecture tests pour vérifier dépendances
- Tests API avec MockMvc/RestAssured

## 7. Configuration du projet

### 7.1 Configuration Maven (pom.xml parent)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.nexusai</groupId>
    <artifactId>nexus-ai-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>NexusAI Parent</name>
    <description>Advanced AI companion application</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <modules>
        <module>nexus-core</module>
        <module>nexus-auth</module>
        <module>nexus-companion</module>
        <module>nexus-conversation</module>
        <module>nexus-ai-engine</module>
        <module>nexus-media</module>
        <module>nexus-api</module>
        <module>nexus-web</module>
        <module>nexus-commons</module>
        <module>nexus-deployment</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        
        <!-- Dependency versions -->
        <djl.version>0.23.0</djl.version>
        <jjwt.version>0.11.5</jjwt.version>
        <minio.version>8.5.4</minio.version>
        <openapi.version>2.2.0</openapi.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Internal modules -->
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>nexus-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            
            <!-- Continue for all modules -->
            
            <!-- External dependencies -->
            <dependency>
                <groupId>ai.djl</groupId>
                <artifactId>api</artifactId>
                <version>${djl.version}</version>
            </dependency>
            
            <!-- Continue for all external dependencies -->
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Common dependencies for all modules -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
                
                <!-- Other plugins -->
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 7.2 Application Entry Point (nexus-web)

```java
package com.nexusai.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.nexusai"})
@EntityScan(basePackages = {"com.nexusai.core.model", "com.nexusai.auth.model"})
@EnableJpaRepositories(basePackages = {"com.nexusai.core.repository", 
                                    "com.nexusai.auth.repository",
                                    "com.nexusai.companion.repository",
                                    "com.nexusai.conversation.repository",
                                    "com.nexusai.media.repository"})
public class NexusAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusAiApplication.class, args);
    }
}
```

### 7.3 Exemple configuration application (application.yml)

```yaml
spring:
  application:
    name: nexus-ai
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:nexusai}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    hikari:
      maximum-pool-size: 10
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
  
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=60m
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

nexus:
  security:
    jwt:
      secret-key: ${JWT_SECRET:changeThisInProduction}
      expiration: 86400000  # 1 day
      refresh-token:
        expiration: 604800000  # 7 days
  
  ai:
    local-models-path: ${AI_MODELS_PATH:./models}
    default-model: llama-2-7b
    cache-enabled: true
    fallback-to-external: true
    
    external-api:
      enabled: ${EXTERNAL_AI_ENABLED:false}
      url: ${EXTERNAL_AI_URL:}
      key: ${EXTERNAL_AI_KEY:}
      
  storage:
    minio:
      endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
      access-key: ${MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${MINIO_SECRET_KEY:minioadmin}
      bucket: ${MINIO_BUCKET:nexusai}

logging:
  level:
    root: INFO
    com.nexusai: DEBUG
    org.springframework.web: INFO
    org.hibernate: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

server:
  port: 8080
  servlet:
    context-path: /
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,application/javascript,application/json
    min-response-size: 1024
```

---

Ce document d'architecture fournit une vue complète de la structure multi-module Java pour le projet NexusAI. Il guide l'implémentation tout en assurant les bonnes pratiques d'ingénierie logicielle et l'évolutivité du système.

Version: 1.0  
Date: [Date]
