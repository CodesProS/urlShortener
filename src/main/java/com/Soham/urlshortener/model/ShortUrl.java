package com.Soham.urlshortener.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@Entity
@Table(name = "short_url")
@Data
@NoArgsConstructor
@AllArgsConstructor
// Handles Hibernate proxy metadata during Redis JSON serialization
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ShortUrl implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalUrl;

    @Column(nullable = false, unique = true)
    private String shortUrl;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private Long clickCount = 0L;

    @Column(nullable = true, updatable = false)
    private LocalDateTime expireAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore  // exclude from Redis cache — prevents password hash leaking into cache
    private User user;
}
