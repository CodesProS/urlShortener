package com.Soham.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Returned after successful register or login.
 * The client must store this token and send it as:
 *   Authorization: Bearer <token>
 * on every subsequent request.
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String tokenType = "Bearer";

    public AuthResponse(String token, String email) {
        this.token = token;
        this.email = email;
    }
}
