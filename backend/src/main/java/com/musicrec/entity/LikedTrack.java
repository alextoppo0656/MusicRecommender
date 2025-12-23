package com.musicrec.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "liked_tracks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "trackName", "artist"}),
    indexes = @Index(name = "idx_user_liked", columnList = "userId")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikedTrack {
    
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
    
    @Column(length = 100)
    private String spotifyId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}