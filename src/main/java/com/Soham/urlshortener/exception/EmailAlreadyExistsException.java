package com.Soham.urlshortener.exception;

/**
 * Thrown when a registration attempt uses an email already in the database.
 * GlobalExceptionHandler maps this to HTTP 409 Conflict.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
