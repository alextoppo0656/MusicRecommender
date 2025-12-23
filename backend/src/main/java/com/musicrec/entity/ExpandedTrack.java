package com.musicrec.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expanded_tracks", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "trackName", "artist"}),
    indexes = {
        @Index(name = "idx_user_expanded", columnList = "userId"),
        @Index(name = "idx_source", columnList = "source")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpandedTrack {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, length = 500)
    private String trackName;
    
    @Column(nullable = false, length = 300)
    private String artist;
    
    @Column(length = 500)
    private String album;
    
    @Column(length = 4)
    private String releaseYear;
    
    @Column(nullable = false)
    private String source; // spotify_liked, artist_similarity, genre_similarity, user_liked
    
    @Column(length = 500)
    private String tags;
    
    @Column(length = 300)
    private String artistSeed;
    
    @Column(length = 100)
    private String genreSeed;
    
    @Column(length = 100)
    private String spotifyId;
    
    @Column(length = 500)
    private String albumImage;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}