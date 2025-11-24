package com.nexusai.moderation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentFilterService Tests")
class ContentFilterServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private ContentFilterService contentFilterService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        contentFilterService = new ContentFilterService(redisTemplate);
    }

    @Nested
    @DisplayName("IsContentSafe Tests")
    class IsContentSafeTests {

        @Test
        @DisplayName("Should return true for null content")
        void shouldReturnTrueForNullContent() {
            boolean result = contentFilterService.isContentSafe(null);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for blank content")
        void shouldReturnTrueForBlankContent() {
            boolean result = contentFilterService.isContentSafe("   ");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for safe content")
        void shouldReturnTrueForSafeContent() {
            when(valueOperations.get(anyString())).thenReturn(null);
            when(setOperations.members(anyString())).thenReturn(new HashSet<>());

            boolean result = contentFilterService.isContentSafe("Hello, how are you?");

            assertThat(result).isTrue();
            verify(valueOperations).set(anyString(), eq("true"), any());
        }

        @Test
        @DisplayName("Should return cached result")
        void shouldReturnCachedResult() {
            when(valueOperations.get(anyString())).thenReturn("true");

            boolean result = contentFilterService.isContentSafe("Any content");

            assertThat(result).isTrue();
            verify(setOperations, never()).members(anyString());
        }

        @Test
        @DisplayName("Should block content matching harmful patterns")
        void shouldBlockContentMatchingHarmfulPatterns() {
            when(valueOperations.get(anyString())).thenReturn(null);
            when(setOperations.members(anyString())).thenReturn(new HashSet<>());

            boolean result = contentFilterService.isContentSafe("hate against minorities");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should block content with self-harm references")
        void shouldBlockSelfHarmContent() {
            when(valueOperations.get(anyString())).thenReturn(null);
            when(setOperations.members(anyString())).thenReturn(new HashSet<>());

            boolean result = contentFilterService.isContentSafe("information about self-harm");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should check custom patterns from Redis")
        void shouldCheckCustomPatternsFromRedis() {
            when(valueOperations.get(anyString())).thenReturn(null);
            Set<String> customPatterns = Set.of("(?i)custom_bad_word:test reason");
            when(setOperations.members(anyString())).thenReturn(customPatterns);

            boolean result = contentFilterService.isContentSafe("This has custom_bad_word");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("AnalyzeContent Tests")
    class AnalyzeContentTests {

        @Test
        @DisplayName("Should return safe result for clean content")
        void shouldReturnSafeResultForCleanContent() {
            ContentFilterService.FilterResult result = contentFilterService.analyzeContent("Hello world");

            assertThat(result.safe()).isTrue();
            assertThat(result.riskScore()).isEqualTo(0.0);
            assertThat(result.violations()).isEmpty();
        }

        @Test
        @DisplayName("Should return safe for null content")
        void shouldReturnSafeForNullContent() {
            ContentFilterService.FilterResult result = contentFilterService.analyzeContent(null);

            assertThat(result.safe()).isTrue();
            assertThat(result.riskScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should detect excessive capitalization")
        void shouldDetectExcessiveCapitalization() {
            ContentFilterService.FilterResult result = contentFilterService.analyzeContent("THIS IS ALL CAPS MESSAGE");

            assertThat(result.violations()).contains("Excessive capitalization");
            assertThat(result.riskScore()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should detect excessive character repetition")
        void shouldDetectExcessiveRepetition() {
            ContentFilterService.FilterResult result = contentFilterService.analyzeContent("Hellooooooo there");

            assertThat(result.violations()).contains("Excessive character repetition");
            assertThat(result.riskScore()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should detect excessive URLs")
        void shouldDetectExcessiveUrls() {
            ContentFilterService.FilterResult result = contentFilterService.analyzeContent(
                    "Check http://a.com http://b.com http://c.com http://d.com http://e.com");

            assertThat(result.violations()).contains("Excessive URLs");
        }

        @Test
        @DisplayName("Should mark content unsafe when risk score exceeds threshold")
        void shouldMarkUnsafeWhenRiskScoreExceedsThreshold() {
            ContentFilterService.FilterResult result = contentFilterService.analyzeContent(
                    "hate against people SCREAMING http://a.com http://b.com http://c.com http://d.com");

            assertThat(result.safe()).isFalse();
            assertThat(result.riskScore()).isGreaterThanOrEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("SanitizeContent Tests")
    class SanitizeContentTests {

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            String result = contentFilterService.sanitizeContent(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should remove HTML tags")
        void shouldRemoveHtmlTags() {
            String result = contentFilterService.sanitizeContent("<script>alert('xss')</script>Hello");
            assertThat(result).isEqualTo("alert('xss')Hello");
        }

        @Test
        @DisplayName("Should remove control characters")
        void shouldRemoveControlCharacters() {
            String result = contentFilterService.sanitizeContent("Hello\u0000World\u0001");
            assertThat(result).isEqualTo("HelloWorld");
        }

        @Test
        @DisplayName("Should normalize whitespace")
        void shouldNormalizeWhitespace() {
            String result = contentFilterService.sanitizeContent("Hello    World   ");
            assertThat(result).isEqualTo("Hello World");
        }
    }

    @Nested
    @DisplayName("BlockedPattern Management Tests")
    class BlockedPatternManagementTests {

        @Test
        @DisplayName("Should add valid blocked pattern")
        void shouldAddValidBlockedPattern() {
            contentFilterService.addBlockedPattern("(?i)bad_word", "test reason");

            verify(setOperations).add(anyString(), eq("(?i)bad_word:test reason"));
        }

        @Test
        @DisplayName("Should throw exception for invalid regex pattern")
        void shouldThrowExceptionForInvalidPattern() {
            assertThatThrownBy(() -> contentFilterService.addBlockedPattern("[invalid", "reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid regex pattern");
        }

        @Test
        @DisplayName("Should remove blocked pattern")
        void shouldRemoveBlockedPattern() {
            Set<String> patterns = new HashSet<>();
            patterns.add("test_pattern:reason");
            when(setOperations.members(anyString())).thenReturn(patterns);

            contentFilterService.removeBlockedPattern("test_pattern");

            verify(setOperations).remove(anyString(), eq("test_pattern:reason"));
        }
    }
}
