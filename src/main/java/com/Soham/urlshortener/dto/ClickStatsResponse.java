package com.Soham.urlshortener.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ClickStatsResponse {
    private String shortCode;
    private String originalUrl;
    private long totalClicks;
    /**
     * Key = date string (e.g. "2026-06-19"), Value = click count that day.
     * A LinkedHashMap preserves insertion order (chronological).
     */
    private Map<String, Long> clicksByDay;
    private String topReferrer;   // null if no referrer data yet
}
