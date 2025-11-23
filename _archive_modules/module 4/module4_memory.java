package com.nexusai.conversation.memory;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.*;

/**
 * MODULE: conversation-memory
 * 
 * Gère le système de mémoire court et long terme des conversations
 * 
 * DÉVELOPPEUR ASSIGNÉ: Développeur 4
 * 
 * TÂCHES:
 * - Implémenter mémoire court terme (Redis)
 * - Implémenter mémoire long terme (Vector DB)
 * - Créer le système de recherche sémantique
 * - Gérer les embeddings
 * - Implémenter l'extraction de mémoires clés
 * 
 * Architecture:
 * 
 * MÉMOIRE COURT TERME (Redis):
 * - Stocke les 20 derniers messages
 * - TTL: 24 heures
 * - Accès ultra-rapide
 * 
 * MÉMOIRE LONG TERME (Vector DB - Pinecone):
 * - Stocke tous les messages sous forme d'embeddings
 * - Recherche sémantique
 * - Persistance illimitée
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */

// ============================================================================
// SERVICE PRINCIPAL DE MÉMOIRE
// ============================================================================

/**
 * Service orchestrant mémoire court et long terme
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryService {
    
    private final ShortTermMemoryService shortTermMemory;
    private final LongTermMemoryService longTermMemory;
    private final EmbeddingService embeddingService;
    private final MemoryExtractionService extractionService;
    
    /**
     * Ajoute un message à la mémoire
     * 
     * Processus:
     * 1. Ajouter à la mémoire court terme (Redis)
     * 2. Générer l'embedding du message
     * 3. Stocker dans la mémoire long terme (Vector DB)
     * 4. Extraire les informations importantes si nécessaire
     * 
     * @param conversationId ID de la conversation
     * @param message Message à mémoriser
     * @return Mono<Void>
     */
    public Mono<Void> addMessage(String conversationId, MessageEntity message) {
        log.debug("Ajout message à la mémoire: conversation={}, messageId={}", 
                  conversationId, message.getId());
        
        return Mono.just(message)
            // 1. Ajouter à mémoire court terme
            .flatMap(msg -> shortTermMemory.addMessage(conversationId, msg))
            
            // 2. Générer embedding (si message textuel)
            .flatMap(msg -> {
                if ("TEXT".equals(msg.getType())) {
                    return embeddingService.generateEmbedding(msg.getContent())
                        .map(embedding -> Map.entry(msg, embedding));
                }
                return Mono.just(Map.entry(msg, new float[0]));
            })
            
            // 3. Stocker en mémoire long terme
            .flatMap(entry -> {
                if (entry.getValue().length > 0) {
                    return longTermMemory.storeMessage(
                        conversationId,
                        entry.getKey(),
                        entry.getValue()
                    ).thenReturn(entry.getKey());
                }
                return Mono.just(entry.getKey());
            })
            
            // 4. Extraire informations clés si c'est un message utilisateur
            .flatMap(msg -> {
                if ("USER".equals(msg.getSender())) {
                    return extractionService.extractKeyInformation(
                        conversationId, msg.getContent());
                }
                return Mono.empty();
            })
            
            .then()
            .doOnSuccess(v -> log.debug("Message ajouté à la mémoire avec succès"))
            .doOnError(error -> log.error("Erreur ajout mémoire", error));
    }
    
    /**
     * Récupère le contexte pour générer une réponse
     * 
     * Combine:
     * - Les derniers messages (court terme)
     * - Les souvenirs pertinents (long terme via recherche sémantique)
     * 
     * @param conversationId ID de la conversation
     * @param currentMessage Message actuel de l'utilisateur
     * @return Mono contenant le contexte enrichi
     */
    public Mono<EnrichedContext> getContextForResponse(
            String conversationId,
            String currentMessage) {
        
        log.debug("Récupération contexte enrichi pour conversation: {}", conversationId);
        
        return Mono.zip(
            // Mémoire court terme
            shortTermMemory.getRecentMessages(conversationId, 20),
            
            // Mémoire long terme (recherche sémantique)
            searchRelevantMemories(conversationId, currentMessage, 5)
        )
        .map(tuple -> EnrichedContext.builder()
            .recentMessages(tuple.getT1())
            .relevantMemories(tuple.getT2())
            .build())
        .doOnSuccess(ctx -> log.debug("Contexte enrichi récupéré: {} messages récents, {} souvenirs",
                                     ctx.getRecentMessages().size(),
                                     ctx.getRelevantMemories().size()));
    }
    
    /**
     * Recherche dans la mémoire long terme
     * 
     * @param conversationId ID de la conversation
     * @param query Requête de recherche
     * @param limit Nombre de résultats
     * @return Flux de mémoires pertinentes
     */
    public Flux<MemoryEntry> searchRelevantMemories(
            String conversationId,
            String query,
            int limit) {
        
        return embeddingService.generateEmbedding(query)
            .flatMapMany(embedding -> 
                longTermMemory.searchSimilar(conversationId, embedding, limit));
    }
    
    /**
     * Nettoie toute la mémoire d'une conversation
     * 
     * @param conversationId ID de la conversation
     * @return Mono<Void>
     */
    public Mono<Void> clearMemory(String conversationId) {
        log.info("Nettoyage mémoire pour conversation: {}", conversationId);
        
        return Mono.when(
            shortTermMemory.clear(conversationId),
            longTermMemory.deleteConversation(conversationId)
        );
    }
}

