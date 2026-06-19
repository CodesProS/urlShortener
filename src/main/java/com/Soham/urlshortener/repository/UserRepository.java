package com.Soham.urlshortener.repository;

import com.Soham.urlshortener.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA generates the SQL implementation at runtime.
 * JpaRepository<User, Long> gives you save(), findById(), deleteById(), findAll() for free.
 * The custom method below generates: SELECT * FROM users WHERE email = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
