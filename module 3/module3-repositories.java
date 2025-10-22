// ============================================================================
// PACKAGE: com.nexusai.companion.repository
// Description: Repositories pour l'accès aux données MongoDB
// ============================================================================

package com.nexusai.companion.repository;

import com.nexusai.companion.domain.Companion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour les opérations CRUD sur les compagnons.
 * Spring Data MongoDB génère automatiquement l'implémentation.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Repository
public interface CompanionRepository extends MongoRepository<Companion, String> {
    
    /**
     * Trouve tous les compagnons d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @return Liste des compagnons
     */
    List<Companion> findByUserId(String userId);
    
    /**
     * Trouve un compagnon par utilisateur et nom.
     * Utile pour vérifier l'unicité du nom par utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param name Nom du compagnon
     * @return Optional contenant le compagnon si trouvé
     */
    Optional<Companion> findByUserIdAndName(String userId, String name);
    
    /**
     * Compte le nombre de compagnons d'un utilisateur.
     * Utilisé pour vérifier les quotas.
     * 
     * @param userId ID de l'utilisateur
     * @return Nombre de compagnons
     */
    long countByUserId(String userId);
    
    /**
     * Trouve tous les compagnons publics, paginés.
     * Triés par nombre de likes décroissant.
     * 
     * @param isPublic true pour les compagnons publics
     * @param pageable Configuration de pagination
     * @return Page de compagnons
     */
    Page<Companion> findByIsPublicOrderByLikeCountDesc(boolean isPublic, Pageable pageable);
    
    /**
     * Recherche de compagnons publics par nom (recherche partielle insensible à la casse).
     * 
     * @param name Nom recherché (regex)
     * @param isPublic true pour les compagnons publics
     * @param pageable Configuration de pagination
     * @return Page de compagnons
     */
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'isPublic': ?1 }")
    Page<Companion> searchByNameAndIsPublic(String name, boolean isPublic, Pageable pageable);
    
    /**
     * Trouve les compagnons nécessitant une évolution génétique.
     * (Non gelés et dernière évolution > X jours)
     * 
     * @param threshold Date seuil
     * @return Liste des compagnons à faire évoluer
     */
    @Query("{ 'geneticProfile.frozen': false, 'lastEvolutionDate': { $lt: ?0 } }")
    List<Companion> findCompanionsForEvolution(Instant threshold);
    
    /**
     * Supprime tous les compagnons d'un utilisateur.
     * Utilisé lors de la suppression d'un compte.
     * 
     * @param userId ID de l'utilisateur
     */
    void deleteByUserId(String userId);
}

// ============================================================================
// FICHIER: CompanionTemplateRepository.java
// Description: Repository pour les templates de compagnons
// ============================================================================

package com.nexusai.companion.repository;

import com.nexusai.companion.domain.CompanionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les templates de compagnons prédéfinis.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Repository
public interface CompanionTemplateRepository extends MongoRepository<CompanionTemplate, String> {
    
    /**
     * Trouve tous les templates d'une catégorie.
     * 
     * @param category Catégorie (REALISTIC, ANIME, etc.)
     * @param pageable Configuration de pagination
     * @return Page de templates
     */
    Page<CompanionTemplate> findByCategory(String category, Pageable pageable);
    
    /**
     * Trouve les templates contenant un tag spécifique.
     * 
     * @param tag Tag recherché
     * @param pageable Configuration de pagination
     * @return Page de templates
     */
    Page<CompanionTemplate> findByTagsContaining(String tag, Pageable pageable);
    
    /**
     * Trouve les templates les plus populaires (triés par usageCount).
     * 
     * @param pageable Configuration de pagination
     * @return Page de templates
     */
    Page<CompanionTemplate> findAllByOrderByUsageCountDesc(Pageable pageable);
    
    /**
     * Trouve les templates les mieux notés.
     * 
     * @param minRating Note minimale
     * @param pageable Configuration de pagination
     * @return Page de templates
     */
    Page<CompanionTemplate> findByAverageRatingGreaterThanEqualOrderByAverageRatingDesc(
        double minRating, 
        Pageable pageable
    );
}

// ============================================================================
// FICHIER: CompanionLikeRepository.java
// Description: Repository pour les likes de compagnons
// ============================================================================

package com.nexusai.companion.repository;

import com.nexusai.companion.domain.CompanionLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les likes de compagnons publics.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Repository
public interface CompanionLikeRepository extends MongoRepository<CompanionLike, String> {
    
    /**
     * Vérifie si un utilisateur a déjà liké un compagnon.
     * 
     * @param companionId ID du compagnon
     * @param userId ID de l'utilisateur
     * @return Optional contenant le like si existant
     */
    Optional<CompanionLike> findByCompanionIdAndUserId(String companionId, String userId);
    
    /**
     * Compte le nombre de likes d'un compagnon.
     * 
     * @param companionId ID du compagnon
     * @return Nombre de likes
     */
    long countByCompanionId(String companionId);
    