// ============================================================================
// SERVICE MÉMOIRE COURT TERME (REDIS)
// ============================================================================

/**
 * Gère la mémoire court terme dans Redis
 * 
 * Structure Redis:
 * Key: conversation:messages:{conversationId}
 * Type: List (LPUSH/LRANGE)
 * TTL: 24 heures
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShortTermMemoryService {
    
    private final ReactiveRedisTemplate<String, MessageEntity> redisTemplate;
    
    private static final int MAX_SHORT_TERM_MESSAGES = 20;
    private static final Duration TTL = Duration.ofHours(24);
    
    /**
     * Ajoute un message à la mémoire court terme
     */
    public Mono<MessageEntity> addMessage(String conversationId, MessageEntity message) {
        String key = getKey(conversationId);
        
        return redisTemplate.opsForList()
            .leftPush(key, message)
            .flatMap(size -> {
                // Limiter à MAX_SHORT_TERM_MESSAGES
                if (size > MAX_SHORT_TERM_MESSAGES) {
                    return redisTemplate.opsForList()
                        .trim(key, 0, MAX_SHORT_TERM_MESSAGES - 1)
                        .thenReturn(message);
                }
                return Mono.just(message);
            })
            .flatMap(msg -> redisTemplate.expire(key, TTL).thenReturn(msg))
            .doOnSuccess(msg -> log.debug("Message ajouté à Redis: {}", msg.getId()));
    }
    
    /**
     * Récupère les N messages les plus récents
     */
    public Mono<List<MessageEntity>> getRecentMessages(
            String conversationId, 
            int count) {
        
        String key = getKey(conversationId);
        
        return redisTemplate.opsForList()
            .range(key, 0, count - 1)
            .collectList()
            .map(messages -> {
                // Inverser pour avoir l'ordre chronologique
                Collections.reverse(messages);
                return messages;
            });
    }
    
    /**
     * Nettoie la mémoire court terme
     */
    public Mono<Void> clear(String conversationId) {
        return redisTemplate.delete(getKey(conversationId))
            .then();
    }
    
    private String getKey(String conversationId) {
        return "conversation:messages:" + conversationId;
    }
}

// ============================================================================
// SERVICE MÉMOIRE LONG TERME (VECTOR DB)
// ============================================================================

