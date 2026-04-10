package com.alahlia.actionsheet.exception;

/**
 * Thrown when a requested resource does not exist.
 * Automatically mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier);
    }
}
