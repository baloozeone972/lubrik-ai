package com.nexusai.conversation;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

/**
 * TESTS UNITAIRES & INTÉGRATION - MODULE CONVERSATION
 * 
 * Structure des tests:
 * - Tests unitaires (mocks)
 * - Tests d'intégration (base de données réelles)
 * - Tests E2E (API complète)
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */

// ============================================================================
// TESTS SERVICE CONVERSATION
// ============================================================================

/**
 * Tests unitaires pour ConversationService
 */
@SpringBootTest
@ActiveProfiles("test")
class ConversationServiceTest {
    
    @MockBean
    private ConversationRepository conversationRepository;
    
    @MockBean
    private LLMService llmService;
    
    @MockBean
    private MemoryService memoryService;
    
    @Autowired
    private ConversationService conversationService;
    
    @Test
    @DisplayName("Création d'une conversation réussie")
    void testCreateConversation_Success() {
        // Given
        CreateConversationRequest request = CreateConversationRequest.builder()
            .userId("user-123")
            .companionId("companion-456")
            .title("Test Conversation")
            .build();
        
        ConversationEntity entity = ConversationEntity.builder()
            .id("conv-789")
            .userId("user-123")
            .companionId("companion-456")
            .title("Test Conversation")
            .messages(new ArrayList<>())
            .build();
        
        when(conversationRepository.save(any(ConversationEntity.class)))
            .thenReturn(Mono.just(entity));
        
        when(memoryService.initializeMemory(anyString()))
            .thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(conversationService.createConversation(request))
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo("conv-789");
                assertThat(result.getUserId()).isEqualTo("user-123");
                assertThat(result.getTitle()).isEqualTo("Test Conversation");
            })
            .verifyComplete();
        
        // Verify
        verify(conversationRepository, times(1)).save(any());
        verify(memoryService, times(1)).initializeMemory(anyString());
    }
    
    @Test
    @DisplayName("Envoi d'un message avec réponse du compagnon")
    void testSendMessage_Success() {
        // Given
        String conversationId = "conv-789";
        SendMessageRequest request = SendMessageRequest.builder()
            .conversationId(conversationId)
            .content("Bonjour!")
            .type(MessageType.TEXT)
            .build();
        
        ConversationEntity conversation = ConversationEntity.builder()
            .id(conversationId)
            .userId("user-123")
            .companionId("companion-456")
            .messages(new ArrayList<>())
            .build();
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Mono.just(conversation));
        
        LLMResponse llmResponse = LLMResponse.builder()
            .content("Bonjour! Comment vas-tu?")
            .detectedEmotion("JOY")
            .emotionConfidence(0.85)
            .build();
        
        when(llmService.generateResponse(any(), any(), any()))
            .thenReturn(Mono.just(llmResponse));
        
        when(conversationRepository.save(any()))
            .thenReturn(Mono.just(conversation));
        
        // When & Then
        StepVerifier.create(conversationService.sendMessage(request))
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getContent()).isEqualTo("Bonjour! Comment vas-tu?");
                assertThat(result.getSender()).isEqualTo(MessageSender.COMPANION);
                assertThat(result.getDetectedEmotion()).isEqualTo(EmotionType.JOY);
            })
            .verifyComplete();
    }
    
    @Test
    @DisplayName("Conversation non trouvée - erreur")
    void testGetConversation_NotFound() {
        // Given
        String conversationId = "invalid-id";
        when(conversationRepository.findById(conversationId))
            .thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(conversationService.getConversation(conversationId))
            .expectError(ConversationNotFoundException.class)
            .verify();
    }
    
    @Test
    @DisplayName("Suppression d'une conversation")
    void testDeleteConversation_Success() {
        // Given
        String conversationId = "conv-789";
        ConversationEntity entity = ConversationEntity.builder()
            .id(conversationId)
            .build();
        
        when(conversationRepository.findById(conversationId))
            .thenReturn(Mono.just(entity));
        
        when(conversationRepository.deleteById(conversationId))
            .thenReturn(Mono.empty());
        
        when(memoryService.clearMemory(conversationId))
            .thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(conversationService.deleteConversation(conversationId))
            .verifyComplete();
        
        verify(conversationRepository, times(1)).deleteById(conversationId);
        verify(memoryService, times(1)).clearMemory(conversationId);
    }
}

// ============================================================================
// TESTS LLM SERVICE
// ============================================================================

/**
 * Tests pour LLMService
 */
@SpringBootTest
@ActiveProfiles("test")
class LLMServiceTest {
    
    @MockBean
    private OpenAIProvider openAIProvider;
    
    @MockBean
    private AnthropicProvider anthropicProvider;
    
    @MockBean
    private PromptBuilder promptBuilder;
    
    @Autowired
    private LLMService llmService;
    
