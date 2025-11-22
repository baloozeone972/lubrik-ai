// ============================================================================
// PACKAGE: com.nexusai.companion.service
// Description: Services métier pour la gestion des compagnons
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.domain.Companion;
import com.nexusai.companion.dto.*;
import com.nexusai.companion.exception.*;
import com.nexusai.companion.mapper.CompanionMapper;
import com.nexusai.companion.repository.CompanionRepository;
import com.nexusai.companion.repository.CustomCompanionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service principal pour la gestion des compagnons.
 * Contient toute la logique métier.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CompanionService {
    
    private final CompanionRepository companionRepository;
    private final CustomCompanionRepository customCompanionRepository;
    private final CompanionMapper companionMapper;
    private final QuotaService quotaService;
    private final GeneticService geneticService;
    private final EventPublisherService eventPublisher;
    private final StorageService storageService;
    
    /**
     * Crée un nouveau compagnon.
     * Vérifie les quotas utilisateur avant création.
     * 
     * @param userId ID de l'utilisateur
     * @param request Données du compagnon
     * @return CompanionResponse créé
     * @throws QuotaExceededException Si le quota est dépassé
     * @throws DuplicateNameException Si le nom existe déjà
     */
    @Transactional
    public CompanionResponse createCompanion(String userId, CreateCompanionRequest request) {
        log.info("Création d'un compagnon pour l'utilisateur: {}", userId);
        
        // 1. Vérifier le quota
        if (!quotaService.canCreateCompanion(userId)) {
            throw new QuotaExceededException(
                "Quota de compagnons atteint. Améliorez votre abonnement."
            );
        }
        
        // 2. Vérifier l'unicité du nom
        if (companionRepository.findByUserIdAndName(userId, request.getName()).isPresent()) {
            throw new DuplicateNameException(
                "Un compagnon avec ce nom existe déjà"
            );
        }
        
        // 3. Mapper le DTO vers l'entité
        Companion companion = companionMapper.toEntity(request);
        companion.setUserId(userId);
        
        // 4. Générer le profil génétique initial
        Companion.GeneticProfile geneticProfile = 
            geneticService.generateInitialGeneticProfile(companion.getPersonality());
        companion.setGeneticProfile(geneticProfile);
        
        // 5. Initialiser l'état émotionnel
        companion.setEmotionalState(
            Companion.EmotionalState.builder()
                .current("NEUTRAL")
                .intensity(50)
                .duration(0L)
                .build()
        );
        
        // 6. Définir les timestamps
        Instant now = Instant.now();
        companion.setCreatedAt(now);
        companion.setLastEvolutionDate(now);
        companion.setUpdatedAt(now);
        companion.setPublic(false);
        companion.setLikeCount(0);
        
        // 7. Sauvegarder
        Companion saved = companionRepository.save(companion);
        
        // 8. Publier événement
        eventPublisher.publishCompanionCreated(saved);
        
        log.info("Compagnon créé avec succès: {}", saved.getId());
        return companionMapper.toResponse(saved);
    }
    
    /**
     * Récupère un compagnon par son ID.
     * Vérifie que l'utilisateur a accès à ce compagnon.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     * @return CompanionResponse
     * @throws CompanionNotFoundException Si le compagnon n'existe pas
     * @throws UnauthorizedException Si l'utilisateur n'a pas accès
     */
    public CompanionResponse getCompanion(String companionId, String userId) {
        Companion companion = companionRepository.findById(companionId)
            .orElseThrow(() -> new CompanionNotFoundException(
                "Compagnon non trouvé: " + companionId
            ));
        
        // Vérifier l'accès (propriétaire ou public)
        if (!companion.getUserId().equals(userId) && !companion.isPublic()) {
            throw new UnauthorizedException(
                "Vous n'avez pas accès à ce compagnon"
            );
        }
        
        return companionMapper.toResponse(companion);
    }
    
    /**
     * Récupère tous les compagnons d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Liste de CompanionResponse
     */
    public List<CompanionResponse> getUserCompanions(String userId) {
        List<Companion> companions = companionRepository.findByUserId(userId);
        
        return companions.stream()
            .map(companionMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Met à jour un compagnon existant.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     * @param request Données de mise à jour
     * @return CompanionResponse mis à jour
     * @throws CompanionNotFoundException Si le compagnon n'existe pas
     * @throws UnauthorizedException Si l'utilisateur n'est pas propriétaire
     */
    @Transactional
    public CompanionResponse updateCompanion(
            String companionId, 
            String userId, 
            UpdateCompanionRequest request) {
        
        log.info("Mise à jour du compagnon: {}", companionId);
        
        // 1. Récupérer le compagnon
        Companion companion = companionRepository.findById(companionId)
            .orElseThrow(() -> new CompanionNotFoundException(
                "Compagnon non trouvé: " + companionId
            ));
        
        // 2. Vérifier la propriété
        if (!companion.getUserId().equals(userId)) {
            throw new UnauthorizedException(
                "Vous n'êtes pas propriétaire de ce compagnon"
            );
        }
        
        // 3. Appliquer les mises à jour
        if (request.getName() != null && !request.getName().equals(companion.getName())) {
            // Vérifier l'unicité du nouveau nom
            if (companionRepository.findByUserIdAndName(userId, request.getName()).isPresent()) {
                throw new DuplicateNameException(
                    "Un compagnon avec ce nom existe déjà"
                );
            }
            companion.setName(request.getName());
        }
        
        if (request.getAppearance() != null) {
            companion.setAppearance(
                companionMapper.toAppearanceEntity(request.getAppearance())
            );
        }
        
        if (request.getPersonality() != null) {
            companion.setPersonality(
                companionMapper.toPersonalityEntity(request.getPersonality())
            );
        }
        
        if (request.getVoice() != null) {
            companion.setVoice(
                companionMapper.toVoiceEntity(request.getVoice())
            );
        }
        
        if (request.getBackstory() != null) {
            companion.setBackstory(request.getBackstory());
        }
        
        if (request.getIsPublic() != null) {
            companion.setPublic(request.getIsPublic());
        }
        
        // 4. Mettre à jour le timestamp
        companion.setUpdatedAt(Instant.now());
        
        // 5. Sauvegarder
        Companion updated = companionRepository.save(companion);
        
        // 6. Publier événement
        eventPublisher.publishCompanionUpdated(updated);
        
        log.info("Compagnon mis à jour avec succès: {}", companionId);
        return companionMapper.toResponse(updated);
    }
    
    /**
     * Supprime un compagnon.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     * @throws CompanionNotFoundException Si le compagnon n'existe pas
     * @throws UnauthorizedException Si l'utilisateur n'est pas propriétaire
     */
    @Transactional
    public void deleteCompanion(String companionId, String userId) {
        log.info("Suppression du compagnon: {}", companionId);
        
        // 1. Récupérer le compagnon
        Companion companion = companionRepository.findById(companionId)
            .orElseThrow(() -> new CompanionNotFoundException(
                "Compagnon non trouvé: " + companionId
            ));
        
        // 2. Vérifier la propriété
        if (!companion.getUserId().equals(userId)) {
            throw new UnauthorizedException(
                "Vous n'êtes pas propriétaire de ce compagnon"
            );
        }
        
        // 3. Supprimer l'avatar du storage si présent
        if (companion.getAppearance() != null && 
            companion.getAppearance().getAvatarImageUrl() != null) {
            storageService.deleteFile(companion.getAppearance().getAvatarImageUrl());
        }
        
        // 4. Supprimer de la base
        companionRepository.deleteById(companionId);
        
        // 5. Publier événement
        eventPublisher.publishCompanionDeleted(companionId, userId);
        
        log.info("Compagnon supprimé avec succès: {}", companionId);
    }
    
    /**
     * Récupère les compagnons publics (galerie).
     * 
     * @param pageable Configuration de pagination
     * @return Page de compagnons publics
     */
    public PublicCompanionsResponse getPublicCompanions(Pageable pageable) {
        Page<Companion> page = companionRepository
            .findByIsPublicOrderByLikeCountDesc(true, pageable);
        
        List<CompanionResponse> companions = page.getContent().stream()
            .map(companionMapper::toResponse)
            .collect(Collectors.toList());
        
        return PublicCompanionsResponse.builder()
            .companions(companions)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .build();
    }
    
    /**
     * Recherche avancée de compagnons.
     * 
     * @param filters Map de filtres
     * @param pageable Configuration de pagination
     * @return Page de compagnons
     */
    public Page<CompanionResponse> advancedSearch(
            Map<String, Object> filters, 
            Pageable pageable) {
        
        Page<Companion> page = customCompanionRepository
            .advancedSearch(filters, pageable);
        
        return page.map(companionMapper::toResponse);
    }
}

// ============================================================================
// FICHIER: GeneticService.java
// Description: Service pour la gestion de l'évolution génétique
// ============================================================================

package com.nexusai.companion.service;

import com.nexusai.companion.domain.Companion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service gérant l'évolution génétique des compagnons.
 * Implémente un algorithme génétique simplifié.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GeneticService {
    
    private static final List<String> ALL_TRAITS = Arrays.asList(
        "openness", "conscientiousness", "extraversion", "agreeableness", 
        "neuroticism", "humor", "empathy", "jealousy", "curiosity", 
        "confidence", "playfulness", "assertiveness", "sensitivity", 
        "rationality", "creativity", "loyalty", "independence", 
        "patience", "adventurousness", "romanticism"
    );
    
    private static final Random random = new Random();
    
    /**
     * Génère un profil génétique initial basé sur la personnalité.
     * 
     * @param personality Personnalité du compagnon
     * @return GeneticProfile généré
     */
    public Companion.GeneticProfile generateInitialGeneticProfile(
            Companion.Personality personality) {
        
        log.debug("Génération du profil génétique initial");
        
        Map<String, Integer> genes = new HashMap<>();
        List<String> dominantTraits = new ArrayList<>();
        List<String> recessiveTraits = new ArrayList<>();
        
        // Extraire les valeurs de traits via réflexion
        Companion.Traits traits = personality.getTraits();
        Map<String, Integer> traitValues = extractTraitValues(traits);
        
        // Générer les gènes et identifier dominants/récessifs
        for (Map.Entry<String, Integer> entry : traitValues.entrySet()) {
            String traitName = entry.getKey();
            int traitValue = entry.getValue();
            
            // Gène = valeur du trait + variation aléatoire ±10
            int gene = traitValue + (random.nextInt(21) - 10);
            gene = Math.max(0, Math.min(100, gene));
            
            genes.put(traitName, gene);
            
            // Dominant si > 70, récessif si < 30
            if (gene > 70) {
                dominantTraits.add(traitName);
            } else if (gene < 30) {
                recessiveTraits.add(traitName);
            }
        }
        
        return Companion.GeneticProfile.builder()
            .genes(genes)
            .dominantTraits(dominantTraits)
            .recessiveTraits(recessiveTraits)
            .frozen(false)
            .frozenTraits(new ArrayList<>())
            .build();
    }
    
    /**
     * Fait évoluer un compagnon génétiquement.
     * 
     * @param companion Compagnon à faire évoluer
     * @param intensity Intensité de l'évolution (1-10)
     * @param targetTraits Traits spécifiques à cibler (optionnel)
     * @return Compagnon évolué
     */
    public Companion evolveCompanion(
            Companion companion, 
            int intensity, 
            List<String> targetTraits) {
        
        log.info("Évolution du compagnon: {} (intensité: {})", 
                 companion.getId(), intensity);
        
        Companion.GeneticProfile profile = companion.getGeneticProfile();
        
        if (profile.isFrozen()) {
            log.warn("Profil génétique gelé, évolution ignorée");
            return companion;
        }
        
        Map<String, Integer> genes = new HashMap<>(profile.getGenes());
        List<String> frozenTraits = profile.getFrozenTraits();
        
        // Déterminer le nombre de traits à modifier
        int maxChanges = Math.min((intensity + 1) / 2, 3);
        
        // Sélectionner les traits à modifier
        List<String> traitsToModify = targetTraits != null && !targetTraits.isEmpty()
            ? targetTraits
            : selectRandomTraits(genes.keySet(), frozenTraits, maxChanges);
        
        // Modifier les traits
        for (String trait : traitsToModify) {
            if (!frozenTraits.contains(trait)) {
                int currentValue = genes.get(trait);
                int change = (random.nextInt(intensity * 2 + 1) - intensity);
                int newValue = Math.max(0, Math.min(100, currentValue + change));
                
                genes.put(trait, newValue);
                log.debug("Trait {} évolué: {} -> {}", trait, currentValue, newValue);
            }
        }
        
        // Mettre à jour les traits dominants/récessifs
        List<String> newDominant = genes.entrySet().stream()
            .filter(e -> e.getValue() > 70)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        List<String> newRecessive = genes.entrySet().stream()
            .filter(e -> e.getValue() < 30)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        profile.setGenes(genes);
        profile.setDominantTraits(newDominant);
        profile.setRecessiveTraits(newRecessive);
        
        companion.setGeneticProfile(profile);
        companion.setLastEvolutionDate(Instant.now());
        
        return companion;
    }
    
    /**
     * Fusionne deux compagnons génétiquement.
     * 
     * @param companion1 Premier compagnon
     * @param companion2 Deuxième compagnon
     * @param ratio Ratio d'héritage (0.0 - 1.0)
     * @return Profil génétique fusionné
     */
    public Companion.GeneticProfile mergeGeneticProfiles(
            Companion companion1, 
            Companion companion2, 
            double ratio) {
        
        log.info("Fusion génétique de {} et {}", 
                 companion1.getId(), companion2.getId());
        
        Map<String, Integer> genes1 = companion1.getGeneticProfile().getGenes();
        Map<String, Integer> genes2 = companion2.getGeneticProfile().getGenes();
        Map<String, Integer> mergedGenes = new HashMap<>();
        
        // Fusion des gènes selon le ratio
        for (String trait : ALL_TRAITS) {
            int value1 = genes1.getOrDefault(trait, 50);
            int value2 = genes2.getOrDefault(trait, 50);
            
            // Interpolation pondérée
            int mergedValue = (int) (value1 * ratio + value2 * (1 - ratio));
            
            // Mutation aléatoire (5% de chance)
            if (random.nextDouble() < 0.05) {
                mergedValue += (random.nextInt(21) - 10);
                mergedValue = Math.max(0, Math.min(100, mergedValue));
            }
            
            mergedGenes.put(trait, mergedValue);
        }
        
        // Calculer dominants/récessifs
        List<String> dominantTraits = mergedGenes.entrySet().stream()
            .filter(e -> e.getValue() > 70)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        List<String> recessiveTraits = mergedGenes.entrySet().stream()
            .filter(e -> e.getValue() < 30)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        return Companion.GeneticProfile.builder()
            .genes(mergedGenes)
            .dominantTraits(dominantTraits)
            .recessiveTraits(recessiveTraits)
            .frozen(false)
            .frozenTraits(new ArrayList<>())
            .build();
    }
    
    // Méthodes utilitaires privées
    
    private Map<String, Integer> extractTraitValues(Companion.Traits traits) {
        Map<String, Integer> values = new HashMap<>();
        values.put("openness", traits.getOpenness());
        values.put("conscientiousness", traits.getConscientiousness());
        values.put("extraversion", traits.getExtraversion());
        values.put("agreeableness", traits.getAgreeableness());
        values.put("neuroticism", traits.getNeuroticism());
        values.put("humor", traits.getHumor());
        values.put("empathy", traits.getEmpathy());
        values.put("jealousy", traits.getJealousy());
        values.put("curiosity", traits.getCuriosity());
        values.put("confidence", traits.getConfidence());
        values.put("playfulness", traits.getPlayfulness());
        values.put("assertiveness", traits.getAssertiveness());
        values.put("sensitivity", traits.getSensitivity());
        values.put("rationality", traits.getRationality());
        values.put("creativity", traits.getCreativity());
        values.put("loyalty", traits.getLoyalty());
        values.put("independence", traits.getIndependence());
        values.put("patience", traits.getPatience());
        values.put("adventurousness", traits.getAdventurousness());
        values.put("romanticism", traits.getRomanticism());
        return values;
    }
    
    private List<String> selectRandomTraits(
            Set<String> allTraits, 
            List<String> frozenTraits, 
            int count) {
        
        List<String> available = allTraits.stream()
            .filter(t -> !frozenTraits.contains(t))
            .collect(Collectors.toList());
        
        Collections.shuffle(available);
        return available.stream()
            .limit(count)
            .collect(Collectors.toList());
    }
}