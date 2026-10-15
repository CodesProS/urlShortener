package com.Soham.urlshortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

@Data
public class ShortenRequest {

    @NotBlank(message = "URL is required")
    @URL(message = "Must be a valid URL (include http:// or https://)")
    private String originalUrl;

    /**
     * Optional: let the user provide a custom short code.
     * Pattern: only alphanumeric + hyphens, 4-20 chars.
     * If null, we auto-generate one.
     */
    @Pattern(regexp = "^[a-zA-Z0-9-]{4,20}$",
             message = "Custom code must be 4-20 alphanumeric characters")
    private String customCode;

    /**
     * Optional expiry. Null means the URL never expires.
     * Instant (not LocalDateTime) so the timezone in "2026-10-15T05:00:00Z" is respected
     * instead of being silently dropped and read as server-local time.
     */
    @Future(message = "Expiry must be in the future")
    private Instant expiresAt;
}