    @Test
    @DisplayName("Génération de réponse avec OpenAI")
    void testGenerateResponse_OpenAI_Success() {
        // Given
        String conversationId = "conv-123";
        MessageEntity userMessage = MessageEntity.builder()
            .content("Comment vas-tu?")
            .sender("USER")
            .build();
        
        ConversationContext context = ConversationContext.builder()
            .companionEmotionalState("NEUTRAL")
            .build();
        
        when(promptBuilder.buildSystemPrompt(any(), any()))
            .thenReturn("Tu es un compagnon IA...");
        
        LLMResponse response = LLMResponse.builder()
            .content("Je vais bien, merci!")
            .provider("openai")
            .build();
        
        when(openAIProvider.complete(any(), anyInt(), anyDouble()))
            .thenReturn(Mono.just(response));
        
        // When & Then
        StepVerifier.create(
            llmService.generateResponse(conversationId, userMessage, context)
        )
        .assertNext(result -> {
            assertThat(result.getContent()).isEqualTo("Je vais bien, merci!");
            assertThat(result.getProvider()).isEqualTo("openai");
        })
        .verifyComplete();
    }
    
    @Test
    @DisplayName("Fallback vers Anthropic si OpenAI échoue")
    void testGenerateResponse_Fallback_ToAnthropic() {
        // Given
        when(openAIProvider.complete(any(), anyInt(), anyDouble()))
            .thenReturn(Mono.error(new RuntimeException("OpenAI API error")));
        
        LLMResponse anthropicResponse = LLMResponse.builder()
            .content("Réponse de Claude")
            .provider("anthropic")
            .build();
        
        when(anthropicProvider.complete(any(), anyInt(), anyDouble()))
            .thenReturn(Mono.just(anthropicResponse));
        
        // When & Then
        StepVerifier.create(
            llmService.generateResponse("conv-123", new MessageEntity(), new ConversationContext())
        )
        .assertNext(result -> {
            assertThat(result.getProvider()).isEqualTo("anthropic");
        })
        .verifyComplete();
    }
}

// ============================================================================
// TESTS MÉMOIRE SERVICE
// ============================================================================

/**
 * Tests pour MemoryService
 */
@SpringBootTest
@ActiveProfiles("test")
class MemoryServiceTest {
    
    @MockBean
    private ShortTermMemoryService shortTermMemory;
    
    @MockBean
    private LongTermMemoryService longTermMemory;
    
    @MockBean
    private EmbeddingService embeddingService;
    
    @Autowired
    private MemoryService memoryService;
    
    @Test
    @DisplayName("Ajout d'un message à la mémoire")
    void testAddMessage_Success() {
        // Given
        String conversationId = "conv-123";
        MessageEntity message = MessageEntity.builder()
            .id("msg-456")
            .content("Test message")
            .type("TEXT")
            .build();
        
        float[] embedding = new float[1536];
        
        when(shortTermMemory.addMessage(conversationId, message))
            .thenReturn(Mono.just(message));
        
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(Mono.just(embedding));
        
        when(longTermMemory.storeMessage(conversationId, message, embedding))
            .thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(memoryService.addMessage(conversationId, message))
            .verifyComplete();
        
        verify(shortTermMemory, times(1)).addMessage(conversationId, message);
        verify(embeddingService, times(1)).generateEmbedding(anyString());
        verify(longTermMemory, times(1)).storeMessage(any(), any(), any());
    }
    
    @Test
    @DisplayName("Récupération du contexte enrichi")
    void testGetContextForResponse_Success() {
        // Given
        String conversationId = "conv-123";
        String currentMessage = "Parlons de voyage";
        
        List<MessageEntity> recentMessages = List.of(
            MessageEntity.builder().content("Message 1").build(),
            MessageEntity.builder().content("Message 2").build()
        );
        
        when(shortTermMemory.getRecentMessages(conversationId, 20))
            .thenReturn(Mono.just(recentMessages));
        
        when(embeddingService.generateEmbedding(currentMessage))
            .thenReturn(Mono.just(new float[1536]));
        
        when(longTermMemory.searchSimilar(any(), any(), anyInt()))
            .thenReturn(Flux.empty());
        
        // When & Then
        StepVerifier.create(
            memoryService.getContextForResponse(conversationId, currentMessage)
        )
        .assertNext(context -> {
            assertThat(context.getRecentMessages()).hasSize(2);
        })
        .verifyComplete();
    }
}

// ============================================================================
// TESTS D'INTÉGRATION MONGODB
// ============================================================================

/**
 * Tests d'intégration avec MongoDB réel
 */
