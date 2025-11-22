package com.nexusai.commons.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails.
 */
@Getter
public class ValidationException extends NexusException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = new HashMap<>();
    }

    public ValidationException(String field, String message) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = new HashMap<>();
        this.fieldErrors.put(field, message);
    }

    public ValidationException(Map<String, String> fieldErrors) {
        super("VALIDATION_ERROR", "Validation failed");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException addFieldError(String field, String message) {
        this.fieldErrors.put(field, message);
        return this;
    }

    public boolean hasErrors() {
        return !fieldErrors.isEmpty();
    }
}
