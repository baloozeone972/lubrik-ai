package com.nexusai.moderation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentFilterService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLOCKED_PATTERNS_KEY = "moderation:blocked_patterns";
    private static final String FILTER_CACHE_PREFIX = "moderation:filter_cache:";

    // Default blocked patterns (these would normally be loaded from database)
    private static final Set<Pattern> DEFAULT_BLOCKED_PATTERNS = new HashSet<>();

    static {
        // Add common harmful content patterns
        String[] blockedTerms = {
                "(?i)\\b(hate|kill|violence)\\s+(against|towards)\\b",
                "(?i)\\b(illegal|drugs|weapons)\\s+(purchase|buy|sell)\\b",
                "(?i)\\bself[\\s-]?harm\\b",
                "(?i)\\bsuicide\\s+(method|how\\s+to)\\b"
        };
        for (String term : blockedTerms) {
            DEFAULT_BLOCKED_PATTERNS.add(Pattern.compile(term));
        }
    }

    public boolean isContentSafe(String content) {
        if (content == null || content.isBlank()) {
            return true;
        }

        // Check cache first
        String cacheKey = FILTER_CACHE_PREFIX + content.hashCode();
        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return Boolean.parseBoolean(cachedResult);
        }

        boolean isSafe = performSafetyCheck(content);

        // Cache result for 1 hour
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(isSafe), Duration.ofHours(1));

        if (!isSafe) {
            log.warn("Content blocked by filter: {}", truncateForLog(content));
        }

        return isSafe;
    }

    public FilterResult analyzeContent(String content) {
        List<String> violations = new ArrayList<>();
        double riskScore = 0.0;

        if (content == null || content.isBlank()) {
            return new FilterResult(true, 0.0, violations);
        }

        // Check against blocked patterns
        for (Pattern pattern : DEFAULT_BLOCKED_PATTERNS) {
            if (pattern.matcher(content).find()) {
                violations.add("Pattern match: " + pattern.pattern());
                riskScore += 0.3;
            }
        }

        // Check for excessive caps (shouting)
        long capsCount = content.chars().filter(Character::isUpperCase).count();
        if (content.length() > 10 && (double) capsCount / content.length() > 0.7) {
            violations.add("Excessive capitalization");
            riskScore += 0.1;
        }

        // Check for repeated characters (spam indicator)
        if (hasExcessiveRepetition(content)) {
            violations.add("Excessive character repetition");
            riskScore += 0.1;
        }

        // Check for URL spam
        long urlCount = countUrls(content);
        if (urlCount > 3) {
            violations.add("Excessive URLs");
            riskScore += 0.2;
        }

        boolean isSafe = riskScore < 0.5;
        return new FilterResult(isSafe, Math.min(riskScore, 1.0), violations);
    }

    public String sanitizeContent(String content) {
        if (content == null) return null;

        // Remove potentially dangerous HTML/script tags
        String sanitized = content.replaceAll("<[^>]*>", "");

        // Remove null bytes and control characters
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        return sanitized;
    }

    public void addBlockedPattern(String pattern, String reason) {
        try {
            Pattern.compile(pattern); // Validate pattern
            redisTemplate.opsForSet().add(BLOCKED_PATTERNS_KEY, pattern + ":" + reason);
            log.info("Added blocked pattern: {}", pattern);
        } catch (Exception e) {
            log.error("Invalid regex pattern: {}", pattern, e);
            throw new IllegalArgumentException("Invalid regex pattern: " + pattern);
        }
    }

    public void removeBlockedPattern(String pattern) {
        Set<String> patterns = redisTemplate.opsForSet().members(BLOCKED_PATTERNS_KEY);
        if (patterns != null) {
            patterns.stream()
                    .filter(p -> p.startsWith(pattern + ":"))
                    .forEach(p -> redisTemplate.opsForSet().remove(BLOCKED_PATTERNS_KEY, p));
        }
        log.info("Removed blocked pattern: {}", pattern);
    }

    private boolean performSafetyCheck(String content) {
        // Check default patterns
        for (Pattern pattern : DEFAULT_BLOCKED_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return false;
            }
        }

        // Check custom patterns from Redis
        Set<String> customPatterns = redisTemplate.opsForSet().members(BLOCKED_PATTERNS_KEY);
        if (customPatterns != null) {
            for (String patternEntry : customPatterns) {
                String patternStr = patternEntry.split(":")[0];
                try {
                    if (Pattern.compile(patternStr).matcher(content).find()) {
                        return false;
                    }
                } catch (Exception ignored) {}
            }
        }

        return true;
    }

    private boolean hasExcessiveRepetition(String content) {
        int maxRepeat = 0;
        int currentRepeat = 1;
        char lastChar = 0;

        for (char c : content.toCharArray()) {
            if (c == lastChar) {
                currentRepeat++;
                maxRepeat = Math.max(maxRepeat, currentRepeat);
            } else {
                currentRepeat = 1;
            }
            lastChar = c;
        }

        return maxRepeat > 5;
    }

    private long countUrls(String content) {
        Pattern urlPattern = Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", Pattern.CASE_INSENSITIVE);
        return urlPattern.matcher(content).results().count();
    }

    private String truncateForLog(String content) {
        if (content.length() <= 50) return content;
        return content.substring(0, 50) + "...";
    }

    public record FilterResult(boolean safe, double riskScore, List<String> violations) {}
}