/**
 * Gère la mémoire long terme dans Pinecone Vector Database
 * 
 * Chaque message est stocké comme:
 * - ID: {conversationId}:{messageId}
 * - Vector: embedding 1536 dimensions (OpenAI ada-002)
 * - Metadata: sender, content, timestamp, etc.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LongTermMemoryService {
    
    private final PineconeClient pineconeClient;
    
    @Value("${pinecone.index-name:nexusai-conversations}")
    private String indexName;
    
    @Value("${pinecone.namespace-prefix:conv}")
    private String namespacePrefix;
    
    /**
     * Stocke un message dans Pinecone
     */
    public Mono<Void> storeMessage(
            String conversationId,
            MessageEntity message,
            float[] embedding) {
        
        String id = conversationId + ":" + message.getId();
        String namespace = namespacePrefix + ":" + conversationId;
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversationId", conversationId);
        metadata.put("messageId", message.getId());
        metadata.put("sender", message.getSender());
        metadata.put("content", message.getContent());
        metadata.put("timestamp", message.getTimestamp().toString());
        metadata.put("type", message.getType());
        
        if (message.getDetectedEmotion() != null) {
            metadata.put("emotion", message.getDetectedEmotion());
        }
        
        return Mono.fromCallable(() -> {
            pineconeClient.upsert(
                indexName,
                namespace,
                id,
                embedding,
                metadata
            );
            return null;
        })
        .then()
        .doOnSuccess(v -> log.debug("Message stocké dans Pinecone: {}", id))
        .doOnError(error -> log.error("Erreur stockage Pinecone", error));
    }
    
    /**
     * Recherche les messages similaires
     * 
     * Utilise la similarité cosinus pour trouver les messages
     * les plus pertinents par rapport à la requête
     * 
     * @param conversationId ID de la conversation
     * @param queryEmbedding Embedding de la requête
     * @param topK Nombre de résultats
     * @return Flux de mémoires similaires
     */
    public Flux<MemoryEntry> searchSimilar(
            String conversationId,
            float[] queryEmbedding,
            int topK) {
        
        String namespace = namespacePrefix + ":" + conversationId;
        
        return Mono.fromCallable(() -> 
            pineconeClient.query(
                indexName,
                namespace,
                queryEmbedding,
                topK,
                true // includeMetadata
            )
        )
        .flatMapMany(results -> Flux.fromIterable(results))
        .map(this::convertToMemoryEntry)
        .doOnComplete(() -> log.debug("Recherche Pinecone terminée: {} résultats", topK));
    }
    
    /**
     * Supprime toutes les données d'une conversation
     */
    public Mono<Void> deleteConversation(String conversationId) {
        String namespace = namespacePrefix + ":" + conversationId;
        
        return Mono.fromRunnable(() -> 
            pineconeClient.deleteNamespace(indexName, namespace)
        )
        .then()
        .doOnSuccess(v -> log.info("Conversation supprimée de Pinecone: {}", conversationId));
    }
    
    private MemoryEntry convertToMemoryEntry(PineconeMatch match) {
        Map<String, Object> metadata = match.getMetadata();
        
        return MemoryEntry.builder()
            .messageId((String) metadata.get("messageId"))
            .content((String) metadata.get("content"))
            .sender((String) metadata.get("sender"))
            .timestamp(Instant.parse((String) metadata.get("timestamp")))
            .similarity(match.getScore())
            .emotion((String) metadata.get("emotion"))
            .build();
    }
}

// ============================================================================
// SERVICE D'EMBEDDINGS
// ============================================================================

