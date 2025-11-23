package com.nexusai.commons.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date and time operations.
 */
public final class DateTimeUtils {

    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Paris");

    private DateTimeUtils() {
        // Prevent instantiation
    }

    /**
     * Returns the current timestamp in UTC.
     */
    public static Instant nowUtc() {
        return Instant.now();
    }

    /**
     * Returns the current local date time in the default timezone.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * Returns the current zoned date time.
     */
    public static ZonedDateTime nowZoned() {
        return ZonedDateTime.now(DEFAULT_ZONE);
    }

    /**
     * Formats a relative time string (e.g., "il y a 5 minutes").
     */
    public static String formatRelative(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        LocalDateTime now = now();
        long seconds = ChronoUnit.SECONDS.between(dateTime, now);
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);

        if (seconds < 60) {
            return "à l'instant";
        } else if (minutes < 60) {
            return "il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        } else if (hours < 24) {
            return "il y a " + hours + " heure" + (hours > 1 ? "s" : "");
        } else if (days < 7) {
            return "il y a " + days + " jour" + (days > 1 ? "s" : "");
        } else if (days < 30) {
            long weeks = days / 7;
            return "il y a " + weeks + " semaine" + (weeks > 1 ? "s" : "");
        } else if (days < 365) {
            long months = days / 30;
            return "il y a " + months + " mois";
        } else {
            long years = days / 365;
            return "il y a " + years + " an" + (years > 1 ? "s" : "");
        }
    }

    /**
     * Checks if a date is in the past.
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isBefore(now());
    }

    /**
     * Checks if a date is in the future.
     */
    public static boolean isFuture(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(now());
    }

    /**
     * Calculates the start of the current day.
     */
    public static LocalDateTime startOfDay() {
        return now().toLocalDate().atStartOfDay();
    }

    /**
     * Calculates the end of the current day.
     */
    public static LocalDateTime endOfDay() {
        return now().toLocalDate().atTime(23, 59, 59);
    }

    /**
     * Calculates the start of the current month.
     */
    public static LocalDateTime startOfMonth() {
        return now().withDayOfMonth(1).toLocalDate().atStartOfDay();
    }

    /**
     * Adds days to a date.
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        return dateTime != null ? dateTime.plusDays(days) : null;
    }

    /**
     * Converts an Instant to LocalDateTime in the default timezone.
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, DEFAULT_ZONE) : null;
    }

    /**
     * Converts a LocalDateTime to Instant in the default timezone.
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(DEFAULT_ZONE).toInstant() : null;
    }
}
