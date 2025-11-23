// ============================================================================
// PACKAGE: com.nexusai.companion.service
// Description: Services métier additionnels
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.domain.Companion;
import com.nexusai.companion.dto.*;
import com.nexusai.companion.exception.*;
import com.nexusai.companion.mapper.CompanionMapper;
import com.nexusai.companion.repository.CompanionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service pour les opérations d'évolution génétique des compagnons.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EvolutionService {
    
    private final CompanionRepository companionRepository;
    private final GeneticService geneticService;
    private final CompanionMapper companionMapper;
    private final EventPublisherService eventPublisher;
    
    /**
     * Fait évoluer un compagnon selon les paramètres fournis.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     * @param request Paramètres d'évolution
     * @return CompanionResponse évolué
     */
    @Transactional
    public CompanionResponse evolveCompanion(
            String companionId,
            String userId,
            EvolveCompanionRequest request) {
        
        log.info("Évolution du compagnon {} pour l'utilisateur {}", companionId, userId);
        
        // 1. Récupérer et vérifier le compagnon
        Companion companion = companionRepository.findById(companionId)
            .orElseThrow(() -> new CompanionNotFoundException(
                "Compagnon non trouvé: " + companionId
            ));
        
        if (!companion.getUserId().equals(userId)) {
            throw new UnauthorizedException(
                "Vous n'êtes pas propriétaire de ce compagnon"
            );
        }
        
        if (companion.getGeneticProfile().isFrozen()) {
            throw new IllegalStateException(
                "Le profil génétique est complètement gelé"
            );
        }
        
        // 2. Appliquer l'évolution
        Companion evolved = geneticService.evolveCompanion(
            companion,
            request.getIntensity(),
            request.getTargetTraits()
        );
        
        // 3. Mettre à jour les traits de personnalité basés sur les nouveaux gènes
        updatePersonalityFromGenes(evolved);
        
        // 4. Sauvegarder
        evolved.setUpdatedAt(Instant.now());
        Companion saved = companionRepository.save(evolved);
        
        // 5. Publier événement
        eventPublisher.publishCompanionEvolved(saved);
        
        log.info("Compagnon évolué avec succès: {}", companionId);
        return companionMapper.toResponse(saved);
    }
    
    /**
     * Gèle des traits génétiques pour empêcher leur évolution.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     * @param request Traits à geler
     * @return CompanionResponse mis à jour
     */
    @Transactional
    public CompanionResponse freezeTraits(
            String companionId,
            String userId,
            FreezeTraitsRequest request) {
        
        log.info("Gel de traits pour le compagnon {}", companionId);
        
        // 1. Récupérer et vérifier le compagnon
        Companion companion = companionRepository.findById(companionId)
            .orElseThrow(() -> new CompanionNotFoundException(
                "Compagnon non trouvé: " + companionId
            ));
        
        if (!companion.getUserId().equals(userId)) {
            throw new UnauthorizedException(
                "Vous n'êtes pas propriétaire de ce compagnon"
            );
        }
        
        // 2. Appliquer le gel
        Companion.GeneticProfile profile = companion.getGeneticProfile();
        
        if (request.isFreezeAll()) {
            profile.setFrozen(true);
            log.info("Profil génétique complètement gelé");
        } else {
            List<String> frozenTraits = new ArrayList<>(profile.getFrozenTraits());
            frozenTraits.addAll(request.getTraits());
            profile.setFrozenTraits(frozenTraits);
            log.info("Traits gelés: {}", request.getTraits());
        }
        
        companion.setGeneticProfile(profile);
        companion.setUpdatedAt(Instant.now());
        
        // 3. Sauvegarder
        Companion saved = companionRepository.save(companion);
        
        return companionMapper.toResponse(saved);
    }
    
    /**
     * Fusionne deux compagnons pour en créer un nouveau.
     * 
     * @param userId ID de l'utilisateur
     * @param request Paramètres de fusion
     * @return MergeResult contenant le nouveau compagnon et les détails
     */
    @Transactional
    public MergeResult mergeCompanions(String userId, MergeCompanionsRequest request) {
        
        log.info("Fusion des compagnons {} et {}", 
                 request.getCompanion1Id(), request.getCompanion2Id());
        
        // 1. Récupérer les deux compagnons
        Companion companion1 = companionRepository.findById(request.getCompanion1Id())
            .orElseThrow(() -> new CompanionNotFoundException(
                "Compagnon 1 non trouvé"
            ));
        
        Companion companion2 = companionRepository.findById(request.getCompanion2Id())
            .orElseThrow(() -> new CompanionNotFoundException(
                "Compagnon 2 non trouvé"
            ));
        
        // 2. Vérifier la propriété
        if (!companion1.getUserId().equals(userId) || 
            !companion2.getUserId().equals(userId)) {
            throw new UnauthorizedException(
                "Vous devez être propriétaire des deux compagnons"
            );
        }
        
        // 3. Créer le profil génétique fusionné
        Companion.GeneticProfile mergedProfile = geneticService.mergeGeneticProfiles(
            companion1,
            companion2,
            request.getRatio()
        );
        
        // 4. Créer le nouveau compagnon
        Companion merged = Companion.builder()
            .userId(userId)
            .name(request.getNewCompanionName())
            .appearance(mergeAppearances(companion1.getAppearance(), 
                                         companion2.getAppearance(), 
                                         request.getRatio()))
            .personality(mergePersonalities(companion1.getPersonality(), 
                                           companion2.getPersonality(), 
                                           request.getRatio()))
            .voice(companion1.getVoice()) // Prendre la voix du premier
            .backstory(mergeBackstories(companion1.getBackstory(), 
                                       companion2.getBackstory()))
            .geneticProfile(mergedProfile)
            .emotionalState(Companion.EmotionalState.builder()
                .current("CURIOUS")
                .intensity(70)
                .duration(0L)
                .build())
            .isPublic(false)
            .likeCount(0)
            .createdAt(Instant.now())
            .lastEvolutionDate(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        // 5. Sauvegarder
        Companion saved = companionRepository.save(merged);
        
        // 6. Construire le résultat détaillé
        List<String> inheritedFrom1 = mergedProfile.getDominantTraits().stream()
            .filter(t -> companion1.getGeneticProfile().getDominantTraits().contains(t))
            .collect(Collectors.toList());
        
        List<String> inheritedFrom2 = mergedProfile.getDominantTraits().stream()
            .filter(t -> companion2.getGeneticProfile().getDominantTraits().contains(t))
            .collect(Collectors.toList());
        
        String report = generateMergeReport(companion1, companion2, merged, request.getRatio());
        
        log.info("Fusion réussie, nouveau compagnon créé: {}", saved.getId());
        
        return MergeResult.builder()
            .newCompanion(companionMapper.toResponse(saved))
            .inheritedTraitsFrom1(inheritedFrom1)
            .inheritedTraitsFrom2(inheritedFrom2)
            .mergeReport(report)
            .build();
    }
    
    // Méthodes utilitaires privées
    
    /**
     * Met à jour la personnalité basée sur les gènes.
     */
    private void updatePersonalityFromGenes(Companion companion) {
        Map<String, Integer> genes = companion.getGeneticProfile().getGenes();
        Companion.Traits traits = companion.getPersonality().getTraits();
        
        // Appliquer les valeurs génétiques aux traits de personnalité
        traits.setOpenness(genes.getOrDefault("openness", traits.getOpenness()));
        traits.setConscientiousness(genes.getOrDefault("conscientiousness", traits.getConscientiousness()));
        traits.setExtraversion(genes.getOrDefault("extraversion", traits.getExtraversion()));
        traits.setAgreeableness(genes.getOrDefault("agreeableness", traits.getAgreeableness()));
        traits.setNeuroticism(genes.getOrDefault("neuroticism", traits.getNeuroticism()));
        traits.setHumor(genes.getOrDefault("humor", traits.getHumor()));
        traits.setEmpathy(genes.getOrDefault("empathy", traits.getEmpathy()));
        traits.setJealousy(genes.getOrDefault("jealousy", traits.getJealousy()));
        traits.setCuriosity(genes.getOrDefault("curiosity", traits.getCuriosity()));
        traits.setConfidence(genes.getOrDefault("confidence", traits.getConfidence()));
        traits.setPlayfulness(genes.getOrDefault("playfulness", traits.getPlayfulness()));
        traits.setAssertiveness(genes.getOrDefault("assertiveness", traits.getAssertiveness()));
        traits.setSensitivity(genes.getOrDefault("sensitivity", traits.getSensitivity()));
        traits.setRationality(genes.getOrDefault("rationality", traits.getRationality()));
        traits.setCreativity(genes.getOrDefault("creativity", traits.getCreativity()));
        traits.setLoyalty(genes.getOrDefault("loyalty", traits.getLoyalty()));
        traits.setIndependence(genes.getOrDefault("independence", traits.getIndependence()));
        traits.setPatience(genes.getOrDefault("patience", traits.getPatience()));
        traits.setAdventurousness(genes.getOrDefault("adventurousness", traits.getAdventurousness()));
        traits.setRomanticism(genes.getOrDefault("romanticism", traits.getRomanticism()));
    }
    
    /**
     * Fusionne les apparences de deux compagnons.
     */
    private Companion.Appearance mergeAppearances(
            Companion.Appearance app1,
            Companion.Appearance app2,
            double ratio) {
        
        // Utiliser l'apparence du compagnon dominant basé sur le ratio
        Companion.Appearance base = ratio > 0.5 ? app1 : app2;
        
        return Companion.Appearance.builder()
            .gender(base.getGender())
            .hairColor(ratio > 0.5 ? app1.getHairColor() : app2.getHairColor())
            .eyeColor(ratio > 0.5 ? app1.getEyeColor() : app2.getEyeColor())
            .skinTone(base.getSkinTone())
            .bodyType(base.getBodyType())
            .age((int) (app1.getAge() * ratio + app2.getAge() * (1 - ratio)))
            .customFeatures(null)
            .build();
    }
    
    /**
     * Fusionne les personnalités de deux compagnons.
     */
    private Companion.Personality mergePersonalities(
            Companion.Personality pers1,
            Companion.Personality pers2,
            double ratio) {
        
        // Fusionner les traits
        Companion.Traits traits1 = pers1.getTraits();
        Companion.Traits traits2 = pers2.getTraits();
        
        Companion.Traits mergedTraits = Companion.Traits.builder()
            .openness(merge(traits1.getOpenness(), traits2.getOpenness(), ratio))
            .conscientiousness(merge(traits1.getConscientiousness(), traits2.getConscientiousness(), ratio))
            .extraversion(merge(traits1.getExtraversion(), traits2.getExtraversion(), ratio))
            .agreeableness(merge(traits1.getAgreeableness(), traits2.getAgreeableness(), ratio))
            .neuroticism(merge(traits1.getNeuroticism(), traits2.getNeuroticism(), ratio))
            .humor(merge(traits1.getHumor(), traits2.getHumor(), ratio))
            .empathy(merge(traits1.getEmpathy(), traits2.getEmpathy(), ratio))
            .jealousy(merge(traits1.getJealousy(), traits2.getJealousy(), ratio))
            .curiosity(merge(traits1.getCuriosity(), traits2.getCuriosity(), ratio))
            .confidence(merge(traits1.getConfidence(), traits2.getConfidence(), ratio))
            .playfulness(merge(traits1.getPlayfulness(), traits2.getPlayfulness(), ratio))
            .assertiveness(merge(traits1.getAssertiveness(), traits2.getAssertiveness(), ratio))
            .sensitivity(merge(traits1.getSensitivity(), traits2.getSensitivity(), ratio))
            .rationality(merge(traits1.getRationality(), traits2.getRationality(), ratio))
            .creativity(merge(traits1.getCreativity(), traits2.getCreativity(), ratio))
            .loyalty(merge(traits1.getLoyalty(), traits2.getLoyalty(), ratio))
            .independence(merge(traits1.getIndependence(), traits2.getIndependence(), ratio))
            .patience(merge(traits1.getPatience(), traits2.getPatience(), ratio))
            .adventurousness(merge(traits1.getAdventurousness(), traits2.getAdventurousness(), ratio))
            .romanticism(merge(traits1.getRomanticism(), traits2.getRomanticism(), ratio))
            .build();
        
        // Combiner intérêts
        List<String> mergedInterests = new ArrayList<>(pers1.getInterests());
        mergedInterests.addAll(pers2.getInterests());
        
        return Companion.Personality.builder()
            .traits(mergedTraits)
            .interests(mergedInterests.stream().distinct().collect(Collectors.toList()))
            .dislikes(new ArrayList<>())
            .humorStyle(ratio > 0.5 ? pers1.getHumorStyle() : pers2.getHumorStyle())
            .communicationStyle(ratio > 0.5 ? pers1.getCommunicationStyle() : pers2.getCommunicationStyle())
            .build();
    }
    
    private int merge(int value1, int value2, double ratio) {
        return (int) (value1 * ratio + value2 * (1 - ratio));
    }
    
    /**
     * Fusionne les backstories.
     */
    private String mergeBackstories(String backstory1, String backstory2) {
        return String.format(
            "Né de la fusion de deux esprits distincts, ce compagnon unique combine " +
            "les qualités de ses prédécesseurs. %s et %s",
            backstory1 != null ? backstory1.substring(0, Math.min(100, backstory1.length())) : "",
            backstory2 != null ? backstory2.substring(0, Math.min(100, backstory2.length())) : ""
        );
    }
    
    /**
     * Génère un rapport détaillé de la fusion.
     */
    private String generateMergeReport(
            Companion comp1,
            Companion comp2,
            Companion merged,
            double ratio) {
        
        return String.format(
            "=== RAPPORT DE FUSION ===\n\n" +
            "Compagnon 1: %s (contribution: %.0f%%)\n" +
            "Compagnon 2: %s (contribution: %.0f%%)\n\n" +
            "Nouveau compagnon: %s\n\n" +
            "Traits dominants hérités:\n" +
            "- De %s: %d traits\n" +
            "- De %s: %d traits\n\n" +
            "Le nouveau compagnon possède une personnalité unique, " +
            "combinant harmonieusement les caractéristiques de ses prédécesseurs.",
            comp1.getName(), ratio * 100,
            comp2.getName(), (1 - ratio) * 100,
            merged.getName(),
            comp1.getName(), (int) (merged.getGeneticProfile().getDominantTraits().size() * ratio),
            comp2.getName(), (int) (merged.getGeneticProfile().getDominantTraits().size() * (1 - ratio))
        );
    }
}

