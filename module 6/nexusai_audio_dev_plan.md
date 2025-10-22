# PLAN DE DÉVELOPPEMENT DÉTAILLÉ - MODULE 6 : AUDIO PROCESSING

**Version:** 1.0  
**Date:** 20 Octobre 2025  
**Durée estimée:** 5 semaines  
**Équipe:** 6 développeurs Java

---

## 📋 TABLE DES MATIÈRES

1. [Vue d'ensemble](#vue-densemble)
2. [Répartition des tâches](#répartition-des-tâches)
3. [Planning hebdomadaire](#planning-hebdomadaire)
4. [Guide de démarrage](#guide-de-démarrage)
5. [Conventions de code](#conventions-de-code)
6. [Tests et validation](#tests-et-validation)
7. [Déploiement](#déploiement)

---

## 🎯 VUE D'ENSEMBLE

### Objectif du Module

Le Module 6 - Audio Processing gère tous les aspects audio de NexusAI :
- Messages vocaux asynchrones (upload, transcription, stockage)
- Appels vocaux en temps réel via WebRTC
- Synthèse vocale personnalisée pour les compagnons IA
- Analyse émotionnelle des messages vocaux

### Architecture Modulaire

```
nexus-audio/
├── nexus-audio-api           ← Interface REST/WebSocket
├── nexus-audio-core          ← Logique métier
├── nexus-audio-stt           ← Transcription (Whisper)
├── nexus-audio-tts           ← Synthèse vocale (ElevenLabs)
├── nexus-audio-webrtc        ← Gestion appels temps réel
├── nexus-audio-storage       ← Stockage S3/MinIO
├── nexus-audio-emotion       ← Analyse émotionnelle
└── nexus-audio-persistence   ← Base de données JPA
```

### Stack Technique

- **Backend:** Java 21, Spring Boot 3.2+
- **Base de données:** PostgreSQL 16
- **Stockage:** MinIO (compatible S3)
- **Messaging:** Kafka
- **APIs externes:** OpenAI Whisper, ElevenLabs TTS
- **Build:** Maven multi-module
- **Conteneurs:** Docker, Docker Compose

---

## 👥 RÉPARTITION DES TÂCHES

### DÉVELOPPEUR 1 : API & Configuration
**Nom du module:** `nexus-audio-api`  
**Durée:** 2 semaines

#### Responsabilités
- Controllers REST (VoiceMessage, VoiceCall, VoiceProfile)
- DTOs (Request/Response)
- Configuration Spring Security
- Configuration WebSocket
- Documentation OpenAPI/Swagger
- Gestion des exceptions globales

#### Fichiers à créer
```
nexus-audio-api/
├── src/main/java/com/nexusai/audio/api/
│   ├── AudioApplication.java
│   ├── controller/
│   │   ├── VoiceMessageController.java
│   │   ├── VoiceCallController.java
│   │   └── VoiceProfileController.java
│   ├── dto/
│   │   ├── VoiceMessageRequest.java
│   │   ├── VoiceMessageResponse.java
│   │   ├── VoiceCallRequest.java
│   │   ├── VoiceCallResponse.java
│   │   ├── VoiceProfileRequest.java
│   │   └── VoiceProfileResponse.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebSocketConfig.java
│   │   └── RestTemplateConfig.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── ErrorResponse.java
└── src/main/resources/
    └── application.yml
```

#### Critères de succès
- ✅ Tous les endpoints REST fonctionnels
- ✅ Documentation Swagger complète
- ✅ Tests unitaires des controllers (>80% coverage)
- ✅ Validation des DTOs avec `@Valid`

---

### DÉVELOPPEUR 2 : Logique Métier & Persistence
**Modules:** `nexus-audio-core` + `nexus-audio-persistence`  
**Durée:** 2 semaines

#### Responsabilités
- Services métier (VoiceMessage, VoiceCall, VoiceProfile)
- Modèles de domaine
- Entités JPA
- Repositories
- Mappers MapStruct
- Événements Kafka

#### Fichiers à créer
```
nexus-audio-core/
├── src/main/java/com/nexusai/audio/core/
│   ├── domain/
│   │   ├── VoiceMessage.java
│   │   ├── VoiceCall.java
│   │   └── VoiceProfile.java
│   ├── service/
│   │   ├── VoiceMessageService.java
│   │   ├── VoiceCallService.java
│   │   └── VoiceProfileService.java
│   └── exception/
│       ├── AudioProcessingException.java
│       ├── TranscriptionException.java
│       └── SynthesisException.java

nexus-audio-persistence/
├── src/main/java/com/nexusai/audio/persistence/
│   ├── entity/
│   │   ├── VoiceMessageEntity.java
│   │   ├── VoiceCallEntity.java
│   │   └── VoiceProfileEntity.java
│   ├── repository/
│   │   ├── VoiceMessageRepository.java
│   │   ├── VoiceCallRepository.java
│   │   └── VoiceProfileRepository.java
│   └── mapper/
│       ├── VoiceMessageMapper.java
│       ├── VoiceCallMapper.java
│       └── VoiceProfileMapper.java
└── src/main/resources/db/migration/
    └── V1__create_voice_tables.sql
```

#### Critères de succès
- ✅ Services métier complets avec Javadoc
- ✅ Transactions gérées correctement
- ✅ Événements Kafka publiés
- ✅ Tests unitaires des services (>85% coverage)
- ✅ Migrations Flyway fonctionnelles

---

### DÉVELOPPEUR 3 : Speech-to-Text
**Module:** `nexus-audio-stt`  
**Durée:** 1.5 semaines

#### Responsabilités
- Intégration OpenAI Whisper API
- Service de transcription
- Gestion des formats audio
- Cache des transcriptions
- Gestion des quotas API

#### Fichiers à créer
```
nexus-audio-stt/
├── src/main/java/com/nexusai/audio/stt/
│   ├── client/
│   │   └── OpenAIWhisperClient.java
│   ├── service/
│   │   ├── WhisperSTTService.java
│   │   └── STTServiceFactory.java
│   ├── model/
│   │   └── TranscriptionResult.java
│   └── config/
│       └── WhisperConfig.java
└── src/test/java/
    └── com/nexusai/audio/stt/
        ├── WhisperSTTServiceTest.java
        └── OpenAIWhisperClientTest.java
```

#### Configuration nécessaire
```yaml
openai:
  api-key: ${OPENAI_API_KEY}
  whisper:
    url: https://api.openai.com/v1/audio/transcriptions
    model: whisper-1
    timeout: 30s
```

#### Critères de succès
- ✅ Transcription fonctionnelle pour MP3, WAV, M4A
- ✅ Détection automatique de la langue
- ✅ Gestion des erreurs API robuste
- ✅ Tests d'intégration avec l'API réelle
- ✅ Temps de réponse < 5s pour fichiers < 1MB

---

### DÉVELOPPEUR 4 : Text-to-Speech
**Module:** `nexus-audio-tts`  
**Durée:** 1.5 semaines

#### Responsabilités
- Intégration ElevenLabs API
- Service de synthèse vocale
- Personnalisation des voix
- Cache des audios générés
- Factory pour multi-providers

#### Fichiers à créer
```
nexus-audio-tts/
├── src/main/java/com/nexusai/audio/tts/
│   ├── client/
│   │   ├── ElevenLabsClient.java
│   │   └── CoquiClient.java (optionnel)
│   ├── service/
│   │   ├── ElevenLabsTTSService.java
│   │   └── TTSServiceFactory.java
│   ├── model/
│   │   ├── VoiceSettings.java
│   │   └── SynthesisResult.java
│   └── config/
│       └── ElevenLabsConfig.java
└── src/test/java/
    └── com/nexusai/audio/tts/
        └── ElevenLabsTTSServiceTest.java
```

#### Voix ElevenLabs prédéfinies
```java
// Voix populaires à configurer
public static final String RACHEL = "21m00Tcm4TlvDq8ikWAM";
public static final String SARAH = "EXAVITQu4vr4xnSDxMaL";
public static final String ARNOLD = "VR6AewLTigWG4xSOukaG";
```

#### Critères de succès
- ✅ Synthèse vocale fonctionnelle
- ✅ Support multi-voix
- ✅ Personnalisation pitch/speed/style
- ✅ Génération < 3s pour 100 mots
- ✅ Qualité audio excellente

---

### DÉVELOPPEUR 5 : WebRTC & Temps Réel
**Module:** `nexus-audio-webrtc`  
**Durée:** 2.5 semaines

#### Responsabilités
- Gestion sessions WebRTC
- Configuration Janus Gateway
- Signaling WebSocket
- Gestion qualité des appels
- Métriques temps réel

#### Fichiers à créer
```
nexus-audio-webrtc/
├── src/main/java/com/nexusai/audio/webrtc/
│   ├── service/
│   │   ├── WebRTCSessionService.java
│   │   └── JanusGatewayService.java
│   ├── handler/
│   │   └── WebRTCSignalingHandler.java
│   ├── model/
│   │   ├── WebRTCSession.java
│   │   ├── SignalingMessage.java
│   │   └── QualityMetrics.java
│   └── config/
│       ├── WebRTCConfig.java
│       └── JanusConfig.java
```

#### Workflow d'un appel
```
1. Client → POST /api/v1/audio/calls/initiate
2. Server → Crée session WebRTC
3. Client ←→ Server : Signaling WebSocket
4. Client ←→ Janus : Flux audio RTP
5. Client → POST /api/v1/audio/calls/{id}/end
```

#### Critères de succès
- ✅ Sessions WebRTC fonctionnelles
- ✅ Latence audio < 200ms
- ✅ Qualité audio stable (Opus codec)
- ✅ Gestion reconnexions automatiques
- ✅ Métriques de qualité enregistrées

---

### DÉVELOPPEUR 6 : Stockage & Analyse Émotionnelle
**Modules:** `nexus-audio-storage` + `nexus-audio-emotion`  
**Durée:** 2 semaines

#### Responsabilités
- Intégration MinIO/S3
- Upload/Download fichiers audio
- Gestion des buckets
- Analyse émotionnelle (ML)
- Détection des émotions dans la voix

#### Fichiers à créer
```
nexus-audio-storage/
├── src/main/java/com/nexusai/audio/storage/
│   ├── service/
│   │   ├── AudioStorageService.java
│   │   └── S3StorageService.java
│   └── config/
│       └── MinIOConfig.java

nexus-audio-emotion/
├── src/main/java/com/nexusai/audio/emotion/
│   ├── service/
│   │   └── EmotionAnalysisService.java
│   ├── model/
│   │   ├── EmotionResult.java
│   │   └── EmotionType.java
│   └── ml/
│       └── EmotionModelLoader.java
```

#### Configuration MinIO
```yaml
minio:
  url: http://localhost:9000
  access-key: nexusai
  secret-key: nexusai123
  bucket-name: nexusai-audio
```

#### Émotions détectées
- NEUTRAL (Neutre)
- HAPPY (Joyeux)
- SAD (Triste)
- ANGRY (En colère)
- ANXIOUS (Anxieux)
- EXCITED (Excité)

#### Critères de succès
- ✅ Upload/Download audio fonctionnels
- ✅ Bucket auto-créé si inexistant
- ✅ URLs publiques générées
- ✅ Analyse émotionnelle basique implémentée
- ✅ Tests avec fichiers audio réels

---

## 📅 PLANNING HEBDOMADAIRE

### **SEMAINE 1 : Infrastructure & Setup**

#### Tous les développeurs
- [x] Cloner le repository
- [x] Configurer l'environnement de développement
- [x] Installer Docker et Docker Compose
- [x] Obtenir les clés API (OpenAI, ElevenLabs)

#### Dev 1 (API)
- [ ] Créer la structure Maven multi-module
- [ ] Configurer le POM parent
- [ ] Créer `nexus-audio-api` avec Spring Boot
- [ ] Configurer application.yml
- [ ] Implémenter les DTOs

#### Dev 2 (Core)
- [ ] Créer `nexus-audio-core`
- [ ] Créer `nexus-audio-persistence`
- [ ] Définir les modèles de domaine
- [ ] Créer les entités JPA
- [ ] Script SQL de migration Flyway

#### Dev 3 (STT)
- [ ] Créer `nexus-audio-stt`
- [ ] Configuration OpenAI API
- [ ] Implémenter `OpenAIWhisperClient`

#### Dev 4 (TTS)
- [ ] Créer `nexus-audio-tts`
- [ ] Configuration ElevenLabs API
- [ ] Implémenter `ElevenLabsClient`

#### Dev 5 (WebRTC)
- [ ] Créer `nexus-audio-webrtc`
- [ ] Recherche sur Janus Gateway
- [ ] POC WebSocket signaling

#### Dev 6 (Storage)
- [ ] Créer `nexus-audio-storage`
- [ ] Créer `nexus-audio-emotion`
- [ ] Configuration MinIO local

**Livrable fin semaine 1:** Infrastructure complète, tous les modules créés

---

### **SEMAINE 2 : Implémentation Core**

#### Dev 1 (API)
- [ ] Implémenter `VoiceMessageController`
- [ ] Implémenter `VoiceCallController`
- [ ] Implémenter `VoiceProfileController`
- [ ] Configuration Swagger/OpenAPI

#### Dev 2 (Core)
- [ ] Implémenter `VoiceMessageService`
- [ ] Implémenter `VoiceCallService`
- [ ] Implémenter `VoiceProfileService`
- [ ] Créer les repositories JPA
- [ ] Implémenter les mappers MapStruct

#### Dev 3 (STT)
- [ ] Implémenter `WhisperSTTService`
- [ ] Gestion des différents formats audio
- [ ] Tests d'intégration Whisper

#### Dev 4 (TTS)
- [ ] Implémenter `ElevenLabsTTSService`
- [ ] Système de VoiceSettings
- [ ] Tests de synthèse vocale

#### Dev 5 (WebRTC)
- [ ] Implémenter `WebRTCSessionService`
- [ ] Configuration basique Janus
- [ ] Tests de création de sessions

#### Dev 6 (Storage)
- [ ] Implémenter `AudioStorageService`
- [ ] Upload/Download MinIO
- [ ] Tests de stockage

**Livrable fin semaine 2:** Services core fonctionnels, API testable

---

### **SEMAINE 3 : Intégration & Tests**

#### Dev 1 (API)
- [ ] Tests unitaires controllers
- [ ] Documentation API complète
- [ ] Gestion des erreurs globale

#### Dev 2 (Core)
- [ ] Tests unitaires services
- [ ] Publication événements Kafka
- [ ] Tests d'intégration base de données

#### Dev 3 (STT)
- [ ] Optimisation performances
- [ ] Cache des transcriptions
- [ ] Tests de charge

#### Dev 4 (TTS)
- [ ] Factory multi-providers
- [ ] Cache des audios générés
- [ ] Tests qualité audio

#### Dev 5 (WebRTC)
- [ ] Implémentation signaling complet
- [ ] Tests d'appels bout-en-bout
- [ ] Métriques de qualité

#### Dev 6 (Storage + Emotion)
- [ ] Implémentation analyse émotionnelle
- [ ] Intégration avec VoiceMessageService
- [ ] Tests émotions

**Livrable fin semaine 3:** Module complet et testé

---

### **SEMAINE 4 : Polissage & Documentation**

#### Tous
- [ ] Revue de code croisée
- [ ] Correction des bugs
- [ ] Optimisation performances
- [ ] Tests E2E complets
- [ ] Documentation technique complète

#### Spécifiques
- [ ] Configuration Docker Compose finale
- [ ] Dockerfile optimisé
- [ ] Scripts de déploiement
- [ ] README.md complet

**Livrable fin semaine 4:** Module production-ready

---

### **SEMAINE 5 : Déploiement & Formation**

- [ ] Déploiement en environnement staging
- [ ] Tests de charge (1000 utilisateurs simultanés)
- [ ] Documentation utilisateur
- [ ] Formation équipe support
- [ ] Déploiement production

**Livrable fin semaine 5:** Module en production

---

## 🚀 GUIDE DE DÉMARRAGE

### Prérequis

1. **Java Development Kit 21**
   ```bash
   java -version
   # openjdk version "21.0.1"
   ```

2. **Maven 3.9+**
   ```bash
   mvn -version
   # Apache Maven 3.9.5
   ```

3. **Docker & Docker Compose**
   ```bash
   docker --version
   docker-compose --version
   ```

4. **Clés API**
   - OpenAI API Key : https://platform.openai.com/api-keys
   - ElevenLabs API Key : https://elevenlabs.io/

### Installation

1. **Cloner le repository**
   ```bash
   git clone https://github.com/nexusai/nexus-audio.git
   cd nexus-audio
   ```

2. **Configurer les variables d'environnement**
   ```bash
   # Créer un fichier .env à la racine
   cat > .env << EOF
   OPENAI_API_KEY=sk-...
   ELEVENLABS_API_KEY=...
   EOF
   ```

3. **Démarrer les services Docker**
   ```bash
   docker-compose up -d
   ```

4. **Vérifier que tout fonctionne**
   ```bash
   # PostgreSQL
   docker exec -it nexusai-audio-postgres psql -U nexusai -d nexusai
   
   # MinIO (ouvrir http://localhost:9001)
   # Login: nexusai / nexusai123
   
   # Kafka
   docker exec -it nexusai-audio-kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

5. **Compiler et lancer l'application**
   ```bash
   mvn clean install
   mvn spring-boot:run -pl nexus-audio-api
   ```

6. **Tester l'API**
   ```bash
   curl http://localhost:8083/actuator/health
   # {"status":"UP"}
   ```

---

## 📝 CONVENTIONS DE CODE

### Naming Conventions

#### Classes
```java
// Services
public class VoiceMessageService { }

// Controllers
@RestController
public class VoiceMessageController { }

// Entities
@Entity
@Table(name = "voice_messages")
public class VoiceMessageEntity { }

// DTOs
public class VoiceMessageRequest { }
public class VoiceMessageResponse { }
```

#### Méthodes
```java
// CRUD operations
public VoiceMessage createVoiceMessage(...)
public VoiceMessage getVoiceMessageById(UUID id)
public List<VoiceMessage> getVoiceMessagesByConversation(String conversationId)
public VoiceMessage updateVoiceMessage(...)
public void deleteVoiceMessage(UUID id)

// Business logic
public TranscriptionResult transcribe(MultipartFile audioFile)
public SynthesisResult synthesize(String text, String voiceId)
```

### Javadoc Obligatoire

Toutes les classes et méthodes publiques doivent être documentées :

```java
/**
 * Service de gestion des messages vocaux.
 * 
 * <p>Ce service orchestre le traitement complet d'un message vocal :</p>
 * <ul>
 *   <li>Upload du fichier audio vers le stockage</li>
 *   <li>Transcription du contenu audio en texte</li>
 *   <li>Analyse émotionnelle du message</li>
 *   <li>Sauvegarde en base de données</li>
 *   <li>Publication d'événements Kafka</li>
 * </ul>
 * 
 * <p><strong>Usage :</strong></p>
 * <pre>{@code
 * VoiceMessage message = voiceMessageService.createVoiceMessage(
 *     audioFile,
 *     conversationId,
 *     userId,
 *     VoiceMessage.SenderType.USER
 * );
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Service
public class VoiceMessageService {
    
    /**
     * Crée un nouveau message vocal à partir d'un fichier audio.
     * 
     * @param audioFile Fichier audio à traiter
     * @param conversationId ID de la conversation
     * @param userId ID de l'utilisateur
     * @param senderType Type d'émetteur (USER ou COMPANION)
     * @return Le message vocal créé
     * @throws AudioProcessingException Si une erreur survient lors du traitement
     */
    public VoiceMessage createVoiceMessage(...) {
        // Implementation
    }
}
```

### Logging

```java
@Slf4j
@Service
public class VoiceMessageService {
    
    public VoiceMessage createVoiceMessage(...) {
        log.info("Création d'un message vocal pour conversationId={}, userId={}",
                conversationId, userId);
        
        try {
            // Business logic
            log.debug("Upload du fichier audio vers le stockage");
            // ...
            
            log.info("Message vocal créé avec succès : id={}", savedMessage.getId());
            return savedMessage;
            
        } catch (Exception e) {
            log.error("Erreur lors de la création du message vocal", e);
            throw new AudioProcessingException("Échec de la création", e);
        }
    }
}
```

### Gestion des Exceptions

```java
// Exception personnalisée
public class AudioProcessingException extends RuntimeException {
    public AudioProcessingException(String message) {
        super(message);
    }
    
    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Global Exception Handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AudioProcessingException.class)
    public ResponseEntity<ErrorResponse> handleAudioProcessingException(
            AudioProcessingException ex) {
        
        ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
```

---

## ✅ TESTS ET VALIDATION

### Tests Unitaires

Objectif : **>80% de couverture de code**

```java
@ExtendWith(MockitoExtension.class)
class VoiceMessageServiceTest {
    
    @Mock
    private VoiceMessageRepository voiceMessageRepository;
    
    @Mock
    private AudioStorageService audioStorageService;
    
    @Mock
    private WhisperSTTService whisperSTTService;
    
    @InjectMocks
    private VoiceMessageService voiceMessageService;
    
    @Test
    void createVoiceMessage_ShouldSucceed() {
        // Given
        MultipartFile audioFile = createMockAudioFile();
        // ...
        
        // When
        VoiceMessage result = voiceMessageService.createVoiceMessage(...);
        
        // Then
        assertNotNull(result.getId());
        assertEquals("conv-123", result.getConversationId());
        verify(audioStorageService).uploadAudio(any(), anyString());
    }
}
```

### Tests d'Intégration

```java
@SpringBootTest
@Testcontainers
class VoiceMessageIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private VoiceMessageService voiceMessageService;
    
    @Test
    void fullWorkflow_ShouldWork() {
        // Test complet du workflow
    }
}
```

### Tests E2E

```bash
# Utiliser REST Assured
mvn test -Dtest=VoiceMessageE2ETest
```

---

## 🚢 DÉPLOIEMENT

### Build de Production

```bash
# Compiler tous les modules
mvn clean package -DskipTests

# Construire l'image Docker
docker build -t nexusai/audio-service:1.0.0 .

# Pousser vers le registry
docker push nexusai/audio-service:1.0.0
```

### Déploiement Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: audio-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: audio-service
  template:
    metadata:
      labels:
        app: audio-service
    spec:
      containers:
      - name: audio-service
        image: nexusai/audio-service:1.0.0
        ports:
        - containerPort: 8083
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
```

---

## 📊 CRITÈRES DE SUCCÈS

### Critères Techniques

- ✅ 100% des tests passent
- ✅ Couverture de code >80%
- ✅ Temps de réponse API <100ms (P95)
- ✅ Transcription <5s pour fichiers <1MB
- ✅ Synthèse vocale <3s pour 100 mots
- ✅ Latence appels WebRTC <200ms
- ✅ 0 faille de sécurité critique (OWASP)

### Critères Fonctionnels

- ✅ Upload message vocal fonctionnel
- ✅ Transcription automatique opérationnelle
- ✅ Synthèse vocale de qualité
- ✅ Appels vocaux stables
- ✅ Profils vocaux personnalisables
- ✅ Stockage fiable (S3/MinIO)

### Critères de Performance

- ✅ Support 1000 utilisateurs simultanés
- ✅ Upload fichiers jusqu'à 25MB
- ✅ 99.9% de disponibilité
- ✅ RTO < 1 minute (Recovery Time Objective)

---

## 📞 SUPPORT & CONTACT

### Leads Techniques

- **Tech Lead Module Audio:** [Nom]
- **Architecte Système:** [Nom]

### Communication

- **Slack:** #module-audio
- **Daily Standup:** 9h30 tous les jours
- **Code Review:** Obligatoire avant merge

### Ressources

- **Documentation API:** http://localhost:8083/swagger-ui.html
- **Wiki Confluence:** [Lien]
- **Jira Board:** [Lien]

---

**Bonne chance à toute l'équipe ! 🚀**

*Document maintenu par l'équipe NexusAI - Dernière mise à jour : 20 Octobre 2025*
