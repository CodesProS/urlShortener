package com.Soham.urlshortener.service;

import com.Soham.urlshortener.dto.ShortenRequest;
import com.Soham.urlshortener.dto.ShortenResponse;
import com.Soham.urlshortener.exception.ResourceNotFoundException;
import com.Soham.urlshortener.model.ShortUrl;
import com.Soham.urlshortener.model.User;
import com.Soham.urlshortener.repository.ClickRepository;
import com.Soham.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final ClickRepository clickRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Shorten a URL.
     * If the user supplies a custom code, validate it's not taken.
     * Otherwise, generate an 8-char alphanumeric code (collision retry loop).
     */
    @Transactional
    public ShortenResponse shorten(ShortenRequest request, User owner) {
        String code;
        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()) {
            code = request.getCustomCode();
            if (shortUrlRepository.existsByShortUrl(code)) {
                throw new IllegalArgumentException(
                    "Short code '" + code + "' is already taken. Choose another.");
            }
        } else {
            // Generate until unique (birthday-problem: very rare collision at this scale)
            do {
                code = generateCode();
            } while (shortUrlRepository.existsByShortUrl(code));
        }

        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl(request.getOriginalUrl());
        entity.setShortUrl(code);
        entity.setUser(owner);
        entity.setClickCount(0L);
        entity.setExpireAt(request.getExpiresAt());
        shortUrlRepository.save(entity);

        return toResponse(entity);
    }

    /**
     * CACHE-ASIDE PATTERN via Spring's @Cacheable:
     *
     * 1. Spring checks Redis: does key "urls::abc12345" exist?
     * 2. Cache HIT  → return the cached originalUrl immediately (no DB query)
     * 3. Cache MISS → execute this method body → store result in Redis → return it
     *
     * Why Redis over in-memory Map:
     * - Survives app restarts (in-memory cache doesn't)
     * - Shared across multiple app instances (horizontal scaling)
     * - Supports TTL — entries expire automatically
     * - Can handle millions of entries without GC pressure on the JVM heap
     */
    @Cacheable(value = "urls", key = "#shortCode")
    @Transactional(readOnly = true)
    public ShortUrl findByShortCode(String shortCode) {
        return shortUrlRepository.findByShortUrl(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Short URL '" + shortCode + "' not found"));
    }

    /**
     * @CacheEvict removes the entry from Redis when the URL is deleted.
     * Without this, deleted URLs would still redirect for up to TTL minutes.
     */
    @CacheEvict(value = "urls", key = "#shortCode")
    @Transactional
    public void delete(String shortCode, String requestingUserEmail) {
        ShortUrl entity = shortUrlRepository.findByShortUrl(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Short URL '" + shortCode + "' not found"));

        // Authorization check: only the owner can delete their URL
        if (!entity.getUser().getEmail().equals(requestingUserEmail)) {
            throw new SecurityException("You don't own this URL");
        }

        // Delete child clicks first to avoid FK constraint violation
        clickRepository.deleteAll(clickRepository.findByShortUrl(entity));
        shortUrlRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<ShortenResponse> listByUser(User user) {
        return shortUrlRepository.findByUser(user)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // --- private helpers ---

    private String generateCode() {
        // Take first 8 chars of a UUID (after removing hyphens) — lowercase + digits
        // e.g. "1a2b3c4d". Simple, no external library needed.
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private ShortenResponse toResponse(ShortUrl entity) {
        return ShortenResponse.builder()
            .id(entity.getId())
            .originalUrl(entity.getOriginalUrl())
            .shortCode(entity.getShortUrl())
            .shortUrl(baseUrl + "/" + entity.getShortUrl())
            .clickCount(entity.getClickCount())
            .createdAt(entity.getCreatedAt())
            .expiresAt(entity.getExpireAt())
            .build();
    }
}
