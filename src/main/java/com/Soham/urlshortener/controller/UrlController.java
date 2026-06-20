package com.Soham.urlshortener.controller;

import com.Soham.urlshortener.dto.ShortenRequest;
import com.Soham.urlshortener.dto.ShortenResponse;
import com.Soham.urlshortener.model.User;
import com.Soham.urlshortener.service.ShortUrlService;
import com.Soham.urlshortener.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @SecurityRequirement tells Swagger UI to show the lock icon on these endpoints,
 * prompting the user to paste their JWT before testing.
 */
@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Tag(name = "URL Management", description = "Shorten, list, and delete URLs")
@SecurityRequirement(name = "Bearer Authentication")
public class UrlController {

    private final ShortUrlService shortUrlService;
    private final UserService userService;

    /**
     * POST /api/urls/shorten
     *
     * @AuthenticationPrincipal injects the currently authenticated UserDetails.
     * Spring Security populated this from the JWT in JwtAuthFilter.
     *
     * Rate limiting is enforced upstream by RateLimitFilter for this path.
     */
    @PostMapping("/shorten")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Shorten a URL",
               description = "Creates a short code. Rate limited to 10 requests/minute per IP.")
    public ShortenResponse shorten(
            @Valid @RequestBody ShortenRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        User owner = userService.findByEmail(principal.getUsername());
        return shortUrlService.shorten(request, owner);
    }

    /**
     * GET /api/urls
     * Returns all URLs created by the authenticated user.
     */
    @GetMapping
    @Operation(summary = "List my URLs", description = "Returns all shortened URLs for the current user")
    public List<ShortenResponse> listMyUrls(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername());
        return shortUrlService.listByUser(user);
    }

    /**
     * DELETE /api/urls/{shortCode}
     * 204 No Content = success with no response body (RESTful convention for deletes).
     */
    @DeleteMapping("/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a URL", description = "Deletes the URL and evicts it from the Redis cache")
    public void delete(
            @PathVariable String shortCode,
            @AuthenticationPrincipal UserDetails principal) {

        shortUrlService.delete(shortCode, principal.getUsername());
    }
}