// ============================================================================
// FICHIER: TemplateService.java
// Description: Service pour la gestion des templates de compagnons
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.domain.Companion;
import com.nexusai.companion.domain.CompanionTemplate;
import com.nexusai.companion.dto.CompanionResponse;
import com.nexusai.companion.dto.CreateCompanionRequest;
import com.nexusai.companion.exception.CompanionNotFoundException;
import com.nexusai.companion.mapper.CompanionMapper;
import com.nexusai.companion.repository.CompanionTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des templates de compagnons prédéfinis.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateService {
    
    private final CompanionTemplateRepository templateRepository;
    private final CompanionService companionService;
    private final CompanionMapper companionMapper;
    
    /**
     * Récupère tous les templates disponibles.
     */
    public Page<CompanionTemplate> getAllTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable);
    }
    
    /**
     * Récupère les templates d'une catégorie.
     */
    public Page<CompanionTemplate> getTemplatesByCategory(
            String category, 
            Pageable pageable) {
        
        return templateRepository.findByCategory(category, pageable);
    }
    
    /**
     * Récupère les templates par tag.
     */
    public Page<CompanionTemplate> getTemplatesByTag(String tag, Pageable pageable) {
        return templateRepository.findByTagsContaining(tag, pageable);
    }
    
    /**
     * Récupère les templates populaires.
     */
    public Page<CompanionTemplate> getPopularTemplates(Pageable pageable) {
        return templateRepository.findAllByOrderByUsageCountDesc(pageable);
    }
    
    /**
     * Crée un compagnon à partir d'un template.
     */
    @Transactional
    public CompanionResponse createCompanionFromTemplate(
            String templateId,
            String userId,
            String name) {
        
        log.info("Création d'un compagnon depuis le template {} pour l'utilisateur {}", 
                 templateId, userId);
        
        // 1. Récupérer le template
        CompanionTemplate template = templateRepository.findById(templateId)
            .orElseThrow(() -> new CompanionNotFoundException(
                "Template non trouvé: " + templateId
            ));
        
        // 2. Créer la requête depuis le template
        CreateCompanionRequest request = CreateCompanionRequest.builder()
            .name(name)
            .appearance(convertAppearance(template.getDefaultAppearance()))
            .personality(convertPersonality(template.getDefaultPersonality()))
            .voice(convertVoice(template.getDefaultVoice()))
            .backstory(template.getSuggestedBackstory())
            .templateId(templateId)
            .build();
        
        // 3. Créer le compagnon
        CompanionResponse response = companionService.createCompanion(userId, request);
        
        // 4. Incrémenter le compteur d'utilisation du template
        template.setUsageCount(template.getUsageCount() + 1);
        templateRepository.save(template);
        
        log.info("Compagnon créé depuis template: {}", response.getId());
        return response;
    }
    
    // Méthodes de conversion privées
    
    private com.nexusai.companion.dto.AppearanceDto convertAppearance(
            Companion.Appearance appearance) {
        
        return com.nexusai.companion.dto.AppearanceDto.builder()
            .gender(appearance.getGender())
            .hairColor(appearance.getHairColor())
            .eyeColor(appearance.getEyeColor())
            .skinTone(appearance.getSkinTone())
            .bodyType(appearance.getBodyType())
            .age(appearance.getAge())
            .avatarImageUrl(appearance.getAvatarImageUrl())
            .customFeatures(appearance.getCustomFeatures())
            .build();
    }
    
    private com.nexusai.companion.dto.PersonalityDto convertPersonality(
            Companion.Personality personality) {
        
        Companion.Traits traits = personality.getTraits();
        
        return com.nexusai.companion.dto.PersonalityDto.builder()
            .traits(com.nexusai.companion.dto.TraitsDto.builder()
                .openness(traits.getOpenness())
                .conscientiousness(traits.getConscientiousness())
                .extraversion(traits.getExtraversion())
                .agreeableness(traits.getAgreeableness())
                .neuroticism(traits.getNeuroticism())
                .humor(traits.getHumor())
                .empathy(traits.getEmpathy())
                .jealousy(traits.getJealousy())
                .curiosity(traits.getCuriosity())
                .confidence(traits.getConfidence())
                .playfulness(traits.getPlayfulness())
                .assertiveness(traits.getAssertiveness())
                .sensitivity(traits.getSensitivity())
                .rationality(traits.getRationality())
                .creativity(traits.getCreativity())
                .loyalty(traits.getLoyalty())
                .independence(traits.getIndependence())
                .patience(traits.getPatience())
                .adventurousness(traits.getAdventurousness())
                .romanticism(traits.getRomanticism())
                .build())
            .interests(personality.getInterests())
            .dislikes(personality.getDislikes())
            .humorStyle(personality.getHumorStyle())
            .communicationStyle(personality.getCommunicationStyle())
            .build();
    }
    
    private com.nexusai.companion.dto.VoiceDto convertVoice(Companion.Voice voice) {
        return com.nexusai.companion.dto.VoiceDto.builder()
            .voiceId(voice.getVoiceId())
            .pitch(voice.getPitch())
            .speed(voice.getSpeed())
            .style(voice.getStyle())
            .build();
    }
}

