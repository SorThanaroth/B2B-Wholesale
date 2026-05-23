package com.wholesale.marketplace.common.exception;

/** Thrown on state conflicts (duplicate email, already-paid order, etc.). Maps to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
