package com.Soham.urlshortener.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Everything JWT lives here: generation, validation, and claim extraction.
 *
 * How JJWT 0.12 works (this is the version you have — older tutorials use 0.9 which differs):
 *   - Jwts.builder()         → build a new token
 *   - Jwts.parser()          → verify + parse an incoming token
 *   - Keys.hmacShaKeyFor()   → create an HMAC-SHA key from your secret bytes
 *
 * HS256 = HMAC with SHA-256. The signing key must be at least 256 bits (32 bytes).
 * Your secret in application.properties must be at least 32 characters.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // default: 24 hours in ms
    private long expirationMs;

    /**
     * Lazily build the SecretKey from the raw string.
     * We derive it once; HMAC-SHA-256 requires the key to be ≥ 256 bits.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build a signed JWT token.
     *
     * Payload ("claims") contains:
     *   sub  = the user's email (subject)
     *   iat  = issued-at timestamp
     *   exp  = expiry timestamp
     *
     * The token is signed with HS256 using your secret key.
     * Anyone can BASE64-decode the header/payload, but they cannot forge the signature.
     */
    public String generateToken(String email) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(email)           // who this token belongs to
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey()) // algorithm inferred from key type (HS256)
            .compact();
    }

    /**
     * Extract the email (subject) from a valid token.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Full validation: signature check + expiry check.
     * Returns true only if the token is both legitimate and not expired.
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token); // throws if invalid or expired
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        // verifyWith() checks the signature — throws SignatureException if tampered
        // build().parseSignedClaims() also checks expiry — throws ExpiredJwtException if stale
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
