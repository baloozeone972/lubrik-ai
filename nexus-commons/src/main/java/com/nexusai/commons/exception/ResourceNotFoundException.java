package com.nexusai.commons.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends NexusException {

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s with id '%s' not found", resourceType, resourceId));
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
}
