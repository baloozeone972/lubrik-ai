// ============================================================================
// PACKAGE: com.nexusai.companion.controller
// Description: Contrôleurs REST pour l'API des compagnons
// ============================================================================

package com.nexusai.companion.controller;

import com.nexusai.companion.dto.*;
import com.nexusai.companion.service.CompanionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur principal pour la gestion des compagnons.
 * Expose les endpoints REST pour le CRUD et les opérations avancées.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/companions")
@Tag(name = "Companions", description = "API de gestion des compagnons IA")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
@Slf4j
public class CompanionController {
    
    private final CompanionService companionService;
    
    /**
     * Crée un nouveau compagnon.
     * 
     * POST /api/v1/companions
     */
    @PostMapping
    @Operation(
        summary = "Créer un compagnon",
        description = "Crée un nouveau compagnon pour l'utilisateur authentifié"
    )
    @ApiResponse(responseCode = "201", description = "Compagnon créé avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "403", description = "Quota dépassé")
    public ResponseEntity<CompanionResponse> createCompanion(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateCompanionRequest request) {
        
        log.info("POST /api/v1/companions - userId: {}", userId);
        CompanionResponse response = companionService.createCompanion(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Récupère un compagnon par ID.
     * 
     * GET /api/v1/companions/{id}
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Récupérer un compagnon",
        description = "Récupère les détails d'un compagnon par son ID"
    )
    @ApiResponse(responseCode = "200", description = "Compagnon trouvé")
    @ApiResponse(responseCode = "404", description = "Compagnon non trouvé")
    public ResponseEntity<CompanionResponse> getCompanion(
            @Parameter(description = "ID du compagnon")
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        
        log.info("GET /api/v1/companions/{} - userId: {}", id, userId);
        CompanionResponse response = companionService.getCompanion(id, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Met à jour un compagnon existant.
     * 
     * PUT /api/v1/companions/{id}
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Mettre à jour un compagnon",
        description = "Met à jour les informations d'un compagnon existant"
    )
    @ApiResponse(responseCode = "200", description = "Compagnon mis à jour")
    @ApiResponse(responseCode = "404", description = "Compagnon non trouvé")
    public ResponseEntity<CompanionResponse> updateCompanion(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateCompanionRequest request) {
        
        log.info("PUT /api/v1/companions/{} - userId: {}", id, userId);
        CompanionResponse response = companionService.updateCompanion(id, userId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Supprime un compagnon.
     * 
     * DELETE /api/v1/companions/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un compagnon",
        description = "Supprime définitivement un compagnon"
    )
    @ApiResponse(responseCode = "204", description = "Compagnon supprimé")
    @ApiResponse(responseCode = "404", description = "Compagnon non trouvé")
    public ResponseEntity<Void> deleteCompanion(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        
        log.info("DELETE /api/v1/companions/{} - userId: {}", id, userId);
        companionService.deleteCompanion(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Récupère tous les compagnons de l'utilisateur.
     * 
     * GET /api/v1/companions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Lister mes compagnons",
        description = "Récupère tous les compagnons de l'utilisateur authentifié"
    )
    @ApiResponse(responseCode = "200", description = "Liste des compagnons")
    public ResponseEntity<List<CompanionResponse>> getUserCompanions(
            @PathVariable String userId,
            @AuthenticationPrincipal String authenticatedUserId) {
        
        // Sécurité: un utilisateur ne peut voir que ses propres compagnons
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        log.info("GET /api/v1/companions/user/{}", userId);
        List<CompanionResponse> companions = companionService.getUserCompanions(userId);
        return ResponseEntity.ok(companions);
    }
    
    /**
     * Récupère les compagnons publics (galerie).
     * 
     * GET /api/v1/companions/public
     */
    @GetMapping("/public")
    @Operation(
        summary = "Galerie publique",
        description = "Récupère les compagnons publics triés par popularité"
    )
    @ApiResponse(responseCode = "200", description = "Liste des compagnons publics")
    public ResponseEntity<PublicCompanionsResponse> getPublicCompanions(
            @PageableDefault(size = 20, sort = "likeCount", direction = Sort.Direction.DESC)
            Pageable pageable) {
        
        log.info("GET /api/v1/companions/public - page: {}", pageable.getPageNumber());
        PublicCompanionsResponse response = companionService.getPublicCompanions(pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Recherche avancée de compagnons.
     * 
     * GET /api/v1/companions/search
     */
    @GetMapping("/search")
    @Operation(
        summary = "Recherche avancée",
        description = "Recherche de compagnons avec filtres multiples"
    )
    @ApiResponse(responseCode = "200", description = "Résultats de recherche")
    public ResponseEntity<Page<CompanionResponse>> searchCompanions(
            @RequestParam Map<String, Object> filters,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("GET /api/v1/companions/search - filters: {}", filters);
        Page<CompanionResponse> results = companionService.advancedSearch(filters, pageable);
        return ResponseEntity.ok(results);
    }
}

// ============================================================================
// FICHIER: CompanionEvolutionController.java
// Description: Contrôleur pour les opérations d'évolution génétique
// ============================================================================

package com.nexusai.companion.controller;

import com.nexusai.companion.dto.*;
import com.nexusai.companion.service.EvolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour les opérations d'évolution génétique.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/companions")
@Tag(name = "Companion Evolution", description = "API d'évolution génétique")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
@Slf4j
public class CompanionEvolutionController {
    
    private final EvolutionService evolutionService;
    
    /**
     * Fait évoluer un compagnon manuellement.
     * 
     * POST /api/v1/companions/{id}/evolve
     */
    @PostMapping("/{id}/evolve")
    @Operation(
        summary = "Faire évoluer",
        description = "Déclenche l'évolution génétique d'un compagnon"
    )
    public ResponseEntity<CompanionResponse> evolveCompanion(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody(required = false) EvolveCompanionRequest request) {
        
        log.info("POST /api/v1/companions/{}/evolve - userId: {}", id, userId);
        
        if (request == null) {
            request = EvolveCompanionRequest.builder().intensity(5).build();
        }
        
        CompanionResponse response = evolutionService.evolveCompanion(id, userId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gèle des traits génétiques.
     * 
     * POST /api/v1/companions/{id}/freeze-traits
     */
    @PostMapping("/{id}/freeze-traits")
    @Operation(
        summary = "Geler des traits",
        description = "Empêche l'évolution de certains traits génétiques"
    )
    public ResponseEntity<CompanionResponse> freezeTraits(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody FreezeTraitsRequest request) {
        
        log.info("POST /api/v1/companions/{}/freeze-traits - userId: {}", id, userId);
        CompanionResponse response = evolutionService.freezeTraits(id, userId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Fusionne deux compagnons.
     * 
     * POST /api/v1/companions/merge
     */
    @PostMapping("/merge")
    @Operation(
        summary = "Fusionner deux compagnons",
        description = "Crée un nouveau compagnon en fusionnant deux existants"
    )
    public ResponseEntity<MergeResult> mergeCompanions(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody MergeCompanionsRequest request) {
        
        log.info("POST /api/v1/companions/merge - userId: {}", userId);
        MergeResult result = evolutionService.mergeCompanions(userId, request);
        return ResponseEntity.ok(result);
    }
}

// ============================================================================
// FICHIER: CompanionTemplateController.java
// Description: Contrôleur pour les templates de compagnons
// ============================================================================

package com.nexusai.companion.controller;

import com.nexusai.companion.dto.CompanionResponse;
import com.nexusai.companion.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour les templates de compagnons prédéfinis.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/companions/templates")
@Tag(name = "Companion Templates", description = "API des templates prédéfinis")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
@Slf4j
public class CompanionTemplateController {
    
    private final TemplateService templateService;
    
    /**
     * Liste tous les templates disponibles.
     * 
     * GET /api/v1/companions/templates
     */
    @GetMapping
    @Operation(
        summary = "Lister les templates",
        description = "Récupère tous les templates de compagnons disponibles"
    )
    public ResponseEntity<Page<?>> getTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 50) Pageable pageable) {
        
        log.info("GET /api/v1/companions/templates");
        
        if (category != null) {
            return ResponseEntity.ok(
                templateService.getTemplatesByCategory(category, pageable)
            );
        } else if (tag != null) {
            return ResponseEntity.ok(
                templateService.getTemplatesByTag(tag, pageable)
            );
        } else {
            return ResponseEntity.ok(
                templateService.getAllTemplates(pageable)
            );
        }
    }
    
    /**
     * Crée un compagnon depuis un template.
     * 
     * POST /api/v1/companions/from-template/{templateId}
     */
    @PostMapping("/from-template/{templateId}")
    @Operation(
        summary = "Créer depuis template",
        description = "Crée un compagnon en utilisant un template prédéfini"
    )
    public ResponseEntity<CompanionResponse> createFromTemplate(
            @PathVariable String templateId,
            @AuthenticationPrincipal String userId,
            @RequestParam String name) {
        
        log.info("POST /api/v1/companions/from-template/{} - userId: {}", 
                 templateId, userId);
        
        CompanionResponse response = templateService
            .createCompanionFromTemplate(templateId, userId, name);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

// ============================================================================
// FICHIER: CompanionLikeController.java
// Description: Contrôleur pour les likes de compagnons publics
// ============================================================================

package com.nexusai.companion.controller;

import com.nexusai.companion.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur pour les interactions de like sur les compagnons publics.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/companions")
@Tag(name = "Companion Likes", description = "API de likes des compagnons")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
@Slf4j
public class CompanionLikeController {
    
    private final LikeService likeService;
    
    /**
     * Like un compagnon public.
     * 
     * POST /api/v1/companions/{id}/like
     */
    @PostMapping("/{id}/like")
    @Operation(
        summary = "Liker un compagnon",
        description = "Ajoute un like à un compagnon public"
    )
    public ResponseEntity<Void> likeCompanion(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        
        log.info("POST /api/v1/companions/{}/like - userId: {}", id, userId);
        likeService.likeCompanion(id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Unlike un compagnon public.
     * 
     * DELETE /api/v1/companions/{id}/like
     */
    @DeleteMapping("/{id}/like")
    @Operation(
        summary = "Retirer le like",
        description = "Retire le like d'un compagnon public"
    )
    public ResponseEntity<Void> unlikeCompanion(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        
        log.info("DELETE /api/v1/companions/{}/like - userId: {}", id, userId);
        likeService.unlikeCompanion(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Vérifie si l'utilisateur a liké un compagnon.
     * 
     * GET /api/v1/companions/{id}/like/status
     */
    @GetMapping("/{id}/like/status")
    @Operation(
        summary = "Statut du like",
        description = "Vérifie si l'utilisateur a liké ce compagnon"
    )
    public ResponseEntity<Boolean> getLikeStatus(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        
        boolean hasLiked = likeService.hasUserLiked(id, userId);
        return ResponseEntity.ok(hasLiked);
    }
}