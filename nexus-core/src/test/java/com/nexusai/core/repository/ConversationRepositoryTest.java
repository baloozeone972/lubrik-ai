package com.nexusai.core.repository;

import com.nexusai.core.entity.Conversation;
import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.enums.ConversationStatus;
import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.core.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ConversationRepository Tests")
class ConversationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ConversationRepository conversationRepository;

    private UUID userId;
    private UUID companionId;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companionId = UUID.randomUUID();

        testConversation = Conversation.builder()
                .userId(userId)
                .companionId(companionId)
                .title("Test Conversation")
                .status(ConversationStatus.ACTIVE)
                .messageCount(0)
                .totalTokens(0L)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("FindByUserIdAndStatusOrderByLastActivityAtDesc Tests")
    class FindByUserIdAndStatusTests {

        @Test
        @DisplayName("Should return active conversations ordered by last activity")
        void shouldReturnActiveConversationsOrdered() {
            Conversation conv1 = Conversation.builder()
                    .userId(userId)
                    .companionId(companionId)
                    .title("Old Conversation")
                    .status(ConversationStatus.ACTIVE)
                    .lastActivityAt(LocalDateTime.now().minusDays(1))
                    .build();

            Conversation conv2 = Conversation.builder()
                    .userId(userId)
                    .companionId(companionId)
                    .title("New Conversation")
                    .status(ConversationStatus.ACTIVE)
                    .lastActivityAt(LocalDateTime.now())
                    .build();

            entityManager.persist(conv1);
            entityManager.persist(conv2);
            entityManager.flush();

            Page<Conversation> result = conversationRepository
                    .findByUserIdAndStatusOrderByLastActivityAtDesc(
                            userId, ConversationStatus.ACTIVE, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("New Conversation");
            assertThat(result.getContent().get(1).getTitle()).isEqualTo("Old Conversation");
        }

        @Test
        @DisplayName("Should not return archived conversations")
        void shouldNotReturnArchivedConversations() {
            Conversation activeConv = Conversation.builder()
                    .userId(userId)
                    .companionId(companionId)
                    .title("Active")
                    .status(ConversationStatus.ACTIVE)
                    .lastActivityAt(LocalDateTime.now())
                    .build();

            Conversation archivedConv = Conversation.builder()
                    .userId(userId)
                    .companionId(companionId)
                    .title("Archived")
                    .status(ConversationStatus.ARCHIVED)
                    .lastActivityAt(LocalDateTime.now())
                    .build();

            entityManager.persist(activeConv);
            entityManager.persist(archivedConv);
            entityManager.flush();

            Page<Conversation> result = conversationRepository
                    .findByUserIdAndStatusOrderByLastActivityAtDesc(
                            userId, ConversationStatus.ACTIVE, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Active");
        }

        @Test
        @DisplayName("Should return empty page for user with no conversations")
        void shouldReturnEmptyPageForUserWithNoConversations() {
            UUID otherUserId = UUID.randomUUID();

            Page<Conversation> result = conversationRepository
                    .findByUserIdAndStatusOrderByLastActivityAtDesc(
                            otherUserId, ConversationStatus.ACTIVE, PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should respect pagination")
        void shouldRespectPagination() {
            for (int i = 0; i < 15; i++) {
                Conversation conv = Conversation.builder()
                        .userId(userId)
                        .companionId(companionId)
                        .title("Conversation " + i)
                        .status(ConversationStatus.ACTIVE)
                        .lastActivityAt(LocalDateTime.now().minusMinutes(i))
                        .build();
                entityManager.persist(conv);
            }
            entityManager.flush();

            Page<Conversation> firstPage = conversationRepository
                    .findByUserIdAndStatusOrderByLastActivityAtDesc(
                            userId, ConversationStatus.ACTIVE, PageRequest.of(0, 10));

            Page<Conversation> secondPage = conversationRepository
                    .findByUserIdAndStatusOrderByLastActivityAtDesc(
                            userId, ConversationStatus.ACTIVE, PageRequest.of(1, 10));

            assertThat(firstPage.getContent()).hasSize(10);
            assertThat(secondPage.getContent()).hasSize(5);
            assertThat(firstPage.getTotalElements()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("FindByIdAndUserId Tests")
    class FindByIdAndUserIdTests {

        @Test
        @DisplayName("Should find conversation by ID and user ID")
        void shouldFindConversationByIdAndUserId() {
            Conversation saved = entityManager.persist(testConversation);
            entityManager.flush();

            Optional<Conversation> found = conversationRepository.findByIdAndUserId(saved.getId(), userId);

            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Test Conversation");
        }

        @Test
        @DisplayName("Should not find conversation for different user")
        void shouldNotFindConversationForDifferentUser() {
            Conversation saved = entityManager.persist(testConversation);
            entityManager.flush();

            UUID otherUserId = UUID.randomUUID();
            Optional<Conversation> found = conversationRepository.findByIdAndUserId(saved.getId(), otherUserId);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for non-existent conversation")
        void shouldReturnEmptyForNonExistent() {
            UUID randomId = UUID.randomUUID();

            Optional<Conversation> found = conversationRepository.findByIdAndUserId(randomId, userId);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("FindByUserIdAndCompanionIdAndStatus Tests")
    class FindByUserIdAndCompanionIdAndStatusTests {

        @Test
        @DisplayName("Should find conversations by user, companion and status")
        void shouldFindConversationsByUserCompanionAndStatus() {
            entityManager.persist(testConversation);
            entityManager.flush();

            List<Conversation> result = conversationRepository
                    .findByUserIdAndCompanionIdAndStatus(userId, companionId, ConversationStatus.ACTIVE);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty for different companion")
        void shouldReturnEmptyForDifferentCompanion() {
            entityManager.persist(testConversation);
            entityManager.flush();

            UUID otherCompanionId = UUID.randomUUID();
            List<Conversation> result = conversationRepository
                    .findByUserIdAndCompanionIdAndStatus(userId, otherCompanionId, ConversationStatus.ACTIVE);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("IncrementMessageCount Tests")
    class IncrementMessageCountTests {

        @Test
        @DisplayName("Should increment message count and update activity time")
        void shouldIncrementMessageCountAndUpdateActivityTime() {
            Conversation saved = entityManager.persist(testConversation);
            entityManager.flush();

            LocalDateTime newActivityTime = LocalDateTime.now();
            conversationRepository.incrementMessageCount(saved.getId(), newActivityTime);
            entityManager.clear();

            Conversation updated = entityManager.find(Conversation.class, saved.getId());
            assertThat(updated.getMessageCount()).isEqualTo(1);
            assertThat(updated.getLastActivityAt()).isEqualTo(newActivityTime);
        }

        @Test
        @DisplayName("Should increment count multiple times")
        void shouldIncrementCountMultipleTimes() {
            Conversation saved = entityManager.persist(testConversation);
            entityManager.flush();

            conversationRepository.incrementMessageCount(saved.getId(), LocalDateTime.now());
            conversationRepository.incrementMessageCount(saved.getId(), LocalDateTime.now());
            conversationRepository.incrementMessageCount(saved.getId(), LocalDateTime.now());
            entityManager.clear();

            Conversation updated = entityManager.find(Conversation.class, saved.getId());
            assertThat(updated.getMessageCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("CountActiveByUserId Tests")
    class CountActiveByUserIdTests {

        @Test
        @DisplayName("Should count active conversations for user")
        void shouldCountActiveConversationsForUser() {
            for (int i = 0; i < 5; i++) {
                Conversation conv = Conversation.builder()
                        .userId(userId)
                        .companionId(companionId)
                        .title("Conversation " + i)
                        .status(ConversationStatus.ACTIVE)
                        .build();
                entityManager.persist(conv);
            }
            entityManager.flush();

            long count = conversationRepository.countActiveByUserId(userId);

            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("Should not count archived conversations")
        void shouldNotCountArchivedConversations() {
            Conversation active = Conversation.builder()
                    .userId(userId)
                    .companionId(companionId)
                    .title("Active")
                    .status(ConversationStatus.ACTIVE)
                    .build();

            Conversation archived = Conversation.builder()
                    .userId(userId)
                    .companionId(companionId)
                    .title("Archived")
                    .status(ConversationStatus.ARCHIVED)
                    .build();

            entityManager.persist(active);
            entityManager.persist(archived);
            entityManager.flush();

            long count = conversationRepository.countActiveByUserId(userId);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return zero for user with no conversations")
        void shouldReturnZeroForUserWithNoConversations() {
            UUID otherUserId = UUID.randomUUID();

            long count = conversationRepository.countActiveByUserId(otherUserId);

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CRUDOperationsTests {

        @Test
        @DisplayName("Should save and retrieve conversation")
        void shouldSaveAndRetrieveConversation() {
            Conversation saved = conversationRepository.save(testConversation);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();

            Optional<Conversation> found = conversationRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo("Test Conversation");
        }

        @Test
        @DisplayName("Should delete conversation")
        void shouldDeleteConversation() {
            Conversation saved = entityManager.persist(testConversation);
            entityManager.flush();

            conversationRepository.deleteById(saved.getId());
            entityManager.flush();

            Optional<Conversation> found = conversationRepository.findById(saved.getId());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should update conversation status")
        void shouldUpdateConversationStatus() {
            Conversation saved = entityManager.persist(testConversation);
            entityManager.flush();

            saved.setStatus(ConversationStatus.ARCHIVED);
            conversationRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            Conversation found = entityManager.find(Conversation.class, saved.getId());
            assertThat(found.getStatus()).isEqualTo(ConversationStatus.ARCHIVED);
        }
    }
}
