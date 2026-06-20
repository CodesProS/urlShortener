package com.Soham.urlshortener.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This filter intercepts every HTTP request exactly once (OncePerRequestFilter)
 * and authenticates it if it carries a valid JWT.
 *
 * Filter chain position: runs BEFORE Spring Security's default auth filter.
 * (We register it in SecurityConfig with addFilterBefore().)
 *
 * Flow for each request:
 *  1. Check for "Authorization: Bearer <token>" header
 *  2. If absent → skip (unauthenticated request proceeds to security rules)
 *  3. If present → extract email from JWT → load UserDetails from DB
 *  4. If token valid → set Authentication in SecurityContextHolder
 *  5. SecurityContextHolder is a thread-local: lives for exactly this request's thread
 *
 * After this filter runs, Spring Security checks your SecurityFilterChain rules.
 * If the endpoint requires auth and SecurityContextHolder is empty → 401 automatically.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No Authorization header, or not a Bearer token — pass through unchanged
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer abc.def.ghi" → "abc.def.ghi"
        String token = authHeader.substring(7);

        if (!jwtUtil.isValid(token)) {
            // Invalid / expired token — don't set auth, let security rules reject it
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.extractEmail(token);

        // Only authenticate if not already authenticated (avoids redundant DB hits)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Create an authentication token (credentials are null — we trust the JWT)
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,                          // credentials (not needed after JWT validation)
                    userDetails.getAuthorities()
                );

            // Attach request metadata (IP, session) to the authentication object
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Store in thread-local context — Spring Security reads this for authorization
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
