package com.musicrec.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple session manager to store user access tokens
 * In production, consider using Redis or a proper session store
 */
@Component
@Slf4j
public class SessionManager {
    
    // Store access tokens by user ID
    private final Map<String, String> userAccessTokens = new ConcurrentHashMap<>();
    
    // Store refresh tokens by user ID
    private final Map<String, String> userRefreshTokens = new ConcurrentHashMap<>();
    
    /**
     * Store access token for a user
     */
    public void setAccessToken(String userId, String accessToken) {
        userAccessTokens.put(userId, accessToken);
        log.debug("Stored access token for user: {}", userId);
    }
    
    /**
     * Get access token for a user
     */
    public String getAccessToken(String userId) {
        String token = userAccessTokens.get(userId);
        if (token == null) {
            log.warn("No access token found for user: {}", userId);
            throw new IllegalStateException("No Spotify access token found. Please log in again.");
        }
        return token;
    }
    
    /**
     * Store refresh token for a user
     */
    public void setRefreshToken(String userId, String refreshToken) {
        userRefreshTokens.put(userId, refreshToken);
        log.debug("Stored refresh token for user: {}", userId);
    }
    
    /**
     * Get refresh token for a user
     */
    public String getRefreshToken(String userId) {
        return userRefreshTokens.get(userId);
    }
    
    /**
     * Store both tokens at once
     */
    public void setTokens(String userId, String accessToken, String refreshToken) {
        setAccessToken(userId, accessToken);
        if (refreshToken != null) {
            setRefreshToken(userId, refreshToken);
        }
    }
    
    /**
     * Check if user has a valid access token
     */
    public boolean hasAccessToken(String userId) {
        return userAccessTokens.containsKey(userId);
    }
    
    /**
     * Clear all tokens for a user (on logout)
     */
    public void clearSession(String userId) {
        userAccessTokens.remove(userId);
        userRefreshTokens.remove(userId);
        log.info("Cleared session for user: {}", userId);
    }
    
    /**
     * Clear all sessions (for testing/admin purposes)
     */
    public void clearAllSessions() {
        userAccessTokens.clear();
        userRefreshTokens.clear();
        log.warn("Cleared all sessions");
    }
}