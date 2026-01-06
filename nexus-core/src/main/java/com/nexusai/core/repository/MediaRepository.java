package com.nexusai.core.repository;

import com.nexusai.core.entity.Media;
import com.nexusai.core.enums.MediaCategory;
import com.nexusai.core.enums.MediaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité Media.
 */
@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    /**
     * Trouve un média par son storage key.
     */
    Optional<Media> findByStorageKey(String storageKey);

    /**
     * Trouve tous les médias d'un utilisateur.
     */
    Page<Media> findByUserIdAndStatus(UUID userId, MediaStatus status, Pageable pageable);

    /**
     * Trouve tous les médias d'un utilisateur par catégorie.
     */
    Page<Media> findByUserIdAndCategoryAndStatus(
            UUID userId, 
            MediaCategory category, 
            MediaStatus status, 
            Pageable pageable
    );

    /**
     * Compte les médias d'un utilisateur.
     */
    long countByUserIdAndStatus(UUID userId, MediaStatus status);

    /**
     * Trouve les médias temporaires expirés.
     */
    @Query("SELECT m FROM Media m WHERE m.category = 'TEMP' AND m.createdAt < :expirationDate")
    List<Media> findExpiredTempMedia(@Param("expirationDate") LocalDateTime expirationDate);

    /**
     * Calcule l'espace de stockage utilisé par un utilisateur.
     */
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM Media m WHERE m.userId = :userId AND m.status = 'ACTIVE'")
    Long calculateUserStorageUsed(@Param("userId") UUID userId);
}