@SpringBootTest
@ActiveProfiles("test")
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
    @DisplayName("Sauvegarde et récupération d'une conversation")
    void testSaveAndFind_Success() {
        // Given
        ConversationEntity entity = ConversationEntity.builder()
            .userId("user-123")
            .companionId("companion-456")
            .title("Test Integration")
            .messages(new ArrayList<>())
            .createdAt(Instant.now())
            .build();
        
        // When
        ConversationEntity saved = repository.save(entity).block();
        
        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        
        ConversationEntity found = repository.findById(saved.getId()).block();
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Test Integration");
    }
    
    @Test
    @DisplayName("Recherche par userId")
    void testFindByUserId_Success() {
        // Given
        ConversationEntity conv1 = ConversationEntity.builder()
            .userId("user-123")
            .companionId("companion-1")
            .messages(new ArrayList<>())
            .createdAt(Instant.now())
            .lastMessageAt(Instant.now())
            .build();
        
        ConversationEntity conv2 = ConversationEntity.builder()
            .userId("user-123")
            .companionId("companion-2")
            .messages(new ArrayList<>())
            .createdAt(Instant.now().minusSeconds(3600))
            .lastMessageAt(Instant.now().minusSeconds(3600))
            .build();
        
        repository.save(conv1).block();
        repository.save(conv2).block();
        
        // When
        List<ConversationEntity> results = repository
            .findByUserIdOrderByLastMessageAtDesc("user-123")
            .collectList()
            .block();
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCompanionId()).isEqualTo("companion-1"); // Plus récent
    }
}

// ============================================================================
// TESTS E2E CONTROLLER
// ============================================================================

/**
 * Tests End-to-End des APIs REST
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConversationControllerE2ETest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private ConversationService conversationService;
    
    @Test
    @DisplayName("POST /api/v1/conversations - Créer conversation")
    void testCreateConversation_E2E() {
        // Given
        CreateConversationRequest request = CreateConversationRequest.builder()
            .userId("user-123")
            .companionId("companion-456")
            .title("E2E Test")
            .build();
        
        ConversationDTO response = ConversationDTO.builder()
            .id("conv-789")
            .userId("user-123")
            .companionId("companion-456")
            .title("E2E Test")
            .messages(new ArrayList<>())
            .createdAt(Instant.now())
            .build();
        
        when(conversationService.createConversation(any()))
            .thenReturn(Mono.just(response));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/conversations")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isEqualTo("conv-789")
            .jsonPath("$.title").isEqualTo("E2E Test");
    }
    
    @Test
    @DisplayName("GET /api/v1/conversations/{id} - Récupérer conversation")
    void testGetConversation_E2E() {
        // Given
        String conversationId = "conv-789";
        ConversationDTO response = ConversationDTO.builder()
            .id(conversationId)
            .userId("user-123")
            .title("Test")
            .build();
        
        when(conversationService.getConversation(conversationId))
            .thenReturn(Mono.just(response));
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/conversations/{id}", conversationId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(conversationId);
    }
    
    @Test
    @DisplayName("GET /api/v1/conversations/{id} - Not Found")
    void testGetConversation_NotFound_E2E() {
        // Given
        String conversationId = "invalid-id";
        when(conversationService.getConversation(conversationId))
            .thenReturn(Mono.error(new ConversationNotFoundException(conversationId)));
        
        // When & Then
        webTestClient.get()
            .uri("/api/v1/conversations/{id}", conversationId)
            .exchange()
            .expectStatus().isNotFound();
    }
    
    @Test
    @DisplayName("POST /api/v1/conversations/{id}/messages - Envoyer message")
    void testSendMessage_E2E() {
        // Given
        String conversationId = "conv-789";
        SendMessageRequest request = SendMessageRequest.builder()
            .content("Bonjour!")
            .type(MessageType.TEXT)
            .build();
        
        MessageDTO response = MessageDTO.builder()
            .id("msg-123")
            .sender(MessageSender.COMPANION)
            .content("Bonjour! Comment puis-je t'aider?")
            .timestamp(Instant.now())
            .build();
        
        when(conversationService.sendMessage(any()))
            .thenReturn(Mono.just(response));
        
        // When & Then
        webTestClient.post()
            .uri("/api/v1/conversations/{id}/messages", conversationId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.sender").isEqualTo("COMPANION")
            .jsonPath("$.content").isNotEmpty();
    }
}

// ============================================================================
// TESTS DE PERFORMANCE
// ============================================================================

/**
 * Tests de charge et performance
 */
@SpringBootTest
@ActiveProfiles("test")
class PerformanceTest {
    
    @Autowired
    private ConversationService conversationService;
    
    @Test
    @DisplayName("Test de charge - 100 conversations simultanées")
    @Disabled("À exécuter manuellement")
    void testLoadTest_100ConcurrentConversations() {
        // Given
        int numberOfConversations = 100;
        
        // When
        long startTime = System.currentTimeMillis();
        
        List<Mono<ConversationDTO>> requests = new ArrayList<>();
        for (int i = 0; i < numberOfConversations; i++) {
            CreateConversationRequest request = CreateConversationRequest.builder()
                .userId("user-" + i)
                .companionId("companion-1")
                .title("Load Test " + i)
                .build();
            
            requests.add(conversationService.createConversation(request));
        }
        
        Flux.merge(requests).collectList().block();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then
        System.out.println("Created " + numberOfConversations + " conversations in " + duration + "ms");
        assertThat(duration).isLessThan(10000); // Moins de 10 secondes
    }
}