/**
 * Génère les embeddings pour la recherche sémantique
 * 
 * Utilise OpenAI text-embedding-ada-002 (1536 dimensions)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {
    
    private final OpenAiService openAiService;
    
    // Cache pour éviter les appels répétés
    private final Map<String, float[]> embeddingCache = new LRUCache<>(1000);
    
    /**
     * Génère l'embedding d'un texte
     * 
     * @param text Texte à encoder
     * @return Mono contenant le vecteur d'embedding
     */
    public Mono<float[]> generateEmbedding(String text) {
        // Vérifier le cache
        String cacheKey = getCacheKey(text);
        if (embeddingCache.containsKey(cacheKey)) {
            log.debug("Embedding récupéré du cache");
            return Mono.just(embeddingCache.get(cacheKey));
        }
        
        return Mono.fromCallable(() -> {
            log.debug("Génération embedding pour texte de {} caractères", text.length());
            
            EmbeddingRequest request = EmbeddingRequest.builder()
                .model("text-embedding-ada-002")
                .input(List.of(text))
                .build();
            
            EmbeddingResult result = openAiService.createEmbeddings(request);
            
            List<Double> embedding = result.getData().get(0).getEmbedding();
            
            // Convertir en float[]
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }
            
            // Mettre en cache
            embeddingCache.put(cacheKey, vector);
            
            log.debug("Embedding généré: {} dimensions", vector.length);
            return vector;
        })
        .doOnError(error -> log.error("Erreur génération embedding", error));
    }
    
    /**
     * Génère les embeddings pour plusieurs textes en batch
     */
    public Mono<List<float[]>> generateEmbeddingsBatch(List<String> texts) {
        return Flux.fromIterable(texts)
            .flatMap(this::generateEmbedding)
            .collectList();
    }
    
    private String getCacheKey(String text) {
        // Utiliser un hash pour limiter la taille de la clé
        return String.valueOf(text.hashCode());
    }
}

// ============================================================================
// SERVICE D'EXTRACTION DE MÉMOIRES
// ============================================================================

/**
 * Extrait les informations importantes des messages
 * 
 * Identifie:
 * - Préférences de l'utilisateur
 * - Informations personnelles
 * - Événements importants
 * - Relations et personnes mentionnées
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryExtractionService {
    
    private final LLMService llmService;
    private final KeyMemoryRepository keyMemoryRepository;
    
    /**
     * Extrait les informations clés d'un message
     * 
     * Utilise le LLM pour identifier les informations importantes
     * à mémoriser long terme
     * 
     * @param conversationId ID de la conversation
     * @param messageContent Contenu du message
     * @return Mono<Void>
     */
    public Mono<Void> extractKeyInformation(
            String conversationId,
            String messageContent) {
        
        String prompt = buildExtractionPrompt(messageContent);
        
        return llmService.callLLM(prompt)
            .flatMap(extraction -> {
                if (extraction.hasKeyInformation()) {
                    return saveKeyMemories(conversationId, extraction);
                }
                return Mono.empty();
            })
            .then()
            .doOnSuccess(v -> log.debug("Extraction mémoires terminée"))
            .doOnError(error -> log.error("Erreur extraction", error));
    }
    
    private String buildExtractionPrompt(String message) {
        return """
            Analyse le message suivant et extrais les informations importantes
            à mémoriser sur l'utilisateur (préférences, faits personnels, etc.):
            
            Message: "%s"
            
            Réponds au format JSON:
            {
              "hasKeyInformation": true/false,
              "facts": ["fait 1", "fait 2"],
              "preferences": ["préférence 1"],
              "people": ["personne 1"],
              "events": ["événement 1"]
            }
            """.formatted(message);
    }
    
    private Mono<Void> saveKeyMemories(
            String conversationId,
            MemoryExtraction extraction) {
        
        List<KeyMemory> memories = new ArrayList<>();
        
        extraction.getFacts().forEach(fact -> 
            memories.add(KeyMemory.builder()
                .conversationId(conversationId)
                .type("FACT")
                .content(fact)
                .extractedAt(Instant.now())
                .build()));
        
        extraction.getPreferences().forEach(pref ->
            memories.add(KeyMemory.builder()
                .conversationId(conversationId)
                .type("PREFERENCE")
                .content(pref)
                .extractedAt(Instant.now())
                .build()));
        
        return Flux.fromIterable(memories)
            .flatMap(keyMemoryRepository::save)
            .then();
    }
}

