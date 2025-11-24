package com.nexusai.core.repository;

import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.core.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .username("testuser")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.USER)
                .subscriptionType(SubscriptionType.FREE)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .tokensRemaining(100)
                .build();
    }

    @Nested
    @DisplayName("FindByEmail Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            entityManager.persist(testUser);
            entityManager.flush();

            Optional<User> found = userRepository.findByEmail("test@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
            assertThat(found.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should return empty for non-existent email")
        void shouldReturnEmptyForNonExistentEmail() {
            Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should be case-sensitive for email")
        void shouldBeCaseSensitiveForEmail() {
            entityManager.persist(testUser);
            entityManager.flush();

            Optional<User> found = userRepository.findByEmail("TEST@example.com");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("FindByUsername Tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should find user by username")
        void shouldFindUserByUsername() {
            entityManager.persist(testUser);
            entityManager.flush();

            Optional<User> found = userRepository.findByUsername("testuser");

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should return empty for non-existent username")
        void shouldReturnEmptyForNonExistentUsername() {
            Optional<User> found = userRepository.findByUsername("nonexistent");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("ExistsByEmail Tests")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            entityManager.persist(testUser);
            entityManager.flush();

            boolean exists = userRepository.existsByEmail("test@example.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailNotExists() {
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("ExistsByUsername Tests")
    class ExistsByUsernameTests {

        @Test
        @DisplayName("Should return true when username exists")
        void shouldReturnTrueWhenUsernameExists() {
            entityManager.persist(testUser);
            entityManager.flush();

            boolean exists = userRepository.existsByUsername("testuser");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when username does not exist")
        void shouldReturnFalseWhenUsernameNotExists() {
            boolean exists = userRepository.existsByUsername("nonexistent");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("FindByEmailAndAccountStatus Tests")
    class FindByEmailAndAccountStatusTests {

        @Test
        @DisplayName("Should find active user by email")
        void shouldFindActiveUserByEmail() {
            entityManager.persist(testUser);
            entityManager.flush();

            Optional<User> found = userRepository.findByEmailAndAccountStatus(
                    "test@example.com", AccountStatus.ACTIVE);

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("Should not find suspended user when searching for active")
        void shouldNotFindSuspendedUserWhenSearchingActive() {
            testUser.setAccountStatus(AccountStatus.SUSPENDED);
            entityManager.persist(testUser);
            entityManager.flush();

            Optional<User> found = userRepository.findByEmailAndAccountStatus(
                    "test@example.com", AccountStatus.ACTIVE);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("UpdateLastLogin Tests")
    class UpdateLastLoginTests {

        @Test
        @DisplayName("Should update last login timestamp")
        void shouldUpdateLastLoginTimestamp() {
            User saved = entityManager.persist(testUser);
            entityManager.flush();

            LocalDateTime loginTime = LocalDateTime.now();
            userRepository.updateLastLogin(saved.getId(), loginTime);
            entityManager.clear();

            User updated = entityManager.find(User.class, saved.getId());
            assertThat(updated.getLastLoginAt()).isEqualTo(loginTime);
        }
    }

    @Nested
    @DisplayName("Token Management Tests")
    class TokenManagementTests {

        @Test
        @DisplayName("Should consume tokens when sufficient balance")
        void shouldConsumeTokensWhenSufficientBalance() {
            testUser.setTokensRemaining(100);
            User saved = entityManager.persist(testUser);
            entityManager.flush();

            int updated = userRepository.consumeTokens(saved.getId(), 50);
            entityManager.clear();

            assertThat(updated).isEqualTo(1);
            User user = entityManager.find(User.class, saved.getId());
            assertThat(user.getTokensRemaining()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should not consume tokens when insufficient balance")
        void shouldNotConsumeTokensWhenInsufficientBalance() {
            testUser.setTokensRemaining(30);
            User saved = entityManager.persist(testUser);
            entityManager.flush();

            int updated = userRepository.consumeTokens(saved.getId(), 50);
            entityManager.clear();

            assertThat(updated).isEqualTo(0);
            User user = entityManager.find(User.class, saved.getId());
            assertThat(user.getTokensRemaining()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should add tokens to user")
        void shouldAddTokensToUser() {
            testUser.setTokensRemaining(100);
            User saved = entityManager.persist(testUser);
            entityManager.flush();

            userRepository.addTokens(saved.getId(), 50);
            entityManager.clear();

            User user = entityManager.find(User.class, saved.getId());
            assertThat(user.getTokensRemaining()).isEqualTo(150);
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CRUDOperationsTests {

        @Test
        @DisplayName("Should save and retrieve user")
        void shouldSaveAndRetrieveUser() {
            User saved = userRepository.save(testUser);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();

            Optional<User> found = userRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should delete user")
        void shouldDeleteUser() {
            User saved = entityManager.persist(testUser);
            entityManager.flush();

            userRepository.deleteById(saved.getId());
            entityManager.flush();

            Optional<User> found = userRepository.findById(saved.getId());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should update user")
        void shouldUpdateUser() {
            User saved = entityManager.persist(testUser);
            entityManager.flush();

            saved.setDisplayName("Updated Name");
            userRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            User found = entityManager.find(User.class, saved.getId());
            assertThat(found.getDisplayName()).isEqualTo("Updated Name");
        }
    }
}
