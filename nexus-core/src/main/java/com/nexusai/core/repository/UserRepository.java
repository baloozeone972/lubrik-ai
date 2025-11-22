package com.nexusai.core.repository;

import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailAndAccountStatus(String email, AccountStatus status);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("loginTime") LocalDateTime loginTime);

    @Modifying
    @Query("UPDATE User u SET u.tokensRemaining = u.tokensRemaining - :amount WHERE u.id = :userId AND u.tokensRemaining >= :amount")
    int consumeTokens(@Param("userId") UUID userId, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE User u SET u.tokensRemaining = u.tokensRemaining + :amount WHERE u.id = :userId")
    void addTokens(@Param("userId") UUID userId, @Param("amount") int amount);
}
