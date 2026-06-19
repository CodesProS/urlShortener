package com.Soham.urlshortener.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Builder generates: ShortenResponse.builder().shortCode("abc").originalUrl("...").build()
 * Much cleaner than a constructor with 5+ parameters.
 */
@Data
@Builder
public class ShortenResponse {
    private Long id;
    private String originalUrl;
    private String shortCode;
    private String shortUrl;        // full URL: https://yourdomain.com/abc12345
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