    /**
     * Supprime tous les likes d'un compagnon.
     * 
     * @param companionId ID du compagnon
     */
    void deleteByCompanionId(String companionId);
    
    /**
     * Supprime tous les likes d'un utilisateur.
     * 
     * @param userId ID de l'utilisateur
     */
    void deleteByUserId(String userId);
}

// ============================================================================
// FICHIER: CustomCompanionRepository.java
// Description: Repository personnalisé pour requêtes complexes
// ============================================================================

package com.nexusai.companion.repository;

import com.nexusai.companion.domain.Companion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Interface pour les requêtes personnalisées complexes sur les compagnons.
 * L'implémentation sera dans CustomCompanionRepositoryImpl.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
public interface CustomCompanionRepository {
    
    /**
     * Recherche avancée de compagnons avec filtres multiples.
     * 
     * @param filters Map de filtres (gender, category, minAge, maxAge, etc.)
     * @param pageable Configuration de pagination
     * @return Page de compagnons correspondants
     */
    Page<Companion> advancedSearch(Map<String, Object> filters, Pageable pageable);
    
    /**
     * Mise à jour partielle d'un compagnon.
     * Permet de mettre à jour uniquement certains champs.
     * 
     * @param companionId ID du compagnon
     * @param updates Map des champs à mettre à jour
     * @return true si la mise à jour a réussi
     */
    boolean partialUpdate(String companionId, Map<String, Object> updates);
    
    /**
     * Incrémente le compteur de likes d'un compagnon.
     * Opération atomique thread-safe.
     * 
     * @param companionId ID du compagnon
     * @return Nouveau nombre de likes
     */
    int incrementLikeCount(String companionId);
    
    /**
     * Décrémente le compteur de likes d'un compagnon.
     * 
     * @param companionId ID du compagnon
     * @return Nouveau nombre de likes
     */
    int decrementLikeCount(String companionId);
}

// ============================================================================
// FICHIER: CustomCompanionRepositoryImpl.java
// Description: Implémentation du repository personnalisé
// ============================================================================

package com.nexusai.companion.repository;

import com.nexusai.companion.domain.Companion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implémentation des requêtes personnalisées complexes.
 * Utilise MongoTemplate pour des opérations avancées.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@Repository
@RequiredArgsConstructor
public class CustomCompanionRepositoryImpl implements CustomCompanionRepository {
    
    private final MongoTemplate mongoTemplate;
    
    @Override
    public Page<Companion> advancedSearch(Map<String, Object> filters, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();
        
        // Filtres dynamiques
        if (filters.containsKey("gender")) {
            criteriaList.add(Criteria.where("appearance.gender").is(filters.get("gender")));
        }
        
        if (filters.containsKey("minAge")) {
            criteriaList.add(Criteria.where("appearance.age").gte(filters.get("minAge")));
        }
        
        if (filters.containsKey("maxAge")) {
            criteriaList.add(Criteria.where("appearance.age").lte(filters.get("maxAge")));
        }
        
        if (filters.containsKey("isPublic")) {
            criteriaList.add(Criteria.where("isPublic").is(filters.get("isPublic")));
        }
        
        if (filters.containsKey("minLikes")) {
            criteriaList.add(Criteria.where("likeCount").gte(filters.get("minLikes")));
        }
        
        // Filtres sur personnalité
        if (filters.containsKey("minEmpathy")) {
            criteriaList.add(
                Criteria.where("personality.traits.empathy").gte(filters.get("minEmpathy"))
            );
        }
        
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(
                criteriaList.toArray(new Criteria[0])
            ));
        }
        
        // Pagination
        long total = mongoTemplate.count(query, Companion.class);
        query.with(pageable);
        
        List<Companion> companions = mongoTemplate.find(query, Companion.class);
        
        return new PageImpl<>(companions, pageable, total);
    }
    
    @Override
    public boolean partialUpdate(String companionId, Map<String, Object> updates) {
        Query query = new Query(Criteria.where("id").is(companionId));
        Update update = new Update();
        
        updates.forEach(update::set);
        
        var result = mongoTemplate.updateFirst(query, update, Companion.class);
        return result.getModifiedCount() > 0;
    }
    
    @Override
    public int incrementLikeCount(String companionId) {
        Query query = new Query(Criteria.where("id").is(companionId));
        Update update = new Update().inc("likeCount", 1);
        
        mongoTemplate.updateFirst(query, update, Companion.class);
        
        Companion updated = mongoTemplate.findOne(query, Companion.class);
        return updated != null ? updated.getLikeCount() : 0;
    }
    
    @Override
    public int decrementLikeCount(String companionId) {
        Query query = new Query(Criteria.where("id").is(companionId));
        Update update = new Update().inc("likeCount", -1);
        
        mongoTemplate.updateFirst(query, update, Companion.class);
        
        Companion updated = mongoTemplate.findOne(query, Companion.class);
        return updated != null ? updated.getLikeCount() : 0;
    }
}