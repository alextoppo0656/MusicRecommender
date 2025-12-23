package com.musicrec.controller;

import com.musicrec.dto.*;
import com.musicrec.service.MusicService;
import com.musicrec.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MusicController {
    
    private final MusicService musicService;
    private final RecommendationService recommendationService;
    
    @PostMapping("/expand")
    public ResponseEntity<ExpandResponse> expandDataset(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("Expand request from user: {}", userId);
        
        ExpandResponse response = musicService.expandDataset(userId);
        
        // Clear recommendation cache after expansion
        recommendationService.clearCache(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        StatsResponse response = musicService.getStats(userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/recommend")
    public ResponseEntity<RecommendationResponse> getRecommendations(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        log.info("Recommendation request from user: {}", userId);
        
        RecommendationResponse response = recommendationService.generateRecommendations(userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/recommend/next")
    public ResponseEntity<RecommendationResponse> getNextRecommendations(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        RecommendationResponse response = recommendationService.getNextBatch(userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/recommend/previous")
    public ResponseEntity<RecommendationResponse> getPreviousRecommendations(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        RecommendationResponse response = recommendationService.getPreviousBatch(userId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse> submitFeedback(
            Authentication authentication,
            @Valid @RequestBody FeedbackRequest request) {
        
        String userId = (String) authentication.getPrincipal();
        ApiResponse response = musicService.saveFeedback(userId, request);
        
        // Clear cache to trigger re-ranking
        recommendationService.clearCache(userId);
        
        return ResponseEntity.ok(response);
    }
}