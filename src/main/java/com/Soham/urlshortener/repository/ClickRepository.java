package com.Soham.urlshortener.repository;

import com.Soham.urlshortener.model.Click;
import com.Soham.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {

    List<Click> findByShortUrl(ShortUrl shortUrl);

    long countByShortUrl(ShortUrl shortUrl);

    // Analytics: clicks grouped by date — a JPQL aggregate query
    // Returns List<Object[]> where [0] = date string, [1] = count
    @Query("SELECT CAST(c.clickedAt AS date), COUNT(c) FROM Click c " +
           "WHERE c.shortUrl = :shortUrl AND c.clickedAt >= :since " +
           "GROUP BY CAST(c.clickedAt AS date) ORDER BY CAST(c.clickedAt AS date)")
    List<Object[]> countClicksByDay(ShortUrl shortUrl, LocalDateTime since);
}
