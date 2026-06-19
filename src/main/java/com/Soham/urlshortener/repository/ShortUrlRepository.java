package com.Soham.urlshortener.repository;

import com.Soham.urlshortener.model.ShortUrl;
import com.Soham.urlshortener.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    // Used for redirect lookups (hot path — this result gets cached in Redis)
    Optional<ShortUrl> findByShortUrl(String shortCode);

    // List all URLs owned by a specific user
    List<ShortUrl> findByUser(User user);

    boolean existsByShortUrl(String shortCode);

    // Increment click counter directly in DB — avoids read-modify-write race condition
    @Modifying
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 WHERE s.shortUrl = :shortCode")
    void incrementClickCount(String shortCode);
}
