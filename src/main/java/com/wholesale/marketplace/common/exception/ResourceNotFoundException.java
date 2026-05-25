package com.wholesale.marketplace.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " not found: " + id);
    }
}