// ============================================================================
// FICHIER: LikeService.java
// Description: Service pour la gestion des likes de compagnons
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.domain.Companion;
import com.nexusai.companion.domain.CompanionLike;
import com.nexusai.companion.exception.CompanionNotFoundException;
import com.nexusai.companion.repository.CompanionLikeRepository;
import com.nexusai.companion.repository.CompanionRepository;
import com.nexusai.companion.repository.CustomCompanionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service de gestion des likes de compagnons publics.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LikeService {
    
    private final CompanionRepository companionRepository;
    private final CompanionLikeRepository likeRepository;
    private final CustomCompanionRepository customCompanionRepository;
    
    /**
     * Like un compagnon public.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     */
    @Transactional
    public void likeCompanion(String companionId, String userId) {
        log.info("Like du compagnon {} par l'utilisateur {}", companionId, userId);
        
        // 1. Vérifier que le compagnon existe et est public
        Companion companion = companionRepository.findById(companionId)
            .orElseThrow(() -> new CompanionNotFoundException(
                "Compagnon non trouvé: " + companionId
            ));
        
        if (!companion.isPublic()) {
            throw new IllegalStateException(
                "Le compagnon doit être public pour être liké"
            );
        }
        
        // 2. Vérifier si déjà liké
        if (likeRepository.findByCompanionIdAndUserId(companionId, userId).isPresent()) {
            log.debug("Compagnon déjà liké par cet utilisateur");
            return; // Idempotent
        }
        
        // 3. Créer le like
        CompanionLike like = CompanionLike.builder()
            .companionId(companionId)
            .userId(userId)
            .likedAt(Instant.now())
            .build();
        
        likeRepository.save(like);
        
        // 4. Incrémenter le compteur
        customCompanionRepository.incrementLikeCount(companionId);
        
        log.info("Like enregistré avec succès");
    }
    
    /**
     * Unlike un compagnon public.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     */
    @Transactional
    public void unlikeCompanion(String companionId, String userId) {
        log.info("Unlike du compagnon {} par l'utilisateur {}", companionId, userId);
        
        // 1. Trouver le like
        CompanionLike like = likeRepository
            .findByCompanionIdAndUserId(companionId, userId)
            .orElse(null);
        
        if (like == null) {
            log.debug("Aucun like à retirer");
            return; // Idempotent
        }
        
        // 2. Supprimer le like
        likeRepository.delete(like);
        
        // 3. Décrémenter le compteur
        customCompanionRepository.decrementLikeCount(companionId);
        
        log.info("Unlike enregistré avec succès");
    }
    
    /**
     * Vérifie si un utilisateur a liké un compagnon.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     * @return true si l'utilisateur a liké
     */
    public boolean hasUserLiked(String companionId, String userId) {
        return likeRepository
            .findByCompanionIdAndUserId(companionId, userId)
            .isPresent();
    }
}