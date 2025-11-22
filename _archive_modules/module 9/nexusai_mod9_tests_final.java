// =============================================================================
// FICHIER 1: ImageModerationServiceTest.java
// =============================================================================
package com.nexusai.moderation.service.moderation;

import com.nexusai.moderation.model.dto.ModerationResponse;
import com.nexusai.moderation.model.enums.*;
import com.nexusai.moderation.repository.ModerationIncidentRepository;
import com.nexusai.moderation.service.client.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests pour ImageModerationService.
 * 
 * @author NexusAI Team
 */
@ExtendWith(MockitoExtension.class)
class ImageModerationServiceTest {
    
    @Mock
    private RekognitionClient rekognitionClient;
    
    @Mock
    private PhotoDNAClient photoDNAClient;
    
    @Mock
    private UserServiceClient userServiceClient;
    
    @Mock
    private ModerationRulesService rulesService;
    
    @Mock
    private ModerationIncidentRepository incidentRepository;
    
    @InjectMocks
    private ImageModerationService imageService;
    
    @Test
    void testModerateImage_CleanImage_ShouldAllow() {
        // Arrange
        String imageUrl = "https://example.com/clean.jpg";
        String userId = UUID.randomUUID().toString();
        
        when(photoDNAClient.checkImage(imageUrl))
            .thenReturn(new PhotoDNAClient.PhotoDNAResponse(false, 0.0, "NO_MATCH"));
        
        Map<String, Double> cleanLabels = new HashMap<>();
        cleanLabels.put("Explicit Nudity", 5.0);
        cleanLabels.put("Violence", 2.0);
        
        when(rekognitionClient.detectModerationLabels(imageUrl))
            .thenReturn(new RekognitionClient.ModerationResult(cleanLabels));
        
        var userInfo = createUserInfo(ModerationLevel.STRICT);
        when(userServiceClient.getUserInfo(userId)).thenReturn(userInfo);
        
        // Act
        ModerationResponse response = imageService.moderateImage(imageUrl, userId);
        
        // Assert
        assertThat(response.isAllowed()).isTrue();
        verify(incidentRepository, never()).save(any());
    }
    
    @Test
    void testModerateImage_CSAM_ShouldBlock() {
        // Arrange
        String imageUrl = "https://example.com/illegal.jpg";
        String userId = UUID.randomUUID().toString();
        
        // PhotoDNA détecte CSAM
        when(photoDNAClient.checkImage(imageUrl))
            .thenReturn(new PhotoDNAClient.PhotoDNAResponse(true, 0.95, "MATCH"));
        
        when(incidentRepository.save(any())).thenAnswer(inv -> {
            var incident = inv.getArgument(0);
            return incident;
        });
        
        // Act
        ModerationResponse response = imageService.moderateImage(imageUrl, userId);
        
        // Assert
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getIncidentType()).isEqualTo(IncidentType.SEXUAL_MINORS);
        assertThat(response.getSeverity()).isEqualTo(Severity.CRITICAL);
        
        verify(incidentRepository).save(argThat(incident ->
            incident.getSeverity() == Severity.CRITICAL &&
            incident.getIncidentType() == IncidentType.SEXUAL_MINORS
        ));
    }
    
    @Test
    void testModerateImage_ExplicitContent_StrictMode_ShouldBlock() {
        // Arrange
        String imageUrl = "https://example.com/explicit.jpg";
        String userId = UUID.randomUUID().toString();
        
        when(photoDNAClient.checkImage(imageUrl))
            .thenReturn(new PhotoDNAClient.PhotoDNAResponse(false, 0.0, "NO_MATCH"));
        
        Map<String, Double> explicitLabels = new HashMap<>();
        explicitLabels.put("Explicit Nudity", 85.0);
        explicitLabels.put("Sexual Activity", 75.0);
        
        when(rekognitionClient.detectModerationLabels(imageUrl))
            .thenReturn(new RekognitionClient.ModerationResult(explicitLabels));
        
        var userInfo = createUserInfo(ModerationLevel.STRICT);
        when(userServiceClient.getUserInfo(userId)).thenReturn(userInfo);
        
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        ModerationResponse response = imageService.moderateImage(imageUrl, userId);
        
        // Assert
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getIncidentType()).isEqualTo(IncidentType.SEXUAL_CONTENT);
        
        verify(incidentRepository).save(any());
    }
    
    // Helper methods
    
    private UserServiceClient.UserInfo createUserInfo(ModerationLevel level) {
        var userInfo = new UserServiceClient.UserInfo();
        var subscription = new UserServiceClient.SubscriptionInfo();
        subscription.setPlan(level == ModerationLevel.STRICT ? "FREE" : "PREMIUM");
        userInfo.setSubscription(subscription);
        
        var preferences = new UserServiceClient.UserPreferences();
        preferences.setModerationLevel(level.name());
        userInfo.setPreferences(preferences);
        
        return userInfo;
    }
}

