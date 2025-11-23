package com.nexusai.analytics.repository;

import com.nexusai.analytics.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {

    Optional<UserActivity> findByUserIdAndSummaryDate(UUID userId, LocalDate date);

    List<UserActivity> findByUserIdAndSummaryDateBetween(UUID userId, LocalDate from, LocalDate to);

    @Query("SELECT SUM(ua.messagesSent) FROM UserActivity ua WHERE ua.userId = :userId " +
           "AND ua.summaryDate BETWEEN :from AND :to")
    Long sumMessagesSent(@Param("userId") UUID userId, @Param("from") LocalDate from,
                         @Param("to") LocalDate to);

    @Query("SELECT SUM(ua.tokensUsed) FROM UserActivity ua WHERE ua.userId = :userId " +
           "AND ua.summaryDate BETWEEN :from AND :to")
    Long sumTokensUsed(@Param("userId") UUID userId, @Param("from") LocalDate from,
                       @Param("to") LocalDate to);
}
