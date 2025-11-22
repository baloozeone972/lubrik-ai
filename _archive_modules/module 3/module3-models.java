// ============================================================================
// PACKAGE: com.nexusai.companion.domain
// Description: Modèles de domaine MongoDB pour les compagnons
// ============================================================================

package com.nexusai.companion.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entité principale représentant un compagnon IA.
 * Stockée dans MongoDB pour sa flexibilité avec les documents JSON.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Document(collection = "companions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Companion {
    
    @Id
    private String id;
    
    /**
     * Identifiant de l'utilisateur propriétaire
     */
    @Indexed
    private String userId;
    
    /**
     * Nom du compagnon (unique par utilisateur)
     */
    @Indexed
    private String name;
    
    /**
     * Configuration d'apparence physique
     */
    private Appearance appearance;
    
    /**
     * Configuration de personnalité (20+ traits)
     */
    private Personality personality;
    
    /**
     * Configuration vocale
     */
    private Voice voice;
    
    /**
     * Histoire personnelle du compagnon
     */
    private String backstory;
    
    /**
     * Profil génétique pour l'évolution
     */
    private GeneticProfile geneticProfile;
    
    /**
     * État émotionnel actuel
     */
    private EmotionalState emotionalState;
    
    /**
     * Visibilité publique dans la galerie
     */
    @Indexed
    private boolean isPublic;
    
    /**
     * Nombre de likes (si public)
     */
    private int likeCount;
    
    /**
     * Date de création
     */
    private Instant createdAt;
    
    /**
     * Date de dernière évolution génétique
     */
    private Instant lastEvolutionDate;
    
    /**
     * Date de dernière modification
     */
    private Instant updatedAt;
    
    /**
     * Classe interne : Apparence physique
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Appearance {
        private String gender;
        private String hairColor;
        private String eyeColor;
        private String skinTone;
        private String bodyType;
        private Integer age;
        private String avatarImageUrl;
        
        /**
         * Caractéristiques personnalisées additionnelles
         */
        private Map<String, Object> customFeatures;
    }
    
    /**
     * Classe interne : Personnalité (Big Five + traits additionnels)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Personality {
        /**
         * Traits principaux (valeurs 0-100)
         */
        private Traits traits;
        
        /**
         * Centres d'intérêt
         */
        private List<String> interests;
        
        /**
         * Aversions
         */
        private List<String> dislikes;
        
        /**
         * Style d'humour
         */
        private String humorStyle;
        
        /**
         * Style de communication
         */
        private String communicationStyle;
    }
    
    /**
     * Classe interne : Traits de personnalité
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Traits {
        // Big Five
        private int openness;
        private int conscientiousness;
        private int extraversion;
        private int agreeableness;
        private int neuroticism;
        
        // Traits additionnels
        private int humor;
        private int empathy;
        private int jealousy;
        private int curiosity;
        private int confidence;
        private int playfulness;
        private int assertiveness;
        private int sensitivity;
        private int rationality;
        private int creativity;
        private int loyalty;
        private int independence;
        private int patience;
        private int adventurousness;
        private int romanticism;
    }
    
    /**
     * Classe interne : Configuration vocale
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Voice {
        private String voiceId;
        private float pitch;
        private float speed;
        private String style;
    }
    
    /**
     * Classe interne : Profil génétique
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneticProfile {
        /**
         * Gènes encodés (Map de trait -> valeur génétique)
         */
        private Map<String, Integer> genes;
        
        /**
         * Traits dominants
         */
        private List<String> dominantTraits;
        
        /**
         * Traits récessifs
         */
        private List<String> recessiveTraits;
        
        /**
         * Indique si le profil est gelé (pas d'évolution)
         */
        private boolean frozen;
        
        /**
         * Traits gelés individuellement
         */
        private List<String> frozenTraits;
    }
    
    /**
     * Classe interne : État émotionnel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionalState {
        /**
         * Émotion courante (HAPPY, SAD, ANGRY, etc.)
         */
        private String current;
        
        /**
         * Intensité (0-100)
         */
        private int intensity;
        
        /**
         * Durée en secondes
         */
        private long duration;
    }
}

// ============================================================================
// FICHIER: CompanionTemplate.java
// Description: Modèle prédéfini de compagnon
// ============================================================================

package com.nexusai.companion.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Template de compagnon prédéfini pour création rapide.
 * Contient 1000+ configurations préparées.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Document(collection = "companion_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionTemplate {
    
    @Id
    private String id;
    
    /**
     * Nom du template
     */
    @Indexed
    private String name;
    
    /**
     * Description
     */
    private String description;
    
    /**
     * Catégorie (REALISTIC, ANIME, FANTASY, etc.)
     */
    @Indexed
    private String category;
    
    /**
     * Tags de recherche
     */
    private List<String> tags;
    
    /**
     * URL de l'image de prévisualisation
     */
    private String thumbnailUrl;
    
    /**
     * Configuration d'apparence par défaut
     */
    private Companion.Appearance defaultAppearance;
    
    /**
     * Configuration de personnalité par défaut
     */
    private Companion.Personality defaultPersonality;
    
    /**
     * Configuration vocale par défaut
     */
    private Companion.Voice defaultVoice;
    
    /**
     * Backstory suggérée
     */
    private String suggestedBackstory;
    
    /**
     * Nombre d'utilisations
     */
    private int usageCount;
    
    /**
     * Note moyenne (1-5)
     */
    private double averageRating;
}

// ============================================================================
// FICHIER: CompanionLike.java
// Description: Like d'un compagnon public
// ============================================================================

package com.nexusai.companion.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Représente un like sur un compagnon public.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Document(collection = "companion_likes")
@CompoundIndex(name = "companion_user_idx", 
               def = "{'companionId': 1, 'userId': 1}", 
               unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionLike {
    
    @Id
    private String id;
    
    /**
     * ID du compagnon liké
     */
    private String companionId;
    
    /**
     * ID de l'utilisateur qui like
     */
    private String userId;
    
    /**
     * Date du like
     */
    private Instant likedAt;
}