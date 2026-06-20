package com.Soham.urlshortener.controller;

import com.Soham.urlshortener.dto.ClickStatsResponse;
import com.Soham.urlshortener.service.ClickService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Click statistics per short URL")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final ClickService clickService;

    /**
     * GET /api/analytics/{shortCode}
     *
     * Returns total clicks + daily breakdown for the last 30 days.
     * Authorization: only the URL's owner can see its analytics (enforced in ClickService).
     */
    @GetMapping("/{shortCode}")
    @Operation(summary = "Get click analytics",
               description = "Returns total clicks and daily breakdown for the last 30 days. Requires ownership.")
    public ClickStatsResponse getStats(
            @PathVariable String shortCode,
            @AuthenticationPrincipal UserDetails principal) {

        return clickService.getStats(shortCode, principal.getUsername());
    }
}
