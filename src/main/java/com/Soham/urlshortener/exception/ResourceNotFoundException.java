package com.Soham.urlshortener.exception;

/**
 * Thrown when a requested resource doesn't exist (short URL not found, user not found).
 * GlobalExceptionHandler maps this to HTTP 404.
 *
 * Extending RuntimeException means you don't need to declare it with 'throws'.
 * Spring only rolls back transactions on RuntimeExceptions by default.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