// ============================================================================
// DTOs & MODELS
// ============================================================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class EnrichedContext {
    /**
     * Messages récents (mémoire court terme)
     */
    private List<MessageEntity> recentMessages;
    
    /**
     * Souvenirs pertinents (mémoire long terme)
     */
    private List<MemoryEntry> relevantMemories;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MemoryEntry {
    private String messageId;
    private String content;
    private String sender;
    private Instant timestamp;
    private Double similarity;
    private String emotion;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class KeyMemory {
    private String id;
    private String conversationId;
    private String type; // FACT, PREFERENCE, PERSON, EVENT
    private String content;
    private Instant extractedAt;
}

@Data
class MemoryExtraction {
    private boolean hasKeyInformation;
    private List<String> facts;
    private List<String> preferences;
    private List<String> people;
    private List<String> events;
}

// ============================================================================
// REPOSITORY POUR MÉMOIRES CLÉS
// ============================================================================

/**
 * Repository pour les mémoires clés extraites
 */
public interface KeyMemoryRepository extends ReactiveMongoRepository<KeyMemory, String> {
    
    Flux<KeyMemory> findByConversationId(String conversationId);
    
    Flux<KeyMemory> findByConversationIdAndType(String conversationId, String type);
    
    Mono<Void> deleteByConversationId(String conversationId);
}

// ============================================================================
// PINECONE CLIENT WRAPPER
// ============================================================================

/**
 * Wrapper pour l'API Pinecone
 */
@Service
@Slf4j
public class PineconeClient {
    
    @Value("${pinecone.api-key}")
    private String apiKey;
    
    @Value("${pinecone.environment}")
    private String environment;
    
    private final WebClient webClient;
    
    public PineconeClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    /**
     * Upsert (insert or update) un vecteur
     */
    public void upsert(
            String indexName,
            String namespace,
            String id,
            float[] vector,
            Map<String, Object> metadata) {
        
        Map<String, Object> request = Map.of(
            "vectors", List.of(Map.of(
                "id", id,
                "values", vector,
                "metadata", metadata
            )),
            "namespace", namespace
        );
        
        webClient.post()
            .uri(buildUrl(indexName, "/vectors/upsert"))
            .header("Api-Key", apiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
    
    /**
     * Recherche les vecteurs similaires
     */
    public List<PineconeMatch> query(
            String indexName,
            String namespace,
            float[] queryVector,
            int topK,
            boolean includeMetadata) {
        
        Map<String, Object> request = Map.of(
            "namespace", namespace,
            "topK", topK,
            "includeMetadata", includeMetadata,
            "vector", queryVector
        );
        
        PineconeQueryResponse response = webClient.post()
            .uri(buildUrl(indexName, "/query"))
            .header("Api-Key", apiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PineconeQueryResponse.class)
            .block();
        
        return response != null ? response.getMatches() : List.of();
    }
    
    /**
     * Supprime un namespace complet
     */
    public void deleteNamespace(String indexName, String namespace) {
        Map<String, Object> request = Map.of(
            "namespace", namespace,
            "deleteAll", true
        );
        
        webClient.post()
            .uri(buildUrl(indexName, "/vectors/delete"))
            .header("Api-Key", apiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
    
    private String buildUrl(String indexName, String path) {
        return String.format("https://%s-%s.svc.%s.pinecone.io%s",
            indexName, "default", environment, path);
    }
}

@Data
class PineconeQueryResponse {
    private List<PineconeMatch> matches;
}

@Data
class PineconeMatch {
    private String id;
    private Double score;
    private Map<String, Object> metadata;
}

// ============================================================================
// UTILITY: LRU CACHE
// ============================================================================

/**
 * Cache LRU simple pour les embeddings
 */
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
