package com.musicrec.service;

import com.musicrec.dto.*;
import com.musicrec.entity.Track;
import com.musicrec.entity.Feedback;
import com.musicrec.repository.TrackRepository;
import com.musicrec.repository.FeedbackRepository;
import com.musicrec.util.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicService {
    
    private final SpotifyService spotifyService;
    private final TrackRepository trackRepository;
    private final FeedbackRepository feedbackRepository;
    
    // OPTION 1: If you have SessionManager
    private final SessionManager sessionManager;
    
    // OPTION 2: If you DON'T have SessionManager, remove the line above and use the method below
    // You'll need to pass the access token from your authentication system
    
    /**
     * FIXED: Only fetches and stores Spotify liked tracks.
     * No Last.fm calls, no genre fetching - super fast!
     */
    @Transactional
    public ExpandResponse expandDataset(String userId) {
        log.info("Starting dataset expansion for user: {}", userId);
        
        try {
            // OPTION 1: Using SessionManager (if you have it)
            String accessToken = sessionManager.getAccessToken(userId);
            
            // OPTION 2: If you DON'T have SessionManager, get token from your auth system:
            // String accessToken = yourAuthService.getTokenForUser(userId);
            // OR if token is stored elsewhere:
            // String accessToken = userRepository.findById(userId).orElseThrow().getSpotifyAccessToken();
            
            if (accessToken == null || accessToken.isEmpty()) {
                return ExpandResponse.builder()
                    .success(false)
                    .totalTracks(0)
                    .message("No valid Spotify access token found. Please log in again.")
                    .build();
            }
            
            // Fetch ALL liked tracks from Spotify (NO genre calls - fast!)
            List<Map<String, Object>> spotifyTracks = spotifyService.getAllLikedTracks(accessToken);
            log.info("✅ Fetched {} tracks from Spotify", spotifyTracks.size());
            
            if (spotifyTracks.isEmpty()) {
                return ExpandResponse.builder()
                    .success(true)
                    .totalTracks(0)
                    .message("No liked tracks found in your Spotify account. Like some songs and try again!")
                    .build();
            }
            
            // Delete old liked tracks for this user to avoid duplicates
            trackRepository.deleteByUserIdAndSource(userId, "spotify_liked");
            log.info("Cleared old liked tracks for user: {}", userId);
            
            // Save new tracks to database
            int savedCount = 0;
            for (Map<String, Object> trackData : spotifyTracks) {
                try {
                    Track track = new Track();
                    track.setUserId(userId);
                    track.setSpotifyId((String) trackData.get("spotifyId"));
                    track.setTrackName((String) trackData.get("trackName"));
                    track.setArtist((String) trackData.get("artist"));
                    track.setArtistId((String) trackData.get("artistId")); // Store for later genre fetching
                    track.setAlbum((String) trackData.get("album"));
                    track.setYear((String) trackData.get("year"));
                    track.setAlbumImage((String) trackData.get("albumImage"));
                    track.setTags(""); // Empty for now, will be filled during recommendation generation
                    track.setSource("spotify_liked");
                    track.setCreatedAt(LocalDateTime.now());
                    
                    trackRepository.save(track);
                    savedCount++;
                } catch (Exception e) {
                    log.warn("Failed to save track: {}", e.getMessage());
                }
            }
            
            log.info("✅ Saved {} tracks to database", savedCount);
            
            // Return success (NO Last.fm calls - that happens during recommendation generation!)
            return ExpandResponse.builder()
                    .success(true)
                    .totalTracks(savedCount)
                    .message(String.format("✅ Successfully fetched and stored %d liked tracks from Spotify! Click 'Generate Recommendations' to get started.", savedCount))
                    .build();
                    
        } catch (IllegalStateException e) {
            log.error("Session error: {}", e.getMessage());
            return ExpandResponse.builder()
                    .success(false)
                    .totalTracks(0)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error during dataset expansion: {}", e.getMessage(), e);
            return ExpandResponse.builder()
                    .success(false)
                    .totalTracks(0)
                    .message("Failed to fetch tracks: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Get statistics about user's music data
     */
    public StatsResponse getStats(String userId) {
        try {
            // Count liked tracks
            long likedTracksCount = trackRepository.countByUserIdAndSource(userId, "spotify_liked");
            
            // Count recommended tracks
            long recommendedTracksCount = trackRepository.countByUserIdAndSourceNot(userId, "spotify_liked");
            
            // Get unique artists
            List<String> uniqueArtists = trackRepository.findDistinctArtistsByUserId(userId);
            
            // Get feedback stats
            long likedRecommendations = feedbackRepository.countByUserIdAndFeedbackType(userId, "like");
            long dislikedRecommendations = feedbackRepository.countByUserIdAndFeedbackType(userId, "dislike");
            
            return StatsResponse.builder()
                    .success(true)
                    .likedTracksCount(likedTracksCount)
                    .recommendedTracksCount(recommendedTracksCount)
                    .uniqueArtistsCount(uniqueArtists.size())
                    .likedRecommendations(likedRecommendations)
                    .dislikedRecommendations(dislikedRecommendations)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting stats: {}", e.getMessage());
            return StatsResponse.builder()
                    .success(false)
                    .likedTracksCount(0)
                    .recommendedTracksCount(0)
                    .uniqueArtistsCount(0)
                    .likedRecommendations(0)
                    .dislikedRecommendations(0)
                    .build();
        }
    }
    
    /**
     * Save user feedback for a recommendation
     */
    @Transactional
    public ApiResponse saveFeedback(String userId, FeedbackRequest request) {
        try {
            // Validate input
            if (request.getTrackId() == null) {
                return ApiResponse.builder()
                        .success(false)
                        .message("Track ID is required")
                        .build();
            }
            
            if (request.getFeedbackType() == null || request.getFeedbackType().isEmpty()) {
                return ApiResponse.builder()
                        .success(false)
                        .message("Feedback type is required")
                        .build();
            }
            
            // Find the track
            Optional<Track> trackOpt = trackRepository.findById(request.getTrackId());
            
            if (trackOpt.isEmpty()) {
                return ApiResponse.builder()
                        .success(false)
                        .message("Track not found")
                        .build();
            }
            
            Track track = trackOpt.get();
            
            // Check if feedback already exists
            Optional<Feedback> existingFeedback = feedbackRepository
                    .findByUserIdAndTrackId(userId, request.getTrackId());
            
            Feedback feedback;
            if (existingFeedback.isPresent()) {
                // Update existing feedback
                feedback = existingFeedback.get();
                feedback.setFeedbackType(request.getFeedbackType());
                feedback.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new feedback
                feedback = new Feedback();
                feedback.setUserId(userId);
                feedback.setTrackId(request.getTrackId());
                feedback.setFeedbackType(request.getFeedbackType());
                feedback.setCreatedAt(LocalDateTime.now());
                feedback.setUpdatedAt(LocalDateTime.now());
            }
            
            feedbackRepository.save(feedback);
            
            log.info("Saved feedback for user {}: {} on track '{}'", 
                    userId, request.getFeedbackType(), track.getTrackName());
            
            return ApiResponse.builder()
                    .success(true)
                    .message("Feedback saved successfully")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error saving feedback: {}", e.getMessage());
            return ApiResponse.builder()
                    .success(false)
                    .message("Failed to save feedback: " + e.getMessage())
                    .build();
        }
    }
}