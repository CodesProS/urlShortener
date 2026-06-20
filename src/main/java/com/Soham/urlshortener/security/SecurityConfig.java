package com.Soham.urlshortener.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The security configuration: the most important class in the security layer.
 *
 * Key decisions made here:
 * 1. STATELESS sessions — no HTTP session, no cookie. Every request must carry its JWT.
 * 2. CSRF disabled — CSRF attacks exploit session cookies. Since we use JWTs in headers,
 *    not cookies, CSRF is irrelevant and disabling it simplifies API clients.
 * 3. BCrypt with cost factor 10 — each hash takes ~100ms on modern hardware.
 *    This is intentional: makes brute-forcing 10,000x slower than plaintext comparison.
 * 4. Our JwtAuthFilter runs BEFORE UsernamePasswordAuthenticationFilter in the chain.
 *
 * Why Container Apps over App Service (preview of Azure section):
 *   Container Apps auto-scales to zero (no cost when idle), supports KEDA event-driven scaling,
 *   and is fully container-native. App Service is a PaaS layer on top of VMs — less flexible.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder(10) = cost factor 10 (default). Higher = slower + safer.
        return new BCryptPasswordEncoder();
    }

    /**
     * DaoAuthenticationProvider ties together:
     * - How to load users (CustomUserDetailsService)
     * - How to verify passwords (BCrypt)
     * Spring Security's AuthenticationManager delegates to this provider.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 7: UserDetailsService is required in the constructor (no-arg removed)
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager is the main entry point for authentication.
     * We inject it into UserService to authenticate login requests.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * The filter chain: defines which URLs are public vs protected.
     *
     * Public routes:
     *   POST /api/auth/**         → register and login (no token yet)
     *   GET  /{shortCode}         → redirect endpoint (public by design)
     *   GET  /swagger-ui/**       → API docs (useful for testing)
     *   GET  /v3/api-docs/**      → OpenAPI spec (Swagger reads this)
     *
     * Everything else requires a valid JWT in the Authorization header.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — public
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                // Redirect — public (the whole point of a URL shortener)
                .requestMatchers(HttpMethod.GET, "/{shortCode}").permitAll()
                // Swagger UI and OpenAPI spec
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            // Our filter runs before Spring's default UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