// =============================================================================
// FICHIER 2: IncidentManagementServiceTest.java
// =============================================================================
package com.nexusai.moderation.service.incident;

import com.nexusai.moderation.model.entity.ModerationIncident;
import com.nexusai.moderation.model.enums.*;
import com.nexusai.moderation.repository.ModerationIncidentRepository;
import com.nexusai.moderation.event.ModerationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests pour IncidentManagementService.
 * 
 * @author NexusAI Team
 */
@ExtendWith(MockitoExtension.class)
class IncidentManagementServiceTest {
    
    @Mock
    private ModerationIncidentRepository incidentRepository;
    
    @Mock
    private EscalationService escalationService;
    
    @Mock
    private ModerationEventPublisher eventPublisher;
    
    @InjectMocks
    private IncidentManagementService incidentService;
    
    @Test
    void testGetIncident_ExistingId_ShouldReturnIncident() {
        // Arrange
        UUID incidentId = UUID.randomUUID();
        ModerationIncident incident = createIncident(incidentId);
        
        when(incidentRepository.findById(incidentId))
            .thenReturn(Optional.of(incident));
        
        // Act
        Optional<ModerationIncident> result = incidentService.getIncident(incidentId);
        
        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(incidentId);
    }
    
    @Test
    void testUpdateIncidentStatus_ShouldUpdateAndPublishEvent() {
        // Arrange
        UUID incidentId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        ModerationIncident incident = createIncident(incidentId);
        
        when(incidentRepository.findById(incidentId))
            .thenReturn(Optional.of(incident));
        when(incidentRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        ModerationIncident updated = incidentService.updateIncidentStatus(
            incidentId,
            "RESOLVED",
            reviewerId,
            "Test review notes"
        );
        
        // Assert
        assertThat(updated.getStatus()).isEqualTo("RESOLVED");
        assertThat(updated.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(updated.getReviewedAt()).isNotNull();
        assertThat(updated.getNotes()).contains("Test review notes");
        
        verify(eventPublisher).publishIncidentReviewed(updated);
    }
    
    @Test
    void testEscalateIfNeeded_CriticalIncident_ShouldEscalate() {
        // Arrange
        UUID incidentId = UUID.randomUUID();
        ModerationIncident incident = createCriticalIncident(incidentId);
        
        when(incidentRepository.findById(incidentId))
            .thenReturn(Optional.of(incident));
        when(escalationService.shouldEscalate(incident))
            .thenReturn(true);
        when(incidentRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        boolean escalated = incidentService.escalateIfNeeded(incidentId);
        
        // Assert
        assertThat(escalated).isTrue();
        verify(escalationService).escalate(incident);
    }
    
    // Helper methods
    
    private ModerationIncident createIncident(UUID id) {
        return ModerationIncident.builder()
            .id(id)
            .userId(UUID.randomUUID())
            .contentType(ContentType.TEXT)
            .incidentType(IncidentType.SEXUAL_CONTENT)
            .severity(Severity.MEDIUM)
            .status("PENDING")
            .automated(true)
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    private ModerationIncident createCriticalIncident(UUID id) {
        return ModerationIncident.builder()
            .id(id)
            .userId(UUID.randomUUID())
            .contentType(ContentType.IMAGE)
            .incidentType(IncidentType.SEXUAL_MINORS)
            .severity(Severity.CRITICAL)
            .status("PENDING")
            .automated(true)
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();
    }
}

// =============================================================================
// FICHIER 3: WarningServiceTest.java
// =============================================================================
package com.nexusai.moderation.service.incident;

import com.nexusai.moderation.model.entity.UserWarning;
import com.nexusai.moderation.repository.UserWarningRepository;
import com.nexusai.moderation.event.ModerationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests pour WarningService.
 * 
 * @author NexusAI Team
 */
@ExtendWith(MockitoExtension.class)
class WarningServiceTest {
    
    @Mock
    private UserWarningRepository warningRepository;
    
    @Mock
    private ModerationEventPublisher eventPublisher;
    
    @InjectMocks
    private WarningService warningService;
    
    @Test
    void testIssueWarning_ShouldCreateWarning() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        
        when(warningRepository.save(any()))
            .thenAnswer(inv -> {
                UserWarning warning = inv.getArgument(0);
                warning.setId(UUID.randomUUID());
                return warning;
            });
        
        when(warningRepository.findByUserIdAndExpiresAtAfter(eq(userId), any()))
            .thenReturn(Collections.emptyList());
        
        // Act
        UserWarning warning = warningService.issueWarning(
            userId,
            incidentId,
            "MEDIUM",
            "Test warning"
        );
        
        // Assert
        assertThat(warning).isNotNull();
        assertThat(warning.getUserId()).isEqualTo(userId);
        assertThat(warning.getWarningType()).isEqualTo("MEDIUM");
        assertThat(warning.getAcknowledged()).isFalse();
        
        verify(eventPublisher).publishUserWarned(any());
    }
    
    @Test
    void testCalculateUserWarningPoints_MultipleWarnings_ShouldSumCorrectly() {
        // Arrange
        UUID userId = UUID.randomUUID();
        
        List<UserWarning> warnings = Arrays.asList(
            createWarning("LOW"),      // 1 point
            createWarning("MEDIUM"),   // 2 points
            createWarning("HIGH")      // 3 points
        );
        
        when(warningRepository.findByUserIdAndExpiresAtAfter(eq(userId), any()))
            .thenReturn(warnings);
        
        // Act
        int points = warningService.calculateUserWarningPoints(userId);
        
        // Assert
        assertThat(points).isEqualTo(6); // 1 + 2 + 3
    }
    
    @Test
    void testAcknowledgeWarning_ShouldMarkAsAcknowledged() {
        // Arrange
        UUID warningId = UUID.randomUUID();
        UserWarning warning = createWarning("LOW");
        warning.setId(warningId);
        
        when(warningRepository.findById(warningId))
            .thenReturn(Optional.of(warning));
        when(warningRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        UserWarning acknowledged = warningService.acknowledgeWarning(warningId);
        
        // Assert
        assertThat(acknowledged.getAcknowledged()).isTrue();
        assertThat(acknowledged.getAcknowledgedAt()).isNotNull();
    }
    
    // Helper methods
    
    private UserWarning createWarning(String type) {
        return UserWarning.builder()
            .userId(UUID.randomUUID())
            .warningType(type)
            .description("Test warning")
            .acknowledged(false)
            .expiresAt(LocalDateTime.now().plusDays(30))
            .createdAt(LocalDateTime.now())
            .build();
    }
}

// =============================================================================
// FICHIER 4: ConsentManagementServiceTest.java
// =============================================================================
package com.nexusai.moderation.service.consent;

import com.nexusai.moderation.model.entity.AdultContentConsent;
import com.nexusai.moderation.repository.AdultContentConsentRepository;
import com.nexusai.moderation.service.client.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests pour ConsentManagementService.
 * 
 * @author NexusAI Team
 */
@ExtendWith(MockitoExtension.class)
class ConsentManagementServiceTest {
    
    @Mock
    private AdultContentConsentRepository consentRepository;
    
    @Mock
    private DigitalSignatureService signatureService;
    
    @Mock
    private UserServiceClient userServiceClient;
    
    @InjectMocks
    private ConsentManagementService consentService;
    
    @Test
    void testCreateConsent_ValidKYC_ShouldCreateConsent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        
        when(userServiceClient.hasValidKYC(userId.toString(), 3))
            .thenReturn(true);
        when(consentRepository.findActiveConsentByUserIdAndType(userId, "UNMODERATED_MODE"))
            .thenReturn(Optional.empty());
        when(signatureService.signData(anyString()))
            .thenReturn("signature123");
        when(consentRepository.save(any()))
            .thenAnswer(inv -> {
                AdultContentConsent consent = inv.getArgument(0);
                consent.setId(UUID.randomUUID());
                return consent;
            });
        
        // Act
        AdultContentConsent consent = consentService.createConsent(
            userId,
            "UNMODERATED_MODE",
            ipAddress,
            userAgent
        );
        
        // Assert
        assertThat(consent).isNotNull();
        assertThat(consent.getUserId()).isEqualTo(userId);
        assertThat(consent.getIpAddress()).isEqualTo(ipAddress);
        assertThat(consent.getRevoked()).isFalse();
        
        verify(consentRepository).save(any());
    }
    
    @Test
    void testCreateConsent_InsufficientKYC_ShouldThrowException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        
        when(userServiceClient.hasValidKYC(userId.toString(), 3))
            .thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() ->
            consentService.createConsent(userId, "UNMODERATED_MODE", "127.0.0.1", "Mozilla")
        ).isInstanceOf(ConsentManagementService.InsufficientKYCException.class);
    }
    
    @Test
    void testRevokeConsent_ShouldMarkAsRevoked() {
        // Arrange
        UUID consentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        AdultContentConsent consent = AdultContentConsent.builder()
            .id(consentId)
            .userId(userId)
            .consentType("UNMODERATED_MODE")
            .revoked(false)
            .build();
        
        when(consentRepository.findById(consentId))
            .thenReturn(Optional.of(consent));
        when(consentRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        
        // Act
        consentService.revokeConsent(consentId, userId);
        
        // Assert
        assertThat(consent.getRevoked()).isTrue();
        assertThat(consent.getRevokedAt()).isNotNull();
    }
}

// =============================================================================
// FICHIER 5: EmotionAnalysisServiceTest.java
// =============================================================================
package com.nexusai.moderation.service.detection;

import com.nexusai.moderation.service.detection.EmotionAnalysisService.EmotionAnalysisResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests pour EmotionAnalysisService.
 * 
 * @author NexusAI Team
 */
class EmotionAnalysisServiceTest {
    
    private final EmotionAnalysisService emotionService = new EmotionAnalysisService();
    
    @Test
    void testAnalyzeEmotion_HappyText_ShouldDetectJoy() {
        // Arrange
        String happyText = "Je suis tellement heureux et joyeux aujourd'hui! C'est génial! 😊";
        
        // Act
        EmotionAnalysisResult result = emotionService.analyzeEmotion(happyText);
        
        // Assert
        assertThat(result.getDominantEmotion()).isEqualTo("JOY");
        assertThat(result.isPositive()).isTrue();
        assertThat(result.getEmotions()).containsKey("JOY");
    }
    
    @Test
    void testAnalyzeEmotion_SadText_ShouldDetectSadness() {
        // Arrange
        String sadText = "Je suis triste et malheureux 😢. J'ai le chagrin.";
        
        // Act
        EmotionAnalysisResult result = emotionService.analyzeEmotion(sadText);
        
        // Assert
        assertThat(result.getDominantEmotion()).isEqualTo("SADNESS");
        assertThat(result.isNegative()).isTrue();
        assertThat(result.getEmotions()).containsKey("SADNESS");
    }
    
    @Test
    void testAnalyzeEmotion_NeutralText_ShouldBeNeutral() {
        // Arrange
        String neutralText = "Bonjour, comment allez-vous?";
        
        // Act
        EmotionAnalysisResult result = emotionService.analyzeEmotion(neutralText);
        
        // Assert
        assertThat(result.isNeutral()).isTrue();
        assertThat(result.getOverallIntensity()).isLessThan(30.0);
    }
}

// =============================================================================
// FIN - Tests
// =============================================================================
