package com.musicrec.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "trackName", "artist"}),
    indexes = {
        @Index(name = "idx_user_feedback", columnList = "userId"),
        @Index(name = "idx_liked", columnList = "liked")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, length = 500)
    private String trackName;
    
    @Column(nullable = false, length = 300)
    private String artist;
    
    @Column(nullable = false)
    private Boolean liked;
    
    @Column(length = 500)
    private String album;
    
    @Column(length = 4)
    private String releaseYear;
    
    @Column(length = 100)
    private String spotifyId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        timestamp = LocalDateTime.now();
    }
}