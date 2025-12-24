package com.musicrec.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracks", indexes = {
    @Index(name = "idx_user_source", columnList = "userId,source"),
    @Index(name = "idx_user_id", columnList = "userId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Track {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    private String spotifyId;
    
    @Column(nullable = false)
    private String trackName;
    
    @Column(nullable = false)
    private String artist;
    
    // IMPORTANT: This field is crucial for batch genre fetching
    private String artistId;
    
    private String album;
    
    private String year;
    
    @Column(length = 1000)
    private String tags; // Genres, comma-separated
    
    @Column(length = 500)
    private String albumImage;
    
    @Column(nullable = false)
    private String source; // "spotify_liked", "artist_similarity", "genre_similarity"
    
    // Seeds for recommendations
    private String artistSeed; // Which artist led to this recommendation
    private String genreSeed; // Which genre led to this recommendation
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}