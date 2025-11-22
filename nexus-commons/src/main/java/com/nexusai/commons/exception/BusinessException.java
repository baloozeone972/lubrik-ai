package com.nexusai.commons.exception;

/**
 * Exception thrown for business rule violations.
 */
public class BusinessException extends NexusException {

    public BusinessException(String message) {
        super("BUSINESS_ERROR", message);
    }

    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
}
