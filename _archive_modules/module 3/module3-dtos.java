// ============================================================================
// PACKAGE: com.nexusai.companion.dto
// Description: DTOs pour les échanges API
// ============================================================================

package com.nexusai.companion.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO pour la création d'un compagnon.
 * Contient les validations des champs requis.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanionRequest {
    
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String name;
    
    @NotNull(message = "L'apparence est obligatoire")
    private AppearanceDto appearance;
    
    @NotNull(message = "La personnalité est obligatoire")
    private PersonalityDto personality;
    
    @NotNull(message = "La voix est obligatoire")
    private VoiceDto voice;
    
    @Size(max = 2000, message = "Le backstory ne peut pas dépasser 2000 caractères")
    private String backstory;
    
    /**
     * Création depuis un template (optionnel)
     */
    private String templateId;
}

/**
 * DTO pour la mise à jour d'un compagnon.
 * Tous les champs sont optionnels.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanionRequest {
    
    @Size(min = 2, max = 50)
    private String name;
    
    private AppearanceDto appearance;
    private PersonalityDto personality;
    private VoiceDto voice;
    
    @Size(max = 2000)
    private String backstory;
    
    private Boolean isPublic;
}

/**
 * DTO de réponse pour un compagnon.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionResponse {
    private String id;
    private String userId;
    private String name;
    private AppearanceDto appearance;
    private PersonalityDto personality;
    private VoiceDto voice;
    private String backstory;
    private GeneticProfileDto geneticProfile;
    private EmotionalStateDto emotionalState;
    private boolean isPublic;
    private int likeCount;
    private Instant createdAt;
    private Instant lastEvolutionDate;
    private Instant updatedAt;
}

/**
 * DTO pour l'apparence.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppearanceDto {
    
    @NotBlank(message = "Le genre est obligatoire")
    private String gender;
    
    @NotBlank(message = "La couleur de cheveux est obligatoire")
    private String hairColor;
    
    @NotBlank(message = "La couleur des yeux est obligatoire")
    private String eyeColor;
    
    @NotBlank(message = "Le teint de peau est obligatoire")
    private String skinTone;
    
    @NotBlank(message = "Le type de corps est obligatoire")
    private String bodyType;
    
    @NotNull(message = "L'âge est obligatoire")
    @Min(value = 18, message = "L'âge minimum est 18 ans")
    @Max(value = 100, message = "L'âge maximum est 100 ans")
    private Integer age;
    
    private String avatarImageUrl;
    private Map<String, Object> customFeatures;
}

/**
 * DTO pour la personnalité.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityDto {
    
    @NotNull(message = "Les traits sont obligatoires")
    private TraitsDto traits;
    
    private List<String> interests;
    private List<String> dislikes;
    private String humorStyle;
    private String communicationStyle;
}

/**
 * DTO pour les traits de personnalité.
 * Tous les traits doivent être entre 0 et 100.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraitsDto {
    
    @Min(0) @Max(100)
    private int openness;
    
    @Min(0) @Max(100)
    private int conscientiousness;
    
    @Min(0) @Max(100)
    private int extraversion;
    
    @Min(0) @Max(100)
    private int agreeableness;
    
    @Min(0) @Max(100)
    private int neuroticism;
    
    @Min(0) @Max(100)
    private int humor;
    
    @Min(0) @Max(100)
    private int empathy;
    
    @Min(0) @Max(100)
    private int jealousy;
    
    @Min(0) @Max(100)
    private int curiosity;
    
    @Min(0) @Max(100)
    private int confidence;
    
    @Min(0) @Max(100)
    private int playfulness;
    
    @Min(0) @Max(100)
    private int assertiveness;
    
    @Min(0) @Max(100)
    private int sensitivity;
    
    @Min(0) @Max(100)
    private int rationality;
    
    @Min(0) @Max(100)
    private int creativity;
    
    @Min(0) @Max(100)
    private int loyalty;
    
    @Min(0) @Max(100)
    private int independence;
    
    @Min(0) @Max(100)
    private int patience;
    
    @Min(0) @Max(100)
    private int adventurousness;
    
    @Min(0) @Max(100)
    private int romanticism;
}

/**
 * DTO pour la configuration vocale.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceDto {
    
    @NotBlank(message = "L'ID de voix est obligatoire")
    private String voiceId;
    
    @Min(0) @Max(2)
    private float pitch = 1.0f;
    
    @Min(0) @Max(2)
    private float speed = 1.0f;
    
    private String style;
}

/**
 * DTO pour le profil génétique.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneticProfileDto {
    private Map<String, Integer> genes;
    private List<String> dominantTraits;
    private List<String> recessiveTraits;
    private boolean frozen;
    private List<String> frozenTraits;
}

/**
 * DTO pour l'état émotionnel.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionalStateDto {
    private String current;
    
    @Min(0) @Max(100)
    private int intensity;
    
    private long duration;
}

/**
 * DTO pour la demande d'évolution génétique.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolveCompanionRequest {
    
    /**
     * Traits spécifiques à faire évoluer (optionnel)
     */
    private List<String> targetTraits;
    
    /**
     * Intensité de l'évolution (1-10)
     */
    @Min(1) @Max(10)
    private int intensity = 5;
}

/**
 * DTO pour le gel de traits génétiques.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreezeTraitsRequest {
    
    @NotEmpty(message = "Au moins un trait doit être spécifié")
    private List<String> traits;
    
    /**
     * Gel complet du profil génétique
     */
    private boolean freezeAll = false;
}

/**
 * DTO pour la fusion de compagnons.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCompanionsRequest {
    
    @NotBlank(message = "Le premier compagnon est obligatoire")
    private String companion1Id;
    
    @NotBlank(message = "Le deuxième compagnon est obligatoire")
    private String companion2Id;
    
    @NotBlank(message = "Le nom du nouveau compagnon est obligatoire")
    @Size(min = 2, max = 50)
    private String newCompanionName;
    
    /**
     * Ratio de traits du compagnon 1 vs compagnon 2 (0.0 à 1.0)
     * 0.5 = 50/50, 0.7 = 70% comp1 / 30% comp2
     */
    @Min(0) @Max(1)
    private double ratio = 0.5;
}

/**
 * DTO pour le résultat de fusion.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeResult {
    private CompanionResponse newCompanion;
    private List<String> inheritedTraitsFrom1;
    private List<String> inheritedTraitsFrom2;
    private String mergeReport;
}

/**
 * DTO pour la réponse paginée de compagnons publics.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCompanionsResponse {
    private List<CompanionResponse> companions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}

/**
 * DTO pour le résumé d'un compagnon (version light).
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionSummary {
    private String id;
    private String name;
    private String avatarImageUrl;
    private int likeCount;
    private Instant createdAt;
}