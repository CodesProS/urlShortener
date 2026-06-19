package com.Soham.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error envelope returned for ALL error scenarios.
 * @JsonInclude(NON_NULL) means fields that are null are omitted from JSON output.
 * This prevents {"details": null} cluttering simple 404 responses.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String error;           // e.g. "Not Found"
    private String message;         // human-readable explanation
    private LocalDateTime timestamp;
    private String path;            // which endpoint was called
    private List<String> details;   // field-level validation errors (only for 400)
}
