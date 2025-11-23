package com.nexusai.commons.util;

import org.apache.commons.lang3.StringUtils as ApacheStringUtils;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility class for string operations.
 */
public final class StringUtils {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern HTML_TAGS_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");

    private StringUtils() {
        // Prevent instantiation
    }

    /**
     * Truncates a string to the specified maximum length.
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }

    /**
     * Sanitizes input to prevent XSS attacks.
     */
    public static String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return HTML_TAGS_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Removes diacritical marks from a string.
     */
    public static String removeDiacritics(String str) {
        if (str == null) {
            return null;
        }
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
    }

    /**
     * Converts a string to a URL-friendly slug.
     */
    public static String toSlug(String str) {
        if (str == null) {
            return null;
        }
        String normalized = removeDiacritics(str.toLowerCase().trim());
        return normalized.replaceAll("\\s+", "-")
                        .replaceAll("[^a-z0-9-]", "")
                        .replaceAll("-+", "-")
                        .replaceAll("^-|-$", "");
    }

    /**
     * Checks if a string is blank (null, empty, or whitespace only).
     */
    public static boolean isBlank(String str) {
        return ApacheStringUtils.isBlank(str);
    }

    /**
     * Checks if a string is not blank.
     */
    public static boolean isNotBlank(String str) {
        return ApacheStringUtils.isNotBlank(str);
    }

    /**
     * Masks sensitive data, showing only the last N characters.
     */
    public static String maskSensitive(String str, int visibleChars) {
        if (str == null || str.length() <= visibleChars) {
            return str;
        }
        int maskLength = str.length() - visibleChars;
        return "*".repeat(maskLength) + str.substring(maskLength);
    }

    /**
     * Masks an email address for privacy.
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }
}
