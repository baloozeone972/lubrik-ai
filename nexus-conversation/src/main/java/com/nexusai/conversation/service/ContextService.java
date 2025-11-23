package com.nexusai.conversation.service;

import com.nexusai.core.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContextService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String CONTEXT_KEY_PREFIX = "conversation:context:";
    private static final String SUMMARY_KEY_PREFIX = "conversation:summary:";
    private static final Duration CONTEXT_TTL = Duration.ofHours(24);
    private static final int MAX_CONTEXT_TOKENS = 4000;

    private final ConcurrentHashMap<UUID, StringBuilder> activeContexts = new ConcurrentHashMap<>();

    public String buildContext(UUID conversationId, List<Message> messages) {
        StringBuilder context = new StringBuilder();

        // Check for cached summary
        String cachedSummary = getCachedSummary(conversationId);
        if (cachedSummary != null) {
            context.append("[Previous context summary]\n").append(cachedSummary).append("\n\n");
        }

        // Add recent messages
        context.append("[Recent conversation]\n");
        for (Message message : messages) {
            String rolePrefix = message.isUserMessage() ? "User: " : "Assistant: ";
            context.append(rolePrefix).append(message.getContent()).append("\n");
        }

        // Trim if too long
        String finalContext = context.toString();
        if (estimateTokens(finalContext) > MAX_CONTEXT_TOKENS) {
            finalContext = truncateContext(finalContext);
        }

        // Cache the context
        cacheContext(conversationId, finalContext);

        return finalContext;
    }

    public void updateContext(UUID conversationId, String role, String content) {
        activeContexts.computeIfAbsent(conversationId, k -> new StringBuilder())
                .append(role).append(": ").append(content).append("\n");
    }

    public void clearContext(UUID conversationId) {
        activeContexts.remove(conversationId);
        String contextKey = CONTEXT_KEY_PREFIX + conversationId;
        String summaryKey = SUMMARY_KEY_PREFIX + conversationId;
        redisTemplate.delete(contextKey);
        redisTemplate.delete(summaryKey);
        log.info("Cleared context for conversation {}", conversationId);
    }

    public void summarizeAndCache(UUID conversationId, String summary) {
        String summaryKey = SUMMARY_KEY_PREFIX + conversationId;
        redisTemplate.opsForValue().set(summaryKey, summary, CONTEXT_TTL);
        log.debug("Cached summary for conversation {}", conversationId);
    }

    public String getCachedSummary(UUID conversationId) {
        String summaryKey = SUMMARY_KEY_PREFIX + conversationId;
        return redisTemplate.opsForValue().get(summaryKey);
    }

    public void setMemory(UUID conversationId, String key, String value) {
        String memoryKey = CONTEXT_KEY_PREFIX + conversationId + ":memory:" + key;
        redisTemplate.opsForValue().set(memoryKey, value, Duration.ofDays(30));
    }

    public String getMemory(UUID conversationId, String key) {
        String memoryKey = CONTEXT_KEY_PREFIX + conversationId + ":memory:" + key;
        return redisTemplate.opsForValue().get(memoryKey);
    }

    private void cacheContext(UUID conversationId, String context) {
        String contextKey = CONTEXT_KEY_PREFIX + conversationId;
        redisTemplate.opsForValue().set(contextKey, context, CONTEXT_TTL);
    }

    private String truncateContext(String context) {
        // Simple truncation - keep last portion
        int maxChars = MAX_CONTEXT_TOKENS * 4; // Rough estimate: 1 token ≈ 4 chars
        if (context.length() > maxChars) {
            int startIndex = context.length() - maxChars;
            // Find next newline to avoid cutting mid-message
            int newlineIndex = context.indexOf('\n', startIndex);
            if (newlineIndex > 0 && newlineIndex < context.length()) {
                return "...[truncated]\n" + context.substring(newlineIndex + 1);
            }
            return "...[truncated]\n" + context.substring(startIndex);
        }
        return context;
    }

    private int estimateTokens(String text) {
        // Rough estimation: ~4 characters per token
        return text.length() / 4;
    }
}
