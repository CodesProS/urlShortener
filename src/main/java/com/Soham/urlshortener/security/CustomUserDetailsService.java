package com.Soham.urlshortener.security;

import com.Soham.urlshortener.model.User;
import com.Soham.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security calls this when it needs to verify credentials.
 * The AuthenticationManager uses it during login: it loads the user by email,
 * then Spring compares the supplied password against the stored BCrypt hash.
 *
 * We're using the builder pattern from spring-security-core's User class (not our User entity)
 * to produce a UserDetails object — Spring Security only cares about the password hash and roles.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "No account found with email: " + email));

        // Build a Spring Security UserDetails wrapper around our User entity.
        // The password here is the BCrypt hash — Spring Security handles comparison.
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword()) // BCrypt hash, e.g. "$2a$10$..."
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
    }
}
