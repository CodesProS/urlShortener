package com.Soham.urlshortener.service;

import com.Soham.urlshortener.dto.AuthResponse;
import com.Soham.urlshortener.dto.LoginRequest;
import com.Soham.urlshortener.dto.RegisterRequest;
import com.Soham.urlshortener.exception.EmailAlreadyExistsException;
import com.Soham.urlshortener.model.User;
import com.Soham.urlshortener.repository.UserRepository;
import com.Soham.urlshortener.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // Lombok: generates constructor injecting all final fields
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;  // BCrypt — wired in SecurityConfig
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Register flow:
     * 1. Check email uniqueness
     * 2. Hash the password with BCrypt (NEVER store plain text)
     * 3. Save user
     * 4. Issue a JWT immediately (no second login needed after registration)
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                "An account with email " + request.getEmail() + " already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        // BCrypt hash: adds random salt, applies cost factor — output looks like $2a$10$...
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail());
    }

    /**
     * Login flow:
     * 1. Delegate credential check to Spring Security's AuthenticationManager
     *    It calls our CustomUserDetailsService + BCrypt comparison internally.
     * 2. On success, issue JWT.
     * 3. On failure, AuthenticationManager throws BadCredentialsException (caught globally).
     */
    public AuthResponse login(LoginRequest request) {
        // This single line triggers: load user from DB → compare BCrypt hashes
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String email = auth.getName(); // the authenticated principal's username (= email)
        String token = jwtUtil.generateToken(email);
        return new AuthResponse(token, email);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
