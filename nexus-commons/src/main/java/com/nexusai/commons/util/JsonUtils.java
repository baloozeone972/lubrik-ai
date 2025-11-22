package com.nexusai.commons.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * Utility class for JSON operations.
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtils() {
        // Prevent instantiation
    }

    /**
     * Returns the shared ObjectMapper instance.
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Converts an object to JSON string.
     */
    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * Converts an object to pretty-printed JSON string.
     */
    public static String toPrettyJson(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * Converts a JSON string to an object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to object", e);
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    /**
     * Converts a JSON string to an object with type reference.
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to object", e);
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    /**
     * Safely converts an object to JSON, returning empty on failure.
     */
    public static Optional<String> toJsonSafe(Object object) {
        try {
            return Optional.of(OBJECT_MAPPER.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON", e);
            return Optional.empty();
        }
    }

    /**
     * Safely converts a JSON string to an object.
     */
    public static <T> Optional<T> fromJsonSafe(String json, Class<T> clazz) {
        try {
            return Optional.of(OBJECT_MAPPER.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON to object", e);
            return Optional.empty();
        }
    }

    /**
     * Converts an object to a Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object object) {
        return OBJECT_MAPPER.convertValue(object, Map.class);
    }

    /**
     * Converts a Map to an object.
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(map, clazz);
    }
}
