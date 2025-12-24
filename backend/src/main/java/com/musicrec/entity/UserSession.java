package com.musicrec.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {
    
    @Id
    private String userId;
    
    @Column(nullable = false)
    private String tokenHash;
    
    @Column(nullable = false)
    private LocalDateTime lastSeen = LocalDateTime.now();
    
    @Column(length = 20)
    private String sessionId;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastSeen = LocalDateTime.now();
    }
}