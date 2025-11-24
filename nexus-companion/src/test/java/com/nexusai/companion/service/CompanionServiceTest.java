package com.nexusai.companion.service;

import com.nexusai.commons.exception.BusinessException;
import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.companion.dto.CompanionCreateRequest;
import com.nexusai.companion.dto.CompanionResponse;
import com.nexusai.companion.dto.CompanionUpdateRequest;
import com.nexusai.core.entity.Companion;
import com.nexusai.core.entity.User;
import com.nexusai.core.enums.CompanionStatus;
import com.nexusai.core.enums.CompanionStyle;
import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.core.repository.CompanionRepository;
import com.nexusai.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanionService Tests")
class CompanionServiceTest {

    @Mock
    private CompanionRepository companionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanionService companionService;

    private UUID userId;
    private UUID companionId;
    private User testUser;
    private Companion testCompanion;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companionId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .subscriptionType(SubscriptionType.STANDARD)
                .build();

        testCompanion = Companion.builder()
                .id(companionId)
                .userId(userId)
                .name("Test Companion")
                .description("A test companion")
                .style(CompanionStyle.REALISTIC)
                .status(CompanionStatus.ACTIVE)
                .isPublic(false)
                .totalMessages(100L)
                .likesCount(10)
                .build();
    }

    @Nested
    @DisplayName("CreateCompanion Tests")
    class CreateCompanionTests {

        @Test
        @DisplayName("Should create companion successfully")
        void shouldCreateCompanionSuccessfully() {
            CompanionCreateRequest request = CompanionCreateRequest.builder()
                    .name("New Companion")
                    .description("A friendly companion")
                    .style(CompanionStyle.ANIME)
                    .systemPrompt("You are a friendly assistant")
                    .isPublic(true)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(companionRepository.countActiveByUserId(userId)).thenReturn(1L);
            when(companionRepository.save(any(Companion.class))).thenAnswer(invocation -> {
                Companion saved = invocation.getArgument(0);
                saved.setId(companionId);
                return saved;
            });

            CompanionResponse result = companionService.createCompanion(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Companion");
            assertThat(result.getDescription()).isEqualTo("A friendly companion");
            assertThat(result.getIsPublic()).isTrue();

            ArgumentCaptor<Companion> captor = ArgumentCaptor.forClass(Companion.class);
            verify(companionRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(userId);
            assertThat(captor.getValue().getSystemPrompt()).isEqualTo("You are a friendly assistant");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            CompanionCreateRequest request = CompanionCreateRequest.builder()
                    .name("New Companion")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> companionService.createCompanion(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(companionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when companion limit reached")
        void shouldThrowExceptionWhenLimitReached() {
            CompanionCreateRequest request = CompanionCreateRequest.builder()
                    .name("New Companion")
                    .build();

            // STANDARD allows 3 companions
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(companionRepository.countActiveByUserId(userId)).thenReturn(3L);

            assertThatThrownBy(() -> companionService.createCompanion(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("LIMIT_REACHED");

            verify(companionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow unlimited companions for VIP_PLUS users")
        void shouldAllowUnlimitedCompanionsForVipPlus() {
            User vipUser = User.builder()
                    .id(userId)
                    .subscriptionType(SubscriptionType.VIP_PLUS)
                    .build();

            CompanionCreateRequest request = CompanionCreateRequest.builder()
                    .name("New Companion")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(vipUser));
            when(companionRepository.countActiveByUserId(userId)).thenReturn(100L);
            when(companionRepository.save(any(Companion.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CompanionResponse result = companionService.createCompanion(userId, request);

            assertThat(result).isNotNull();
            verify(companionRepository).save(any(Companion.class));
        }

        @Test
        @DisplayName("Should default isPublic to false when null")
        void shouldDefaultIsPublicToFalse() {
            CompanionCreateRequest request = CompanionCreateRequest.builder()
                    .name("Private Companion")
                    .isPublic(null)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(companionRepository.countActiveByUserId(userId)).thenReturn(0L);
            when(companionRepository.save(any(Companion.class))).thenAnswer(invocation -> invocation.getArgument(0));

            companionService.createCompanion(userId, request);

            ArgumentCaptor<Companion> captor = ArgumentCaptor.forClass(Companion.class);
            verify(companionRepository).save(captor.capture());
            assertThat(captor.getValue().getIsPublic()).isFalse();
        }
    }

    @Nested
    @DisplayName("GetCompanion Tests")
    class GetCompanionTests {

        @Test
        @DisplayName("Should return companion for owner")
        void shouldReturnCompanionForOwner() {
            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            CompanionResponse result = companionService.getCompanion(companionId, userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(companionId);
            assertThat(result.getName()).isEqualTo("Test Companion");
        }

        @Test
        @DisplayName("Should return public companion for non-owner")
        void shouldReturnPublicCompanionForNonOwner() {
            testCompanion.setIsPublic(true);
            UUID otherUserId = UUID.randomUUID();

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            CompanionResponse result = companionService.getCompanion(companionId, otherUserId);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Companion");
        }

        @Test
        @DisplayName("Should throw exception for private companion accessed by non-owner")
        void shouldThrowExceptionForPrivateCompanionNonOwner() {
            testCompanion.setIsPublic(false);
            UUID otherUserId = UUID.randomUUID();

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            assertThatThrownBy(() -> companionService.getCompanion(companionId, otherUserId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception for deleted companion")
        void shouldThrowExceptionForDeletedCompanion() {
            testCompanion.setStatus(CompanionStatus.DELETED);

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            assertThatThrownBy(() -> companionService.getCompanion(companionId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception for non-existent companion")
        void shouldThrowExceptionForNonExistentCompanion() {
            when(companionRepository.findById(companionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> companionService.getCompanion(companionId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GetUserCompanions Tests")
    class GetUserCompanionsTests {

        @Test
        @DisplayName("Should return all user companions except deleted")
        void shouldReturnAllUserCompanionsExceptDeleted() {
            Companion companion2 = Companion.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .name("Companion 2")
                    .status(CompanionStatus.ACTIVE)
                    .build();

            when(companionRepository.findByUserIdAndStatusNot(userId, CompanionStatus.DELETED))
                    .thenReturn(List.of(testCompanion, companion2));

            List<CompanionResponse> result = companionService.getUserCompanions(userId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Test Companion");
        }

        @Test
        @DisplayName("Should return empty list when user has no companions")
        void shouldReturnEmptyListWhenNoCompanions() {
            when(companionRepository.findByUserIdAndStatusNot(userId, CompanionStatus.DELETED))
                    .thenReturn(Collections.emptyList());

            List<CompanionResponse> result = companionService.getUserCompanions(userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("UpdateCompanion Tests")
    class UpdateCompanionTests {

        @Test
        @DisplayName("Should update companion fields")
        void shouldUpdateCompanionFields() {
            CompanionUpdateRequest request = CompanionUpdateRequest.builder()
                    .name("Updated Name")
                    .description("Updated description")
                    .style(CompanionStyle.ANIME)
                    .isPublic(true)
                    .build();

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));
            when(companionRepository.save(any(Companion.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CompanionResponse result = companionService.updateCompanion(companionId, userId, request);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getIsPublic()).isTrue();

            ArgumentCaptor<Companion> captor = ArgumentCaptor.forClass(Companion.class);
            verify(companionRepository).save(captor.capture());
            assertThat(captor.getValue().getStyle()).isEqualTo(CompanionStyle.ANIME);
        }

        @Test
        @DisplayName("Should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            CompanionUpdateRequest request = CompanionUpdateRequest.builder()
                    .name("Only Name Updated")
                    .build();

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));
            when(companionRepository.save(any(Companion.class))).thenAnswer(invocation -> invocation.getArgument(0));

            companionService.updateCompanion(companionId, userId, request);

            ArgumentCaptor<Companion> captor = ArgumentCaptor.forClass(Companion.class);
            verify(companionRepository).save(captor.capture());
            Companion saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Only Name Updated");
            assertThat(saved.getDescription()).isEqualTo("A test companion"); // unchanged
        }

        @Test
        @DisplayName("Should throw exception when updating non-owned companion")
        void shouldThrowExceptionWhenUpdatingNonOwnedCompanion() {
            UUID otherUserId = UUID.randomUUID();
            CompanionUpdateRequest request = CompanionUpdateRequest.builder()
                    .name("Hacked Name")
                    .build();

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            assertThatThrownBy(() -> companionService.updateCompanion(companionId, otherUserId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(companionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("DeleteCompanion Tests")
    class DeleteCompanionTests {

        @Test
        @DisplayName("Should soft delete companion")
        void shouldSoftDeleteCompanion() {
            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));
            when(companionRepository.save(any(Companion.class))).thenReturn(testCompanion);

            companionService.deleteCompanion(companionId, userId);

            ArgumentCaptor<Companion> captor = ArgumentCaptor.forClass(Companion.class);
            verify(companionRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CompanionStatus.DELETED);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-owned companion")
        void shouldThrowExceptionWhenDeletingNonOwnedCompanion() {
            UUID otherUserId = UUID.randomUUID();

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            assertThatThrownBy(() -> companionService.deleteCompanion(companionId, otherUserId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(companionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting already deleted companion")
        void shouldThrowExceptionWhenDeletingAlreadyDeleted() {
            testCompanion.setStatus(CompanionStatus.DELETED);

            when(companionRepository.findById(companionId)).thenReturn(Optional.of(testCompanion));

            assertThatThrownBy(() -> companionService.deleteCompanion(companionId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(companionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("GetPublicCompanions Tests")
    class GetPublicCompanionsTests {

        @Test
        @DisplayName("Should return paginated public companions")
        void shouldReturnPaginatedPublicCompanions() {
            Pageable pageable = PageRequest.of(0, 10);
            testCompanion.setIsPublic(true);
            Page<Companion> companionPage = new PageImpl<>(List.of(testCompanion), pageable, 1);

            when(companionRepository.findPublicCompanions(pageable)).thenReturn(companionPage);

            Page<CompanionResponse> result = companionService.getPublicCompanions(pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Test Companion");
        }

        @Test
        @DisplayName("Should return empty page when no public companions")
        void shouldReturnEmptyPageWhenNoPublicCompanions() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Companion> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(companionRepository.findPublicCompanions(pageable)).thenReturn(emptyPage);

            Page<CompanionResponse> result = companionService.getPublicCompanions(pageable);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("SearchPublicCompanions Tests")
    class SearchPublicCompanionsTests {

        @Test
        @DisplayName("Should search public companions by query")
        void shouldSearchPublicCompanionsByQuery() {
            Pageable pageable = PageRequest.of(0, 10);
            testCompanion.setIsPublic(true);
            Page<Companion> companionPage = new PageImpl<>(List.of(testCompanion), pageable, 1);

            when(companionRepository.searchPublicCompanions("test", pageable)).thenReturn(companionPage);

            Page<CompanionResponse> result = companionService.searchPublicCompanions("test", pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).getName()).contains("Test");
        }

        @Test
        @DisplayName("Should return empty when no matches found")
        void shouldReturnEmptyWhenNoMatchesFound() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Companion> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(companionRepository.searchPublicCompanions("nonexistent", pageable)).thenReturn(emptyPage);

            Page<CompanionResponse> result = companionService.searchPublicCompanions("nonexistent", pageable);

            assertThat(result).isEmpty();
        }
    }
}
