package com.Soham.urlshortener.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiter on POST /api/urls/shorten.
 *
 * How the token-bucket algorithm works:
 *   - Each IP starts with a bucket of N tokens.
 *   - Every request consumes 1 token.
 *   - Tokens refill at a fixed rate (e.g. 10 tokens per minute).
 *   - When the bucket is empty, the request is rejected with HTTP 429.
 *
 * Why token-bucket over a fixed window counter:
 *   A fixed window allows 10 requests at 00:59 and 10 more at 01:00 —
 *   that's 20 requests in 2 seconds (boundary-burst attack).
 *   Token-bucket smooths this out: tokens only accumulate at the refill rate.
 *
 * Why in-memory over Redis-backed (for this project):
 *   In-memory is simpler and has no network latency. For a multi-instance production
 *   deployment, swap to bucket4j-redis with Lettuce so all instances share one bucket.
 *   See: https://bucket4j.com/8.x/toc.html#redis-integration
 *
 * Limits: 10 requests per minute per IP on POST /api/urls/shorten.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // One bucket per client IP — ConcurrentHashMap is thread-safe
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(10)                           // max 10 tokens in the bucket
            .refillGreedy(10, Duration.ofMinutes(1)) // refill 10 tokens every minute
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Only rate-limit the shorten endpoint
        if (!request.getMethod().equals("POST") ||
            !request.getRequestURI().equals("/api/urls/shorten")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractIp(request);

        // computeIfAbsent: atomically creates a bucket for new IPs
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Rate limit exceeded: 10 requests per minute allowed on this endpoint"
                }
                """);
        }
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
