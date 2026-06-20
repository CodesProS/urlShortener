package com.Soham.urlshortener.repository;

import com.Soham.urlshortener.model.Click;
import com.Soham.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {

    List<Click> findByShortUrl(ShortUrl shortUrl);

    long countByShortUrl(ShortUrl shortUrl);

    // Native query — avoids JPQL CAST inconsistencies across Hibernate versions.
    // DATE(clicked_at) truncates timestamp to date in PostgreSQL.
    // Returns Object[] where [0] = java.sql.Date, [1] = Long count.
    @Query(value = "SELECT DATE(clicked_at), COUNT(*) FROM clicks " +
                   "WHERE short_url_id = :shortUrlId AND clicked_at >= :since " +
                   "GROUP BY DATE(clicked_at) ORDER BY DATE(clicked_at)",
           nativeQuery = true)
    List<Object[]> countClicksByDay(@Param("shortUrlId") Long shortUrlId,
                                    @Param("since") LocalDateTime since);
}
