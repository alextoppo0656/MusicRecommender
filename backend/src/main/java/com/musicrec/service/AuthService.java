package com.musicrec.service;

import com.musicrec.dto.AuthResponse;
import com.musicrec.entity.User;
import com.musicrec.repository.UserRepository;
import com.musicrec.security.JwtUtil;
import com.musicrec.util.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final SpotifyService spotifyService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final SessionManager sessionManager;
    
    @Transactional
    public AuthResponse authenticateWithSpotify(String code, String redirectUri) {
        // Exchange code for Spotify tokens
        Map<String, Object> tokenResponse = spotifyService.exchangeCodeForToken(code, redirectUri);
        
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");
        
        // Get user info from Spotify
        Map<String, Object> userInfo = spotifyService.getCurrentUser(accessToken);
        String spotifyId = (String) userInfo.get("id");
        String displayName = (String) userInfo.get("display_name");
        String email = (String) userInfo.get("email");
        
        // Save or update user
        User user = userRepository.findBySpotifyId(spotifyId)
            .map(existingUser -> {
                existingUser.setAccessToken(accessToken);
                existingUser.setRefreshToken(refreshToken);
                existingUser.setTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
                existingUser.setLastLogin(LocalDateTime.now());
                existingUser.setLoginCount(existingUser.getLoginCount() + 1);
                existingUser.setDisplayName(displayName);
                existingUser.setEmail(email);
                return existingUser;
            })
            .orElseGet(() -> User.builder()
                .spotifyId(spotifyId)
                .displayName(displayName)
                .email(email)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiry(LocalDateTime.now().plusSeconds(expiresIn))
                .lastLogin(LocalDateTime.now())
                .loginCount(1)
                .build());
        
        userRepository.save(user);
        
        // Create session
        sessionManager.validateSession(spotifyId, accessToken);
        
        // Generate JWT
        String jwtToken = jwtUtil.generateToken(spotifyId);
        
        log.info("User authenticated: {} ({})", displayName, spotifyId);
        
        return AuthResponse.builder()
            .accessToken(jwtToken)
            .tokenType("Bearer")
            .expiresIn((long) expiresIn)
            .userInfo(AuthResponse.UserInfo.builder()
                .userId(spotifyId)
                .displayName(displayName)
                .email(email)
                .build())
            .build();
    }
    
    public User getUserBySpotifyId(String spotifyId) {
        return userRepository.findBySpotifyId(spotifyId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}