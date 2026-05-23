package com.wholesale.marketplace.common.exception;

/** Thrown on invalid input or violated business rules (e.g. below min order qty). Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
