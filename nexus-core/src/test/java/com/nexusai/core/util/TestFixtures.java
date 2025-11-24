package com.nexusai.core.util;

import com.nexusai.core.entity.*;
import com.nexusai.core.enums.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test fixtures for creating sample entities in tests.
 * Provides builder methods for common test scenarios.
 */
public final class TestFixtures {

    private TestFixtures() {
        // Utility class
    }

    /**
     * Creates a sample User with default values.
     */
    public static User createUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user" + System.currentTimeMillis() + "@example.com")
                .username("user" + System.currentTimeMillis())
                .passwordHash("$2a$10$dummyhashedpassword")
                .role(UserRole.USER)
                .subscriptionType(SubscriptionType.FREE)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .tokensRemaining(100)
                .build();
    }

    /**
     * Creates a sample User with specified email.
     */
    public static User createUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .username(email.split("@")[0])
                .passwordHash("$2a$10$dummyhashedpassword")
                .role(UserRole.USER)
                .subscriptionType(SubscriptionType.FREE)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .tokensRemaining(100)
                .build();
    }

    /**
     * Creates a sample admin User.
     */
    public static User createAdminUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("admin" + System.currentTimeMillis() + "@example.com")
                .username("admin" + System.currentTimeMillis())
                .passwordHash("$2a$10$dummyhashedpassword")
                .role(UserRole.ADMIN)
                .subscriptionType(SubscriptionType.VIP_PLUS)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .tokensRemaining(-1) // Unlimited
                .build();
    }

    /**
     * Creates a sample Companion with default values.
     */
    public static Companion createCompanion(UUID userId) {
        return Companion.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Test Companion")
                .description("A test companion for unit tests")
                .style(CompanionStyle.REALISTIC)
                .status(CompanionStatus.ACTIVE)
                .systemPrompt("You are a helpful assistant.")
                .modelProvider("ollama")
                .modelName("llama3")
                .isPublic(false)
                .totalMessages(0L)
                .likesCount(0)
                .build();
    }

    /**
     * Creates a sample public Companion.
     */
    public static Companion createPublicCompanion(UUID userId) {
        Companion companion = createCompanion(userId);
        companion.setIsPublic(true);
        companion.setName("Public Companion");
        return companion;
    }

    /**
     * Creates a sample Conversation.
     */
    public static Conversation createConversation(UUID userId, UUID companionId) {
        return Conversation.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .companionId(companionId)
                .title("Test Conversation")
                .status(ConversationStatus.ACTIVE)
                .messageCount(0)
                .totalTokens(0L)
                .lastActivityAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a sample archived Conversation.
     */
    public static Conversation createArchivedConversation(UUID userId, UUID companionId) {
        Conversation conversation = createConversation(userId, companionId);
        conversation.setStatus(ConversationStatus.ARCHIVED);
        return conversation;
    }

    /**
     * Creates a sample user Message.
     */
    public static Message createUserMessage(UUID conversationId) {
        return Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .type(MessageType.TEXT)
                .content("This is a test message")
                .isEdited(false)
                .build();
    }

    /**
     * Creates a sample user Message with content.
     */
    public static Message createUserMessage(UUID conversationId, String content) {
        Message message = createUserMessage(conversationId);
        message.setContent(content);
        return message;
    }

    /**
     * Creates a sample assistant Message.
     */
    public static Message createAssistantMessage(UUID conversationId) {
        return Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .type(MessageType.TEXT)
                .content("This is an AI response")
                .tokensUsed(50)
                .isEdited(false)
                .build();
    }

    /**
     * Creates a sample assistant Message with content.
     */
    public static Message createAssistantMessage(UUID conversationId, String content, int tokensUsed) {
        return Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .type(MessageType.TEXT)
                .content(content)
                .tokensUsed(tokensUsed)
                .isEdited(false)
                .build();
    }

    /**
     * Creates a sample system Message.
     */
    public static Message createSystemMessage(UUID conversationId, String content) {
        return Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.SYSTEM)
                .type(MessageType.TEXT)
                .content(content)
                .isEdited(false)
                .build();
    }

    /**
     * Creates a VIP user with premium subscription.
     */
    public static User createVipUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("vip" + System.currentTimeMillis() + "@example.com")
                .username("vip" + System.currentTimeMillis())
                .passwordHash("$2a$10$dummyhashedpassword")
                .role(UserRole.USER)
                .subscriptionType(SubscriptionType.VIP)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .tokensRemaining(20000)
                .build();
    }

    /**
     * Creates a suspended user.
     */
    public static User createSuspendedUser() {
        User user = createUser();
        user.setAccountStatus(AccountStatus.SUSPENDED);
        return user;
    }
}
