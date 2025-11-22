package com.nexusai.core.repository;

import com.nexusai.core.entity.Companion;
import com.nexusai.core.enums.CompanionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompanionRepository extends JpaRepository<Companion, UUID> {

    List<Companion> findByUserIdAndStatusNot(UUID userId, CompanionStatus status);

    Page<Companion> findByUserIdAndStatus(UUID userId, CompanionStatus status, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Companion c WHERE c.userId = :userId AND c.status != 'DELETED'")
    long countActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Companion c WHERE c.isPublic = true AND c.status = 'ACTIVE' ORDER BY c.likesCount DESC")
    Page<Companion> findPublicCompanions(Pageable pageable);

    @Query("SELECT c FROM Companion c WHERE c.isPublic = true AND c.status = 'ACTIVE' " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Companion> searchPublicCompanions(@Param("query") String query, Pageable pageable);
}
