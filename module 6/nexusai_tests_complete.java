// ===========================================
// TESTS UNITAIRES - VOICE MESSAGE SERVICE
// ===========================================

// nexus-audio-core/src/test/java/com/nexusai/audio/core/service/VoiceMessageServiceTest.java
package com.nexusai.audio.core.service;

import com.nexusai.audio.core.domain.VoiceMessage;
import com.nexusai.audio.core.exception.AudioProcessingException;
import com.nexusai.audio.emotion.model.EmotionResult;
import com.nexusai.audio.emotion.service.EmotionAnalysisService;
import com.nexusai.audio.persistence.entity.VoiceMessageEntity;
import com.nexusai.audio.persistence.mapper.VoiceMessageMapper;
import com.nexusai.audio.persistence.repository.VoiceMessageRepository;
import com.nexusai.audio.storage.service.AudioStorageService;
import com.nexusai.audio.stt.model.TranscriptionResult;
import com.nexusai.audio.stt.service.WhisperSTTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour VoiceMessageService.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class VoiceMessageServiceTest {
    
    @Mock
    private VoiceMessageRepository voiceMessageRepository;
    
    @Mock
    private VoiceMessageMapper voiceMessageMapper;
    
    @Mock
    private AudioStorageService audioStorageService;
    
    @Mock
    private WhisperSTTService whisperSTTService;
    
    @Mock
    private EmotionAnalysisService emotionAnalysisService;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @InjectMocks
    private VoiceMessageService voiceMessageService;
    
    private MultipartFile mockAudioFile;
    private String conversationId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        conversationId = "conv-123";
        userId = UUID.randomUUID();
        mockAudioFile = mock(MultipartFile.class);
    }
    
    @Test
    void createVoiceMessage_WithValidData_ShouldSucceed() throws Exception {
        // Given
        String audioUrl = "http://storage/audio.mp3";
        String transcription = "Bonjour, comment ça va ?";
        String emotion = "HAPPY";
        
        when(mockAudioFile.getOriginalFilename()).thenReturn("test.mp3");
        when(mockAudioFile.getSize()).thenReturn(1024L);
        when(mockAudioFile.getBytes()).thenReturn(new byte[1024]);
        
        when(audioStorageService.uploadAudio(any(), anyString()))
            .thenReturn(audioUrl);
        
        when(whisperSTTService.transcribe(any(MultipartFile.class)))
            .thenReturn(TranscriptionResult.builder()
                .text(transcription)
                .language("fr")
                .confidence(0.95f)
                .build());
        
        when(emotionAnalysisService.analyzeEmotion(any(MultipartFile.class)))
            .thenReturn(EmotionResult.builder()
                .emotion(emotion)
                .confidence(0.85f)
                .build());
        
        VoiceMessageEntity savedEntity = new VoiceMessageEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setConversationId(conversationId);
        savedEntity.setTranscription(transcription);
        
        when(voiceMessageRepository.save(any(VoiceMessageEntity.class)))
            .thenReturn(savedEntity);
        
        VoiceMessage expectedMessage = VoiceMessage.builder()
            .id(savedEntity.getId())
            .conversationId(conversationId)
            .transcription(transcription)
            .emotionDetected(emotion)
            .build();
        
        when(voiceMessageMapper.toDomain(any(VoiceMessageEntity.class)))
            .thenReturn(expectedMessage);
        
        // When
        VoiceMessage result = voiceMessageService.createVoiceMessage(
            mockAudioFile,
            conversationId,
            userId,
            VoiceMessage.SenderType.USER
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo(conversationId);
        assertThat(result.getTranscription()).isEqualTo(transcription);
        assertThat(result.getEmotionDetected()).isEqualTo(emotion);
        
        verify(audioStorageService).uploadAudio(any(), eq("voice-messages"));
        verify(whisperSTTService).transcribe(any(MultipartFile.class));
        verify(emotionAnalysisService).analyzeEmotion(any(MultipartFile.class));
        verify(voiceMessageRepository).save(any(VoiceMessageEntity.class));
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }
    
    @Test
    void createVoiceMessage_WhenStorageFails_ShouldThrowException() throws Exception {
        // Given
        when(mockAudioFile.getBytes()).thenReturn(new byte[1024]);
        when(audioStorageService.uploadAudio(any(), anyString()))
            .thenThrow(new RuntimeException("Storage error"));
        
        // When / Then
        assertThatThrownBy(() -> voiceMessageService.createVoiceMessage(
                mockAudioFile,
                conversationId,
                userId,
                VoiceMessage.SenderType.USER
            ))
            .isInstanceOf(AudioProcessingException.class)
            .hasMessageContaining("Échec de la création du message vocal");
    }
    
    @Test
    void getVoiceMessageById_WhenExists_ShouldReturnMessage() {
        // Given
        UUID messageId = UUID.randomUUID();
        VoiceMessageEntity entity = new VoiceMessageEntity();
        entity.setId(messageId);
        
        VoiceMessage expectedMessage = VoiceMessage.builder()
            .id(messageId)
            .build();
        
        when(voiceMessageRepository.findById(messageId))
            .thenReturn(Optional.of(entity));
        when(voiceMessageMapper.toDomain(entity))
            .thenReturn(expectedMessage);
        
        // When
        VoiceMessage result = voiceMessageService.getVoiceMessageById(messageId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(messageId);
        
        verify(voiceMessageRepository).findById(messageId);
    }
    
    @Test
    void getVoiceMessageById_WhenNotExists_ShouldThrowException() {
        // Given
        UUID messageId = UUID.randomUUID();
        when(voiceMessageRepository.findById(messageId))
            .thenReturn(Optional.empty());
        
        // When / Then
        assertThatThrownBy(() -> voiceMessageService.getVoiceMessageById(messageId))
            .isInstanceOf(AudioProcessingException.class)
            .hasMessageContaining("Message vocal introuvable");
    }
    
    @Test
    void getVoiceMessagesByConversation_ShouldReturnAllMessages() {
        // Given
        VoiceMessageEntity entity1 = new VoiceMessageEntity();
        entity1.setId(UUID.randomUUID());
        
        VoiceMessageEntity entity2 = new VoiceMessageEntity();
        entity2.setId(UUID.randomUUID());
        
        List<VoiceMessageEntity> entities = List.of(entity1, entity2);
        
        when(voiceMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
            .thenReturn(entities);
        
        when(voiceMessageMapper.toDomain(any(VoiceMessageEntity.class)))
            .thenReturn(VoiceMessage.builder().build());
        
        // When
        List<VoiceMessage> results = voiceMessageService
            .getVoiceMessagesByConversation(conversationId);
        
        // Then
        assertThat(results).hasSize(2);
        verify(voiceMessageRepository).findByConversationIdOrderByCreatedAtAsc(conversationId);
    }
    
    @Test
    void deleteVoiceMessage_ShouldDeleteFileAndRecord() {
        // Given
        UUID messageId = UUID.randomUUID();
        String audioUrl = "http://storage/audio.mp3";
        
        VoiceMessageEntity entity = new VoiceMessageEntity();
        entity.setId(messageId);
        entity.setAudioUrl(audioUrl);
        
        VoiceMessage message = VoiceMessage.builder()
            .id(messageId)
            .audioUrl(audioUrl)
            .build();
        
        when(voiceMessageRepository.findById(messageId))
            .thenReturn(Optional.of(entity));
        when(voiceMessageMapper.toDomain(entity))
            .thenReturn(message);
        
        // When
        voiceMessageService.deleteVoiceMessage(messageId);
        
        // Then
        verify(audioStorageService).deleteAudio(audioUrl);
        verify(voiceMessageRepository).deleteById(messageId);
    }
}

// ===========================================
// TESTS INTÉGRATION - VOICE MESSAGE CONTROLLER
// ===========================================

// nexus-audio-api/src/test/java/com/nexusai/audio/api/controller/VoiceMessageControllerIntegrationTest.java
package com.nexusai.audio.api.controller;

import com.nexusai.audio.api.AudioApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour VoiceMessageController.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@SpringBootTest(classes = AudioApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VoiceMessageControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void uploadVoiceMessage_WithValidFile_ShouldReturn201() throws Exception {
        // Given
        MockMultipartFile audioFile = new MockMultipartFile(
            "audioFile",
            "test.mp3",
            "audio/mpeg",
            "fake audio content".getBytes()
        );
        
        // When / Then
        mockMvc.perform(multipart("/api/v1/audio/voice-messages")
                .file(audioFile)
                .param("conversationId", "conv-123")
                .param("userId", "550e8400-e29b-41d4-a716-446655440000")
                .param("senderType", "USER"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.conversationId").value("conv-123"))
            .andExpect(jsonPath("$.transcription").exists());
    }
    
    @Test
    void uploadVoiceMessage_WithMissingParams_ShouldReturn400() throws Exception {
        // Given
        MockMultipartFile audioFile = new MockMultipartFile(
            "audioFile",
            "test.mp3",
            "audio/mpeg",
            "fake audio content".getBytes()
        );
        
        // When / Then
        mockMvc.perform(multipart("/api/v1/audio/voice-messages")
                .file(audioFile))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void getVoiceMessage_WhenExists_ShouldReturn200() throws Exception {
        // Assuming a message was created first
        String messageId = "550e8400-e29b-41d4-a716-446655440000";
        
        // When / Then
        mockMvc.perform(get("/api/v1/audio/voice-messages/{id}", messageId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(messageId));
    }
    
    @Test
    void getVoiceMessage_WhenNotExists_ShouldReturn404() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        
        // When / Then
        mockMvc.perform(get("/api/v1/audio/voice-messages/{id}", nonExistentId))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getConversationMessages_ShouldReturnList() throws Exception {
        // Given
        String conversationId = "conv-123";
        
        // When / Then
        mockMvc.perform(get("/api/v1/audio/voice-messages/conversation/{conversationId}", 
                conversationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}

// ===========================================
// TESTS REPOSITORY
// ===========================================

// nexus-audio-persistence/src/test/java/com/nexusai/audio/persistence/repository/VoiceMessageRepositoryTest.java
package com.nexusai.audio.persistence.repository;

import com.nexusai.audio.persistence.entity.VoiceMessageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests pour VoiceMessageRepository.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@DataJpaTest
@ActiveProfiles("test")
class VoiceMessageRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private VoiceMessageRepository repository;
    
    private String conversationId;
    private UUID userId;
    
    @BeforeEach
    void setUp() {
        conversationId = "conv-123";
        userId = UUID.randomUUID();
    }
    
    @Test
    void findByConversationIdOrderByCreatedAtAsc_ShouldReturnOrderedMessages() {
        // Given
        VoiceMessageEntity message1 = createMessage(conversationId, userId);
        message1.setCreatedAt(LocalDateTime.now().minusHours(2));
        entityManager.persist(message1);
        
        VoiceMessageEntity message2 = createMessage(conversationId, userId);
        message2.setCreatedAt(LocalDateTime.now().minusHours(1));
        entityManager.persist(message2);
        
        VoiceMessageEntity message3 = createMessage(conversationId, userId);
        message3.setCreatedAt(LocalDateTime.now());
        entityManager.persist(message3);
        
        entityManager.flush();
        
        // When
        List<VoiceMessageEntity> results = repository
            .findByConversationIdOrderByCreatedAtAsc(conversationId);
        
        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(message1.getId());
        assertThat(results.get(1).getId()).isEqualTo(message2.getId());
        assertThat(results.get(2).getId()).isEqualTo(message3.getId());
    }
    
    @Test
    void findByUserIdOrderByCreatedAtDesc_ShouldReturnUserMessages() {
        // Given
        VoiceMessageEntity message1 = createMessage("conv-1", userId);
        entityManager.persist(message1);
        
        VoiceMessageEntity message2 = createMessage("conv-2", userId);
        entityManager.persist(message2);
        
        VoiceMessageEntity otherUserMessage = createMessage("conv-3", UUID.randomUUID());
        entityManager.persist(otherUserMessage);
        
        entityManager.flush();
        
        // When
        List<VoiceMessageEntity> results = repository
            .findByUserIdOrderByCreatedAtDesc(userId);
        
        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(VoiceMessageEntity::getUserId)
            .containsOnly(userId);
    }
    
    @Test
    void countByConversationId_ShouldReturnCorrectCount() {
        // Given
        entityManager.persist(createMessage(conversationId, userId));
        entityManager.persist(createMessage(conversationId, userId));
        entityManager.persist(createMessage(conversationId, userId));
        entityManager.flush();
        
        // When
        long count = repository.countByConversationId(conversationId);
        
        // Then
        assertThat(count).isEqualTo(3);
    }
    
    @Test
    void deleteByConversationId_ShouldDeleteAllMessages() {
        // Given
        entityManager.persist(createMessage(conversationId, userId));
        entityManager.persist(createMessage(conversationId, userId));
        entityManager.flush();
        
        // When
        repository.deleteByConversationId(conversationId);
        entityManager.flush();
        
        // Then
        List<VoiceMessageEntity> remaining = repository
            .findByConversationIdOrderByCreatedAtAsc(conversationId);
        assertThat(remaining).isEmpty();
    }
    
    private VoiceMessageEntity createMessage(String convId, UUID userId) {
        VoiceMessageEntity message = new VoiceMessageEntity();
        message.setId(UUID.randomUUID());
        message.setConversationId(convId);
        message.setUserId(userId);
        message.setSender("USER");
        message.setAudioUrl("http://storage/audio.mp3");
        message.setTranscription("Test transcription");
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}

// ===========================================
// CONFIGURATION TEST
// ===========================================

// nexus-audio-api/src/test/resources/application-test.yml
"""
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      auto-offset-reset: earliest
  
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB

minio:
  url: http://localhost:9000
  access-key: test
  secret-key: testtest
  bucket-name: test-bucket

openai:
  api-key: test-key
  whisper:
    url: http://localhost:8080/mock-whisper

elevenlabs:
  api-key: test-key
  url: http://localhost:8080/mock-elevenlabs

logging:
  level:
    com.nexusai.audio: DEBUG
"""

// ===========================================
// TESTS E2E
// ===========================================

// nexus-audio-api/src/test/java/com/nexusai/audio/api/E2EVoiceMessageTest.java
package com.nexusai.audio.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests End-to-End du workflow complet de message vocal.
 * 
 * @author NexusAI Team
 * @version 1.0.0
 */
@SpringBootTest(
    classes = AudioApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class E2EVoiceMessageTest {
    
    @LocalServerPort
    private int port;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/audio";
    }
    
    @Test
    void completeVoiceMessageWorkflow_ShouldSucceed() {
        // 1. Upload voice message
        String messageId = given()
            .multiPart("audioFile", new File("src/test/resources/test-audio.mp3"))
            .multiPart("conversationId", "conv-e2e-123")
            .multiPart("userId", "550e8400-e29b-41d4-a716-446655440000")
            .multiPart("senderType", "USER")
        .when()
            .post("/voice-messages")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("transcription", notNullValue())
            .body("emotionDetected", notNullValue())
            .extract()
            .path("id");
        
        // 2. Retrieve the message
        given()
        .when()
            .get("/voice-messages/{id}", messageId)
        .then()
            .statusCode(200)
            .body("id", equalTo(messageId))
            .body("conversationId", equalTo("conv-e2e-123"));
        
        // 3. Get all messages from conversation
        given()
        .when()
            .get("/voice-messages/conversation/conv-e2e-123")
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(1)));
        
        // 4. Delete the message
        given()
        .when()
            .delete("/voice-messages/{id}", messageId)
        .then()
            .statusCode(204);
        
        // 5. Verify deletion
        given()
        .when()
            .get("/voice-messages/{id}", messageId)
        .then()
            .statusCode(404);
    }
}
