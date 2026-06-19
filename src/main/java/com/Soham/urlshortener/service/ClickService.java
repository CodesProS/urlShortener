package com.Soham.urlshortener.service;

import com.Soham.urlshortener.dto.ClickStatsResponse;
import com.Soham.urlshortener.exception.ResourceNotFoundException;
import com.Soham.urlshortener.model.Click;
import com.Soham.urlshortener.model.ShortUrl;
import com.Soham.urlshortener.repository.ClickRepository;
import com.Soham.urlshortener.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClickService {

    private final ClickRepository clickRepository;
    private final ShortUrlRepository shortUrlRepository;

    /**
     * Record a click event.
     * Called on every redirect — keeps an audit trail per visitor.
     * Also bumps the denormalized clickCount on ShortUrl (for fast total display).
     *
     * Note on @Modifying + @Transactional:
     * The @Query in ShortUrlRepository issues an UPDATE directly — no entity read/write cycle,
     * which avoids the optimistic locking cost of a read-modify-write pattern.
     */
    @Transactional
    public void recordClick(ShortUrl shortUrl, String ipAddress, String userAgent) {
        // shortUrl may be a Redis-deserialized object (not a JPA-managed entity).
        // getReferenceById() creates a managed proxy using only the ID — no extra DB query.
        ShortUrl managed = shortUrlRepository.getReferenceById(shortUrl.getId());

        Click click = new Click();
        click.setShortUrl(managed);
        click.setIpAddress(ipAddress);
        click.setUserAgent(userAgent);
        clickRepository.save(click);

        shortUrlRepository.incrementClickCount(shortUrl.getShortUrl());
    }

    /**
     * Build analytics for a given short code.
     * Shows total clicks + daily breakdown for the last 30 days.
     */
    @Transactional(readOnly = true)
    public ClickStatsResponse getStats(String shortCode, String requestingUserEmail) {
        ShortUrl shortUrl = shortUrlRepository.findByShortUrl(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Short URL '" + shortCode + "' not found"));

        // Authorization: only the owner can see analytics
        if (!shortUrl.getUser().getEmail().equals(requestingUserEmail)) {
            throw new SecurityException("You don't own this URL");
        }

        long totalClicks = clickRepository.countByShortUrl(shortUrl);
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        // Raw result: List of [date, count] pairs from JPQL aggregate query
        List<Object[]> rawByDay = clickRepository.countClicksByDay(shortUrl.getId(), since);

        // Convert to a readable Map<"2026-06-19", 42L>
        Map<String, Long> clicksByDay = new LinkedHashMap<>();
        for (Object[] row : rawByDay) {
            String date = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            clicksByDay.put(date, count);
        }

        return ClickStatsResponse.builder()
            .shortCode(shortCode)
            .originalUrl(shortUrl.getOriginalUrl())
            .totalClicks(totalClicks)
            .clicksByDay(clicksByDay)
            .build();
    }
}
