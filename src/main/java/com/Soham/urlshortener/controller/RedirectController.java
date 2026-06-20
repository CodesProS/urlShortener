package com.Soham.urlshortener.controller;

import com.Soham.urlshortener.model.ShortUrl;
import com.Soham.urlshortener.service.ClickService;
import com.Soham.urlshortener.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * The core feature of the app: redirect /{shortCode} → originalUrl.
 *
 * This endpoint is PUBLIC (no JWT required) — configured in SecurityConfig.
 * Every real user hits this path, so it's the highest-traffic endpoint.
 * That's exactly why we cache it: to avoid a DB query on every redirect.
 *
 * HTTP 302 vs 301:
 * - 301 Moved Permanently: browsers cache it forever. If you later delete the URL,
 *   users are stuck with the old redirect in their browser cache.
 * - 302 Found (Temporary): browser re-checks each time. Correct for user-deletable links.
 */
@RestController
@Tag(name = "Redirect", description = "Public redirect endpoint")
@RequiredArgsConstructor
public class RedirectController {

    private final ShortUrlService shortUrlService;
    private final ClickService clickService;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to original URL",
               description = "Public endpoint — no auth needed. Looks up short code, records click, redirects.")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        // This call goes through Redis cache first (cache-aside via @Cacheable)
        ShortUrl shortUrl = shortUrlService.findByShortCode(shortCode);

        // Check expiry
        if (shortUrl.getExpireAt() != null && LocalDateTime.now().isAfter(shortUrl.getExpireAt())) {
            return ResponseEntity.status(HttpStatus.GONE).build(); // 410 Gone
        }

        // Record the click (IP + User-Agent for analytics)
        String ip = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        clickService.recordClick(shortUrl, ip, userAgent);

        // HTTP 302 redirect — Location header tells the browser where to go
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, shortUrl.getOriginalUrl())
            .build();
    }

    /**
     * Respect X-Forwarded-For if behind a proxy/load balancer.
     * In Azure Container Apps, the real client IP is in X-Forwarded-For.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim(); // first IP in the chain
        }
        return request.getRemoteAddr();
    }
}
