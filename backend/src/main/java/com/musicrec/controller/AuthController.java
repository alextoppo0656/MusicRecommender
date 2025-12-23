package com.musicrec.controller;

import com.musicrec.dto.ApiResponse;
import com.musicrec.dto.AuthRequest;
import com.musicrec.dto.AuthResponse;
import com.musicrec.service.AuthService;
import com.musicrec.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final RecommendationService recommendationService;
    
    @PostMapping("/callback")
    public ResponseEntity<AuthResponse> handleCallback(@Valid @RequestBody AuthRequest request) {
        log.info("Auth callback received");
        
        String redirectUri = request.getRedirectUri() != null ? 
            request.getRedirectUri() : "http://localhost:3000/callback";
        
        AuthResponse response = authService.authenticateWithSpotify(request.getCode(), redirectUri);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(Authentication authentication) {
        if (authentication != null) {
            String userId = (String) authentication.getPrincipal();
            recommendationService.clearCache(userId);
            log.info("User logged out: {}", userId);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}