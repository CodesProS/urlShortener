package com.Soham.urlshortener.controller;

import com.Soham.urlshortener.dto.AuthResponse;
import com.Soham.urlshortener.dto.LoginRequest;
import com.Soham.urlshortener.dto.RegisterRequest;
import com.Soham.urlshortener.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login to get a JWT token")
public class AuthController {

    private final UserService userService;

    /**
     * @Valid triggers Bean Validation on RegisterRequest (email, password constraints).
     * If validation fails before reaching this method, GlobalExceptionHandler handles it.
     *
     * Returns 201 Created (not 200) — semantically correct: a new resource was created.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user", description = "Creates an account and returns a JWT token")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    /**
     * Returns 200 OK with the JWT in the response body.
     * The client must store this token (localStorage, secure cookie, etc.) and send it as:
     *   Authorization: Bearer <token>
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates credentials and returns a JWT token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
