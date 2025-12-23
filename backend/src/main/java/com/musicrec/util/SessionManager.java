package com.musicrec.util;

import com.musicrec.entity.UserSession;
import com.musicrec.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionManager {
    
    private final UserSessionRepository sessionRepository;
    
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    public boolean validateSession(String userId, String token) {
        String tokenHash = hashToken(token);
        
        UserSession session = sessionRepository.findById(userId).orElse(null);
        
        if (session != null) {
            if (!session.getTokenHash().equals(tokenHash)) {
                log.warn("Token mismatch for user {}. Updating session.", userId);
                session.setTokenHash(tokenHash);
                session.setLastSeen(LocalDateTime.now());
                sessionRepository.save(session);
            } else {
                session.setLastSeen(LocalDateTime.now());
                sessionRepository.save(session);
            }
            return true;
        }
        
        // Create new session
        UserSession newSession = UserSession.builder()
            .userId(userId)
            .tokenHash(tokenHash)
            .lastSeen(LocalDateTime.now())
            .build();
        
        sessionRepository.save(newSession);
        log.info("New session registered for user: {}", userId);
        return true;
    }
    
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void cleanupOldSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        int deleted = sessionRepository.deleteOldSessions(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} old sessions", deleted);
        }
    }
}