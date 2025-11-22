package com.nexusai.commons.exception;

import lombok.Getter;

/**
 * Base exception class for all NexusAI exceptions.
 */
@Getter
public class NexusException extends RuntimeException {

    private final String errorCode;
    private final transient Object[] args;

    public NexusException(String message) {
        super(message);
        this.errorCode = "NEXUS_ERROR";
        this.args = new Object[0];
    }

    public NexusException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public NexusException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public NexusException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "NEXUS_ERROR";
        this.args = new Object[0];
    }

    public NexusException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }
}
