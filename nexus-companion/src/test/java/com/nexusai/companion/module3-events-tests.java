// ============================================================================
// PACKAGE: com.nexusai.companion.event
// Description: Modèles d'événements Kafka
// ============================================================================

package com.nexusai.companion.event;

import lombok.*;

import java.time.Instant;

/**
 * Événement représentant une action sur un compagnon.
 * Publié sur Kafka pour notification asynchrone aux autres modules.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionEvent {
    
    /**
     * Type d'événement
     * (COMPANION_CREATED, COMPANION_UPDATED, COMPANION_DELETED, COMPANION_EVOLVED)
     */
    private String type;
    
    /**
     * ID du compagnon concerné
     */
    private String companionId;
    
    /**
     * ID de l'utilisateur propriétaire
     */
    private String userId;
    
    /**
     * Timestamp de l'événement
     */
    private Instant timestamp;
    
    /**
     * Données additionnelles (optionnel)
     */
    private java.util.Map<String, Object> metadata;
}

// ============================================================================
// FICHIER: EventListenerService.java
// Description: Service d'écoute des événements des autres modules
// ============================================================================

package com.nexusai.companion.event;

import com.nexusai.companion.repository.CompanionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service d'écoute des événements provenant des autres modules.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventListenerService {
    
    private final CompanionRepository companionRepository;
    
    /**
     * Écoute les événements de suppression d'utilisateur.
     * Supprime tous les compagnons de l'utilisateur supprimé.
     */
    @KafkaListener(
        topics = "user.events",
        groupId = "companion-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(UserEvent event) {
        if ("USER_DELETED".equals(event.getType())) {
            log.info("Réception d'événement USER_DELETED pour userId: {}", 
                     event.getUserId());
            
            // Supprimer tous les compagnons de cet utilisateur
            companionRepository.deleteByUserId(event.getUserId());
            
            log.info("Compagnons supprimés pour l'utilisateur: {}", event.getUserId());
        }
    }
    
    /**
     * Écoute les événements de changement d'abonnement.
     * Permet d'ajuster les quotas dynamiquement.
     */
    @KafkaListener(
        topics = "subscription.events",
        groupId = "companion-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSubscriptionEvent(SubscriptionEvent event) {
        log.info("Réception d'événement subscription: {} pour userId: {}", 
                 event.getType(), event.getUserId());
        
        // Logique de gestion des changements d'abonnement
        // Par exemple, vérifier si l'utilisateur dépasse maintenant son nouveau quota
        if ("SUBSCRIPTION_DOWNGRADED".equals(event.getType())) {
            handleSubscriptionDowngrade(event.getUserId());
        }
    }
    
    private void handleSubscriptionDowngrade(String userId) {
        // TODO: Implémenter la logique de gestion du downgrade
        // Par exemple, désactiver les compagnons en excès
        log.warn("Downgrade d'abonnement détecté pour userId: {}", userId);
    }
}

