/**
 * ═══════════════════════════════════════════════════════════════════════════
 * MODULE 5 : TESTS UNITAIRES & INTÉGRATION
 * ═══════════════════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════════════════
// 10. TESTS UNITAIRES - SERVICE
// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.core.service;

import com.nexusai.image.domain.dto.ImageGenerationRequest;
import com.nexusai.image.domain.dto.ImageGenerationResponse;
import com.nexusai.image.domain.entity.GeneratedImage;
import com.nexusai.image.domain.entity.GeneratedImage.ImageStatus;
import com.nexusai.image.infrastructure.kafka.ImageGenerationProducer;
import com.nexusai.image.infrastructure.repository.GeneratedImageRepository;
import com.nexusai.image.core.mapper.ImageMapper;
import com.nexusai.image.core.exception.ImageNotFoundException;
import com.nexusai.image.core.exception.InsufficientTokensException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ImageGenerationService
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests ImageGenerationService")
class ImageGenerationServiceTest {

    @Mock
    private GeneratedImageRepository imageRepository;

    @Mock
    private ImageGenerationProducer kafkaProducer;

    @Mock
    private TokenService tokenService;

    @Mock
    private ModerationService moderationService;

    @Mock
    private ImageMapper imageMapper;

    @InjectMocks
    private ImageGenerationService imageGenerationService;

    private UUID userId;
    private ImageGenerationRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        request = ImageGenerationRequest.builder()
            .prompt("A beautiful sunset over mountains")
            .negativePrompt("blurry, low quality")
            .style("realistic")
            .resolution("1024x1024")
            .build();
    }

    @Test
    @DisplayName("Génération d'image réussie")
    void generateImage_Success() {
        // Given
        GeneratedImage savedImage = GeneratedImage.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .prompt(request.getPrompt())
            .status(ImageStatus.QUEUED)
            .tokensCost(20)
            .build();

        ImageGenerationResponse expectedResponse = ImageGenerationResponse.builder()
            .id(savedImage.getId())
            .userId(userId)
            .prompt(request.getPrompt())
            .status("QUEUED")
            .tokensCost(20)
            .build();

        when(tokenService.hasEnoughTokens(userId, 20)).thenReturn(true);
        when(imageRepository.save(any(GeneratedImage.class))).thenReturn(savedImage);
        when(imageMapper.toResponse(savedImage)).thenReturn(expectedResponse);

        // When
        ImageGenerationResponse response = imageGenerationService.generateImage(request, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedImage.getId());
        assertThat(response.getStatus()).isEqualTo("QUEUED");
        assertThat(response.getTokensCost()).isEqualTo(20);

        verify(moderationService).moderatePrompt(request.getPrompt());
        verify(tokenService).consumeTokens(userId, 20);
        verify(kafkaProducer).publishGenerationRequest(any());
    }

    @Test
    @DisplayName("Échec - Tokens insuffisants")
    void generateImage_InsufficientTokens() {
        // Given
        when(tokenService.hasEnoughTokens(userId, 20)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> imageGenerationService.generateImage(request, userId))
            .isInstanceOf(InsufficientTokensException.class)
            .hasMessageContaining("Solde insuffisant");

        verify(imageRepository, never()).save(any());
        verify(kafkaProducer, never()).publishGenerationRequest(any());
    }

    @Test
    @DisplayName("Récupération d'image réussie")
    void getImage_Success() {
        // Given
        UUID imageId = UUID.randomUUID();
        GeneratedImage image = GeneratedImage.builder()
            .id(imageId)
            .userId(userId)
            .prompt("Test prompt")
            .status(ImageStatus.COMPLETED)
            .build();

        ImageGenerationResponse expectedResponse = ImageGenerationResponse.builder()
            .id(imageId)
            .userId(userId)
            .prompt("Test prompt")
            .status("COMPLETED")
            .build();

        when(imageRepository.findByIdAndUserId(imageId, userId))
            .thenReturn(Optional.of(image));
        when(imageMapper.toResponse(image)).thenReturn(expectedResponse);

        // When
        ImageGenerationResponse response = imageGenerationService.getImage(imageId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(imageId);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Échec - Image non trouvée")
    void getImage_NotFound() {
        // Given
        UUID imageId = UUID.randomUUID();
        when(imageRepository.findByIdAndUserId(imageId, userId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> imageGenerationService.getImage(imageId, userId))
            .isInstanceOf(ImageNotFoundException.class)
            .hasMessageContaining("Image non trouvée");
    }

    @Test
    @DisplayName("Toggle favorite réussi")
    void toggleFavorite_Success() {
        // Given
        UUID imageId = UUID.randomUUID();
        GeneratedImage image = GeneratedImage.builder()
            .id(imageId)
            .userId(userId)
            .isFavorite(false)
            .build();

        when(imageRepository.findByIdAndUserId(imageId, userId))
            .thenReturn(Optional.of(image));

        // When
        imageGenerationService.toggleFavorite(imageId, userId, true);

        // Then
        assertThat(image.getIsFavorite()).isTrue();
        verify(imageRepository).save(image);
    }

    @Test
    @DisplayName("Suppression d'image réussie")
    void deleteImage_Success() {
        // Given
        UUID imageId = UUID.randomUUID();
        GeneratedImage image = GeneratedImage.builder()
            .id(imageId)
            .userId(userId)
            .build();

        when(imageRepository.findByIdAndUserId(imageId, userId))
            .thenReturn(Optional.of(image));

        // When
        imageGenerationService.deleteImage(imageId, userId);

        // Then
        verify(imageRepository).delete(image);
    }

    @Test
    @DisplayName("Calcul du coût en tokens selon résolution")
    void calculateTokensCost_DifferentResolutions() {
        // Test 512x512
        ImageGenerationRequest request512 = ImageGenerationRequest.builder()
            .prompt("Test")
            .resolution("512x512")
            .build();

        when(tokenService.hasEnoughTokens(any(), eq(5))).thenReturn(true);
        when(imageRepository.save(any())).thenReturn(GeneratedImage.builder().build());

        imageGenerationService.generateImage(request512, userId);
        verify(tokenService).consumeTokens(userId, 5);

        // Test 1024x1536
        ImageGenerationRequest request1024 = ImageGenerationRequest.builder()
            .prompt("Test")
            .resolution("1024x1536")
            .build();

        when(tokenService.hasEnoughTokens(any(), eq(30))).thenReturn(true);

        imageGenerationService.generateImage(request1024, userId);
        verify(tokenService).consumeTokens(userId, 30);
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 11. TESTS D'INTÉGRATION - CONTROLLER
// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusai.image.core.service.ImageGenerationService;
import com.nexusai.image.domain.dto.ImageGenerationRequest;
import com.nexusai.image.domain.dto.ImageGenerationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour ImageGenerationController
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@WebMvcTest(ImageGenerationController.class)
@DisplayName("Tests ImageGenerationController")
class ImageGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ImageGenerationService imageGenerationService;

    private UUID userId;
    private ImageGenerationRequest request;
    private ImageGenerationResponse response;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        request = ImageGenerationRequest.builder()
            .prompt("A beautiful sunset over mountains")
            .negativePrompt("blurry, low quality")
            .style("realistic")
            .resolution("1024x1024")
            .build();

        response = ImageGenerationResponse.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .prompt(request.getPrompt())
            .negativePrompt(request.getNegativePrompt())
            .style(request.getStyle())
            .resolution(request.getResolution())
            .status("QUEUED")
            .tokensCost(20)
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/images/generate - Succès")
    @WithMockUser
    void generateImage_Success() throws Exception {
        // Given
        when(imageGenerationService.generateImage(any(ImageGenerationRequest.class), eq(userId)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/images/generate")
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(response.getId().toString()))
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.tokens_cost").value(20));
    }

    @Test
    @DisplayName("POST /api/v1/images/generate - Validation échouée (prompt trop court)")
    @WithMockUser
    void generateImage_ValidationFailed() throws Exception {
        // Given
        ImageGenerationRequest invalidRequest = ImageGenerationRequest.builder()
            .prompt("Short") // Moins de 10 caractères
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/images/generate")
                .with(user(userId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/images/{imageId} - Succès")
    @WithMockUser
    void getImage_Success() throws Exception {
        // Given
        UUID imageId = response.getId();
        when(imageGenerationService.getImage(imageId, userId))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/images/{imageId}", imageId)
                .with(user(userId.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(imageId.toString()))
            .andExpect(jsonPath("$.prompt").value(request.getPrompt()));
    }

    @Test
    @DisplayName("GET /api/v1/images/user/me - Succès avec pagination")
    @WithMockUser
    void getMyImages_Success() throws Exception {
        // Given
        Page<ImageGenerationResponse> page = new PageImpl<>(
            Collections.singletonList(response),
            PageRequest.of(0, 20),
            1
        );

        when(imageGenerationService.getUserImages(eq(userId), any()))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/images/user/me")
                .with(user(userId.toString()))
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(response.getId().toString()))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/images/{imageId}/favorite - Succès")
    @WithMockUser
    void toggleFavorite_Success() throws Exception {
        // Given
        UUID imageId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(post("/api/v1/images/{imageId}/favorite", imageId)
                .with(user(userId.toString()))
                .param("isFavorite", "true"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/images/{imageId} - Succès")
    @WithMockUser
    void deleteImage_Success() throws Exception {
        // Given
        UUID imageId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/v1/images/{imageId}", imageId)
                .with(user(userId.toString())))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Accès non authentifié refusé")
    void unauthenticatedAccess_Denied() throws Exception {
        mockMvc.perform(get("/api/v1/images/user/me"))
            .andExpect(status().isUnauthorized());
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 12. TESTS D'INTÉGRATION AVEC TESTCONTAINERS
// ═══════════════════════════════════════════════════════════════════════════

package com.nexusai.image;

import com.nexusai.image.domain.dto.ImageGenerationRequest;
import com.nexusai.image.domain.dto.ImageGenerationResponse;
import com.nexusai.image.domain.entity.GeneratedImage;
import com.nexusai.image.infrastructure.repository.GeneratedImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration end-to-end avec Testcontainers
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Tests d'intégration E2E")
class ImageGenerationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GeneratedImageRepository imageRepository;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16-alpine")
    )
        .withDatabaseName("nexusai_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    @DisplayName("Cycle de vie complet d'une image")
    void fullImageLifecycle() {
        // 1. Création d'une image
        ImageGenerationRequest request = ImageGenerationRequest.builder()
            .prompt("A beautiful landscape with mountains and a lake")
            .style("realistic")
            .resolution("1024x1024")
            .build();

        String url = "http://localhost:" + port + "/api/v1/images/generate";
        
        ResponseEntity<ImageGenerationResponse> createResponse = restTemplate.postForEntity(
            url,
            request,
            ImageGenerationResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getStatus()).isEqualTo("QUEUED");

        UUID imageId = createResponse.getBody().getId();

        // 2. Vérification en base de données
        GeneratedImage savedImage = imageRepository.findById(imageId).orElseThrow();
        assertThat(savedImage.getPrompt()).isEqualTo(request.getPrompt());
        assertThat(savedImage.getStatus()).isEqualTo(GeneratedImage.ImageStatus.QUEUED);

        // 3. Récupération de l'image
        ResponseEntity<ImageGenerationResponse> getResponse = restTemplate.getForEntity(
            url + "/" + imageId,
            ImageGenerationResponse.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getId()).isEqualTo(imageId);

        // 4. Marquage comme favorite
        restTemplate.postForLocation(
            url + "/" + imageId + "/favorite?isFavorite=true",
            null
        );

        GeneratedImage updatedImage = imageRepository.findById(imageId).orElseThrow();
        assertThat(updatedImage.getIsFavorite()).isTrue();

        // 5. Suppression
        restTemplate.delete(url + "/" + imageId);

        assertThat(imageRepository.findById(imageId)).isEmpty();
    }
}
