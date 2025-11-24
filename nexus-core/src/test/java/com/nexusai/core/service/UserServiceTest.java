package com.nexusai.core.service;

import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.core.dto.UserDTO;
import com.nexusai.core.dto.UserUpdateRequest;
import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.core.enums.UserRole;
import com.nexusai.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .passwordHash("hashedpassword")
                .role(UserRole.USER)
                .subscriptionType(SubscriptionType.FREE)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            UserDTO result = userService.getUserById(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("getUserByEmail Tests")
    class GetUserByEmailTests {

        @Test
        @DisplayName("Should return user when email exists")
        void shouldReturnUserWhenEmailExists() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            UserDTO result = userService.getUserByEmail("test@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            verify(userRepository).findByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository).findByEmail("nonexistent@example.com");
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            updateRequest.setDisplayName("Updated Name");
            updateRequest.setBio("New bio");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserDTO result = userService.updateUser(userId, updateRequest);

            assertThat(result).isNotNull();
            verify(userRepository).findById(userId);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent user")
        void shouldThrowExceptionWhenUpdatingNonExistentUser() {
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(userId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository).findById(userId);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllUsers Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated users")
        void shouldReturnPaginatedUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<UserDTO> result = userService.getAllUsers(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(userRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should return empty page when no users")
        void shouldReturnEmptyPageWhenNoUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<UserDTO> result = userService.getAllUsers(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("existsByEmail Tests")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            boolean result = userService.existsByEmail("test@example.com");

            assertThat(result).isTrue();
            verify(userRepository).existsByEmail("test@example.com");
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            boolean result = userService.existsByEmail("new@example.com");

            assertThat(result).isFalse();
            verify(userRepository).existsByEmail("new@example.com");
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user when exists")
        void shouldDeleteUserWhenExists() {
            when(userRepository.existsById(userId)).thenReturn(true);
            doNothing().when(userRepository).deleteById(userId);

            assertThatCode(() -> userService.deleteUser(userId))
                    .doesNotThrowAnyException();

            verify(userRepository).existsById(userId);
            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent user")
        void shouldThrowExceptionWhenDeletingNonExistentUser() {
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository).existsById(userId);
            verify(userRepository, never()).deleteById(any());
        }
    }
}