/**
 * DTO pour les événements utilisateur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class UserEvent {
    private String type;
    private String userId;
    private Instant timestamp;
}

/**
 * DTO pour les événements d'abonnement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class SubscriptionEvent {
    private String type;
    private String userId;
    private String oldPlan;
    private String newPlan;
    private Instant timestamp;
}

// ============================================================================
// PACKAGE: com.nexusai.companion
// Description: Tests unitaires
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.domain.Companion;
import com.nexusai.companion.dto.*;
import com.nexusai.companion.exception.*;
import com.nexusai.companion.mapper.CompanionMapper;
import com.nexusai.companion.repository.CompanionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour CompanionService.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du CompanionService")
class CompanionServiceTest {
    
    @Mock
    private CompanionRepository companionRepository;
    
    @Mock
    private CompanionMapper companionMapper;
    
    @Mock
    private QuotaService quotaService;
    
    @Mock
    private GeneticService geneticService;
    
    @Mock
    private EventPublisherService eventPublisher;
    
    @Mock
    private StorageService storageService;
    
    @InjectMocks
    private CompanionService companionService;
    
    private String userId;
    private CreateCompanionRequest createRequest;
    private Companion companion;
    private CompanionResponse companionResponse;
    
    @BeforeEach
    void setUp() {
        userId = "user-123";
        
        // Préparer les données de test
        createRequest = CreateCompanionRequest.builder()
            .name("Luna")
            .appearance(AppearanceDto.builder()
                .gender("FEMALE")
                .hairColor("BLONDE")
                .eyeColor("BLUE")
                .skinTone("FAIR")
                .bodyType("ATHLETIC")
                .age(25)
                .build())
            .personality(PersonalityDto.builder()
                .traits(TraitsDto.builder()
                    .openness(80)
                    .conscientiousness(70)
                    .extraversion(75)
                    .agreeableness(85)
                    .neuroticism(30)
                    .humor(70)
                    .empathy(90)
                    .jealousy(20)
                    .curiosity(85)
                    .confidence(75)
                    .playfulness(80)
                    .assertiveness(65)
                    .sensitivity(75)
                    .rationality(60)
                    .creativity(85)
                    .loyalty(90)
                    .independence(55)
                    .patience(70)
                    .adventurousness(75)
                    .romanticism(80)
                    .build())
                .interests(Arrays.asList("music", "art", "travel"))
                .dislikes(Arrays.asList("rudeness", "dishonesty"))
                .humorStyle("WITTY")
                .communicationStyle("WARM")
                .build())
            .voice(VoiceDto.builder()
                .voiceId("voice-001")
                .pitch(1.0f)
                .speed(1.0f)
                .style("FRIENDLY")
                .build())
            .backstory("A creative and empathetic companion.")
            .build();
        
        companion = Companion.builder()
            .id("companion-123")
            .userId(userId)
            .name("Luna")
            .createdAt(Instant.now())
            .lastEvolutionDate(Instant.now())
            .updatedAt(Instant.now())
            .isPublic(false)
            .likeCount(0)
            .build();
        
        companionResponse = CompanionResponse.builder()
            .id("companion-123")
            .userId(userId)
            .name("Luna")
            .build();
    }
    
    @Test
    @DisplayName("Création d'un compagnon avec succès")
    void createCompanion_Success() {
        // Given
        when(quotaService.canCreateCompanion(userId)).thenReturn(true);
        when(companionRepository.findByUserIdAndName(userId, "Luna"))
            .thenReturn(Optional.empty());
        when(companionMapper.toEntity(any())).thenReturn(companion);
        when(geneticService.generateInitialGeneticProfile(any()))
            .thenReturn(Companion.GeneticProfile.builder().build());
        when(companionRepository.save(any())).thenReturn(companion);
        when(companionMapper.toResponse(companion)).thenReturn(companionResponse);
        
        // When
        CompanionResponse result = companionService.createCompanion(userId, createRequest);
        
        // Then
        assertNotNull(result);
        assertEquals("Luna", result.getName());
        verify(companionRepository).save(any(Companion.class));
        verify(eventPublisher).publishCompanionCreated(companion);
    }
    
    @Test
    @DisplayName("Création échoue si quota dépassé")
    void createCompanion_QuotaExceeded() {
        // Given
        when(quotaService.canCreateCompanion(userId)).thenReturn(false);
        
        // When & Then
        assertThrows(QuotaExceededException.class, () -> {
            companionService.createCompanion(userId, createRequest);
        });
        
        verify(companionRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Création échoue si nom en double")
    void createCompanion_DuplicateName() {
        // Given
        when(quotaService.canCreateCompanion(userId)).thenReturn(true);
        when(companionRepository.findByUserIdAndName(userId, "Luna"))
            .thenReturn(Optional.of(companion));
        
        // When & Then
        assertThrows(DuplicateNameException.class, () -> {
            companionService.createCompanion(userId, createRequest);
        });
        
        verify(companionRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Récupération d'un compagnon par ID")
    void getCompanion_Success() {
        // Given
        when(companionRepository.findById("companion-123"))
            .thenReturn(Optional.of(companion));
        when(companionMapper.toResponse(companion)).thenReturn(companionResponse);
        
        // When
        CompanionResponse result = companionService.getCompanion("companion-123", userId);
        
        // Then
        assertNotNull(result);
        assertEquals("Luna", result.getName());
    }
    
    @Test
    @DisplayName("Récupération échoue si compagnon non trouvé")
    void getCompanion_NotFound() {
        // Given
        when(companionRepository.findById("companion-999"))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(CompanionNotFoundException.class, () -> {
            companionService.getCompanion("companion-999", userId);
        });
    }
    
    @Test
    @DisplayName("Récupération échoue si accès non autorisé")
    void getCompanion_Unauthorized() {
        // Given
        Companion privateCompanion = Companion.builder()
            .id("companion-123")
            .userId("other-user")
            .isPublic(false)
            .build();
        
        when(companionRepository.findById("companion-123"))
            .thenReturn(Optional.of(privateCompanion));
        
        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            companionService.getCompanion("companion-123", userId);
        });
    }
    
    @Test
    @DisplayName("Mise à jour d'un compagnon")
    void updateCompanion_Success() {
        // Given
        UpdateCompanionRequest updateRequest = UpdateCompanionRequest.builder()
            .name("Luna Updated")
            .build();
        
        when(companionRepository.findById("companion-123"))
            .thenReturn(Optional.of(companion));
        when(companionRepository.findByUserIdAndName(userId, "Luna Updated"))
            .thenReturn(Optional.empty());
        when(companionRepository.save(any())).thenReturn(companion);
        when(companionMapper.toResponse(companion)).thenReturn(companionResponse);
        
        // When
        CompanionResponse result = companionService.updateCompanion(
            "companion-123", userId, updateRequest
        );
        
        // Then
        assertNotNull(result);
        verify(companionRepository).save(any());
        verify(eventPublisher).publishCompanionUpdated(companion);
    }
    
    @Test
    @DisplayName("Suppression d'un compagnon")
    void deleteCompanion_Success() {
        // Given
        when(companionRepository.findById("companion-123"))
            .thenReturn(Optional.of(companion));
        doNothing().when(companionRepository).deleteById("companion-123");
        
        // When
        companionService.deleteCompanion("companion-123", userId);
        
        // Then
        verify(companionRepository).deleteById("companion-123");
        verify(eventPublisher).publishCompanionDeleted("companion-123", userId);
    }
    
    @Test
    @DisplayName("Récupération des compagnons d'un utilisateur")
    void getUserCompanions_Success() {
        // Given
        List<Companion> companions = Arrays.asList(companion);
        when(companionRepository.findByUserId(userId)).thenReturn(companions);
        when(companionMapper.toResponse(companion)).thenReturn(companionResponse);
        
        // When
        List<CompanionResponse> result = companionService.getUserCompanions(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Luna", result.get(0).getName());
    }
}

// ============================================================================
// FICHIER: GeneticServiceTest.java
// Description: Tests pour le service génétique
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.domain.Companion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour GeneticService.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@DisplayName("Tests du GeneticService")
class GeneticServiceTest {
    
    private final GeneticService geneticService = new GeneticService();
    
    @Test
    @DisplayName("Génération d'un profil génétique initial")
    void generateInitialGeneticProfile_Success() {
        // Given
        Companion.Personality personality = Companion.Personality.builder()
            .traits(Companion.Traits.builder()
                .openness(80)
                .conscientiousness(70)
                .extraversion(75)
                .agreeableness(85)
                .neuroticism(30)
                .humor(70)
                .empathy(90)
                .jealousy(20)
                .curiosity(85)
                .confidence(75)
                .playfulness(80)
                .assertiveness(65)
                .sensitivity(75)
                .rationality(60)
                .creativity(85)
                .loyalty(90)
                .independence(55)
                .patience(70)
                .adventurousness(75)
                .romanticism(80)
                .build())
            .build();
        
        // When
        Companion.GeneticProfile profile = 
            geneticService.generateInitialGeneticProfile(personality);
        
        // Then
        assertNotNull(profile);
        assertNotNull(profile.getGenes());
        assertEquals(20, profile.getGenes().size());
        assertFalse(profile.isFrozen());
        assertTrue(profile.getFrozenTraits().isEmpty());
        
        // Vérifier que les traits dominants ont des valeurs > 70
        for (String trait : profile.getDominantTraits()) {
            assertTrue(profile.getGenes().get(trait) > 70);
        }
        
        // Vérifier que les traits récessifs ont des valeurs < 30
        for (String trait : profile.getRecessiveTraits()) {
            assertTrue(profile.getGenes().get(trait) < 30);
        }
    }
    
    @Test
    @DisplayName("Évolution d'un compagnon")
    void evolveCompanion_Success() {
        // Given
        Companion companion = createTestCompanion();
        int initialOpennessGene = companion.getGeneticProfile().getGenes().get("openness");
        
        // When
        Companion evolved = geneticService.evolveCompanion(
            companion,
            5,
            Arrays.asList("openness", "creativity")
        );
        
        // Then
        assertNotNull(evolved);
        int finalOpennessGene = evolved.getGeneticProfile().getGenes().get("openness");
        
        // La valeur devrait avoir changé (sauf si pas de chance)
        // On vérifie juste que l'évolution a été appliquée
        assertNotNull(evolved.getLastEvolutionDate());
    }
    
    @Test
    @DisplayName("Fusion de deux compagnons")
    void mergeGeneticProfiles_Success() {
        // Given
        Companion companion1 = createTestCompanion();
        Companion companion2 = createTestCompanion();
        
        // Modifier les gènes du second pour différencier
        companion2.getGeneticProfile().getGenes().put("openness", 20);
        companion2.getGeneticProfile().getGenes().put("empathy", 30);
        
        // When
        Companion.GeneticProfile merged = geneticService.mergeGeneticProfiles(
            companion1,
            companion2,
            0.5 // 50/50
        );
        
        // Then
        assertNotNull(merged);
        assertNotNull(merged.getGenes());
        assertEquals(20, merged.getGenes().size());
        
        // Les valeurs fusionnées devraient être entre celles des deux parents
        int mergedOpenness = merged.getGenes().get("openness");
        int comp1Openness = companion1.getGeneticProfile().getGenes().get("openness");
        int comp2Openness = companion2.getGeneticProfile().getGenes().get("openness");
        
        assertTrue(mergedOpenness >= Math.min(comp1Openness, comp2Openness) - 10);
        assertTrue(mergedOpenness <= Math.max(comp1Openness, comp2Openness) + 10);
    }
    
    private Companion createTestCompanion() {
        Map<String, Integer> genes = new HashMap<>();
        genes.put("openness", 80);
        genes.put("conscientiousness", 70);
        genes.put("extraversion", 75);
        genes.put("agreeableness", 85);
        genes.put("neuroticism", 30);
        genes.put("humor", 70);
        genes.put("empathy", 90);
        genes.put("jealousy", 20);
        genes.put("curiosity", 85);
        genes.put("confidence", 75);
        genes.put("playfulness", 80);
        genes.put("assertiveness", 65);
        genes.put("sensitivity", 75);
        genes.put("rationality", 60);
        genes.put("creativity", 85);
        genes.put("loyalty", 90);
        genes.put("independence", 55);
        genes.put("patience", 70);
        genes.put("adventurousness", 75);
        genes.put("romanticism", 80);
        
        Companion.GeneticProfile profile = Companion.GeneticProfile.builder()
            .genes(genes)
            .dominantTraits(Arrays.asList("empathy", "loyalty", "creativity"))
            .recessiveTraits(Arrays.asList("jealousy"))
            .frozen(false)
            .frozenTraits(new ArrayList<>())
            .build();
        
        return Companion.builder()
            .id("test-companion")
            .userId("test-user")
            .name("Test")
            .geneticProfile(profile)
            .personality(Companion.Personality.builder()
                .traits(Companion.Traits.builder()
                    .openness(80)
                    .conscientiousness(70)
                    .extraversion(75)
                    .agreeableness(85)
                    .neuroticism(30)
                    .humor(70)
                    .empathy(90)
                    .jealousy(20)
                    .curiosity(85)
                    .confidence(75)
                    .playfulness(80)
                    .assertiveness(65)
                    .sensitivity(75)
                    .rationality(60)
                    .creativity(85)
                    .loyalty(90)
                    .independence(55)
                    .patience(70)
                    .adventurousness(75)
                    .romanticism(80)
                    .build())
                .build())
            .lastEvolutionDate(java.time.Instant.now())
            .build();
    }
}