package com.nexusai.core.service;

import com.nexusai.core.entity.User;
import com.nexusai.core.enums.AccountStatus;
import com.nexusai.core.enums.SubscriptionType;
import com.nexusai.core.enums.UserRole;
import com.nexusai.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;

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
                //.id(userId)
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .passwordHash("hashedpassword")
                .role(UserRole.USER)
                .subscriptionType(SubscriptionType.FREE)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
               // .createdAt(LocalDateTime.now())
               // .updatedAt(LocalDateTime.now())
                .build();
    }


}
