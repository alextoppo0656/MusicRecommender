package com.musicrec.service;

import com.musicrec.dto.*;
import com.musicrec.entity.Track;
import com.musicrec.repository.TrackRepository;
import com.musicrec.util.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    
    private final TrackRepository trackRepository;
    private final SpotifyService spotifyService;
    private final LastFmService lastFmService;
    
    // OPTION 1: If you have SessionManager
    private final SessionManager sessionManager;
    
    // OPTION 2: If you DON'T have SessionManager, remove the line above
    // and get access token differently (see comments in methods below)
    
    // Track which batch each user is currently viewing
    private final Map<String, Integer> userBatchIndex = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int BATCH_SIZE = 30; // Process 30 liked songs at a time
    private static final int MAX_ARTISTS_TO_PROCESS = 10; // Process top 10 artists from batch
    private static final int SIMILAR_ARTISTS_PER_ARTIST = 2; // Get 2 similar artists per artist
    private static final int TARGET_RECOMMENDATIONS = 50; // Try to get ~50 recommendations
    
    /**
     * Generate recommendations - starts from batch 1
     */
    @Transactional
    public RecommendationResponse generateRecommendations(String userId) {
        log.info("🎵 Generating recommendations for user: {}", userId);
        
        // Reset to first batch
        userBatchIndex.put(userId, 0);
        
        // Get recommendations for first batch
        return getNextBatch(userId);
    }
    
    /**
     * Get next batch of recommendations
     */
    @Transactional
    public RecommendationResponse getNextBatch(String userId) {
        int currentBatch = userBatchIndex.getOrDefault(userId, 0);
        
        log.info("📦 Getting batch {} for user: {}", currentBatch + 1, userId);
        
        try {
            // 1. Get ALL liked tracks from database
            List<Track> allLikedTracks = trackRepository.findByUserIdAndSource(userId, "spotify_liked");
            
            if (allLikedTracks.isEmpty()) {
                throw new IllegalStateException("No liked tracks found. Please click 'Expand Dataset' first!");
            }
            
            // 2. Calculate batch boundaries
            int startIdx = currentBatch * BATCH_SIZE;
            int endIdx = Math.min(startIdx + BATCH_SIZE, allLikedTracks.size());
            
            // Check if we've gone past all tracks
            if (startIdx >= allLikedTracks.size()) {
                throw new IllegalStateException("No more tracks to process. You've gone through all your liked songs! Click 'Generate' to start over.");
            }
            
            // 3. Get current batch of tracks
            List<Track> batchTracks = allLikedTracks.subList(startIdx, endIdx);
            log.info("📊 Processing batch {} - tracks {} to {} (total: {})", 
                    currentBatch + 1, startIdx + 1, endIdx, batchTracks.size());
            
            // 4. Get access token
            // OPTION 1: Using SessionManager (if you have it)
            String accessToken = sessionManager.getAccessToken(userId);
            
            // OPTION 2: If you DON'T have SessionManager:
            // String accessToken = yourAuthService.getTokenForUser(userId);
            // OR
            // String accessToken = userRepository.findById(userId).orElseThrow().getSpotifyAccessToken();
            
            // 5. Fetch genres for this batch (if not already stored)
            fetchAndUpdateGenres(batchTracks, accessToken);
            
            // 6. Generate recommendations using Last.fm
            List<Track> recommendations = generateLastFmRecommendations(userId, batchTracks);
            
            log.info("✅ Generated {} recommendations for batch {}", recommendations.size(), currentBatch + 1);
            
            // 7. Update batch counter for next time
            userBatchIndex.put(userId, currentBatch + 1);
            
            // 8. Calculate batch info
            int totalBatches = (int) Math.ceil((double) allLikedTracks.size() / BATCH_SIZE);
            boolean hasMore = endIdx < allLikedTracks.size();
            boolean hasPrevious = currentBatch > 0;
            
            // 9. Build response
            return RecommendationResponse.builder()
                    .success(true)
                    .likedTracks(convertToDto(batchTracks))
                    .recommendations(convertToDto(recommendations))
                    .currentBatch(currentBatch + 1)
                    .totalBatches(totalBatches)
                    .hasMore(hasMore)
                    .hasPrevious(hasPrevious)
                    .message(String.format("📦 Showing batch %d of %d - Based on %d of your %d liked songs", 
                            currentBatch + 1, totalBatches, batchTracks.size(), allLikedTracks.size()))
                    .build();
                    
        } catch (IllegalStateException e) {
            log.warn("Batch processing issue: {}", e.getMessage());
            return RecommendationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .likedTracks(Collections.emptyList())
                    .recommendations(Collections.emptyList())
                    .currentBatch(currentBatch + 1)
                    .totalBatches(0)
                    .hasMore(false)
                    .hasPrevious(currentBatch > 0)
                    .build();
        } catch (Exception e) {
            log.error("Error generating recommendations: {}", e.getMessage(), e);
            return RecommendationResponse.builder()
                    .success(false)
                    .message("Failed to generate recommendations: " + e.getMessage())
                    .likedTracks(Collections.emptyList())
                    .recommendations(Collections.emptyList())
                    .currentBatch(currentBatch + 1)
                    .totalBatches(0)
                    .hasMore(false)
                    .hasPrevious(currentBatch > 0)
                    .build();
        }
    }
    
    /**
     * Get previous batch of recommendations
     */
    public RecommendationResponse getPreviousBatch(String userId) {
        int currentBatch = userBatchIndex.getOrDefault(userId, 0);
        
        if (currentBatch > 0) {
            userBatchIndex.put(userId, currentBatch - 1);
            log.info("⬅️ Moving to previous batch for user: {}", userId);
        } else {
            log.info("Already at first batch for user: {}", userId);
        }
        
        return getNextBatch(userId);
    }
    
    /**
     * Clear recommendation cache (called after feedback or logout)
     */
    public void clearCache(String userId) {
        userBatchIndex.remove(userId);
        log.info("🗑️ Cleared recommendation cache for user: {}", userId);
    }
    
    /**
     * Fetch and update genres for a batch of tracks
     */
    private void fetchAndUpdateGenres(List<Track> tracks, String accessToken) {
        // Get unique artist IDs that don't have genres yet
        List<String> artistIds = tracks.stream()
                .filter(t -> t.getArtistId() != null && !t.getArtistId().isEmpty())
                .filter(t -> t.getTags() == null || t.getTags().isEmpty())
                .map(Track::getArtistId)
                .distinct()
                .collect(Collectors.toList());
        
        if (artistIds.isEmpty()) {
            log.info("All tracks in batch already have genres");
            return;
        }
        
        log.info("🎸 Fetching genres for {} artists...", artistIds.size());
        
        try {
            // Fetch genres in batch
            Map<String, List<String>> genresMap = spotifyService.getGenresForArtists(artistIds, accessToken);
            
            // Update tracks with genres
            for (Track track : tracks) {
                if (track.getArtistId() != null && genresMap.containsKey(track.getArtistId())) {
                    List<String> genres = genresMap.get(track.getArtistId());
                    if (genres != null && !genres.isEmpty()) {
                        track.setTags(String.join(", ", genres));
                        trackRepository.save(track);
                    }
                }
            }
            
            log.info("✅ Updated genres for {} tracks", tracks.size());
            
        } catch (Exception e) {
            log.warn("Failed to fetch genres: {}", e.getMessage());
            // Continue without genres - not critical
        }
    }
    
    /**
     * Generate recommendations using Last.fm API
     */
    @Transactional
    private List<Track> generateLastFmRecommendations(String userId, List<Track> batchTracks) {
        log.info("🎵 Generating Last.fm recommendations...");
        
        List<Track> allRecommendations = new ArrayList<>();
        Set<String> processedArtists = new HashSet<>();
        Set<String> seenTracks = new HashSet<>(); // Avoid duplicates
        
        // Take first N artists from the batch
        int artistsToProcess = Math.min(MAX_ARTISTS_TO_PROCESS, batchTracks.size());
        
        for (int i = 0; i < artistsToProcess && allRecommendations.size() < TARGET_RECOMMENDATIONS; i++) {
            Track seedTrack = batchTracks.get(i);
            String seedArtist = seedTrack.getArtist();
            
            // Skip if we've already processed this artist
            if (processedArtists.contains(seedArtist.toLowerCase())) {
                continue;
            }
            
            processedArtists.add(seedArtist.toLowerCase());
            log.info("🎤 Processing artist: {}", seedArtist);
            
            try {
                // 1. Get similar artists from Last.fm
                List<String> similarArtists = lastFmService.getSimilarArtists(seedArtist);
                
                if (similarArtists.isEmpty()) {
                    log.debug("No similar artists found for: {}", seedArtist);
                    continue;
                }
                
                log.info("Found {} similar artists for {}", similarArtists.size(), seedArtist);
                
                // 2. Get top tracks from similar artists
                int artistsProcessed = 0;
                for (String similarArtist : similarArtists) {
                    if (artistsProcessed >= SIMILAR_ARTISTS_PER_ARTIST) {
                        break;
                    }
                    
                    if (allRecommendations.size() >= TARGET_RECOMMENDATIONS) {
                        break;
                    }
                    
                    try {
                        List<Map<String, String>> topTracks = lastFmService.getTopTracksForArtist(similarArtist, seedArtist);
                        
                        for (Map<String, String> trackData : topTracks) {
                            String trackKey = (trackData.get("artist") + " - " + trackData.get("trackName")).toLowerCase();
                            
                            // Skip duplicates
                            if (seenTracks.contains(trackKey)) {
                                continue;
                            }
                            
                            seenTracks.add(trackKey);
                            
                            // Create and save recommendation
                            Track recommendation = new Track();
                            recommendation.setUserId(userId);
                            recommendation.setTrackName(trackData.get("trackName"));
                            recommendation.setArtist(trackData.get("artist"));
                            recommendation.setSource(trackData.get("source"));
                            recommendation.setArtistSeed(trackData.get("artistSeed"));
                            recommendation.setTags(trackData.getOrDefault("tags", ""));
                            recommendation.setCreatedAt(LocalDateTime.now());
                            
                            Track saved = trackRepository.save(recommendation);
                            allRecommendations.add(saved);
                            
                            if (allRecommendations.size() >= TARGET_RECOMMENDATIONS) {
                                break;
                            }
                        }
                        
                        artistsProcessed++;
                        
                    } catch (Exception e) {
                        log.warn("Failed to get tracks for {}: {}", similarArtist, e.getMessage());
                    }
                }
                
                // 3. Also try genre-based recommendations if we have tags
                if (seedTrack.getTags() != null && !seedTrack.getTags().isEmpty() 
                        && allRecommendations.size() < TARGET_RECOMMENDATIONS) {
                    
                    String[] tags = seedTrack.getTags().split(",");
                    if (tags.length > 0) {
                        String primaryTag = tags[0].trim();
                        
                        try {
                            List<Map<String, String>> genreTracks = lastFmService.getTopTracksForTag(primaryTag, primaryTag);
                            
                            for (Map<String, String> trackData : genreTracks) {
                                String trackKey = (trackData.get("artist") + " - " + trackData.get("trackName")).toLowerCase();
                                
                                if (seenTracks.contains(trackKey)) {
                                    continue;
                                }
                                
                                seenTracks.add(trackKey);
                                
                                Track recommendation = new Track();
                                recommendation.setUserId(userId);
                                recommendation.setTrackName(trackData.get("trackName"));
                                recommendation.setArtist(trackData.get("artist"));
                                recommendation.setSource(trackData.get("source"));
                                recommendation.setTags(trackData.getOrDefault("tags", ""));
                                recommendation.setGenreSeed(trackData.get("genreSeed"));
                                recommendation.setCreatedAt(LocalDateTime.now());
                                
                                Track saved = trackRepository.save(recommendation);
                                allRecommendations.add(saved);
                                
                                if (allRecommendations.size() >= TARGET_RECOMMENDATIONS) {
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get genre tracks for {}: {}", primaryTag, e.getMessage());
                        }
                    }
                }
                
            } catch (Exception e) {
                log.warn("Error processing artist {}: {}", seedArtist, e.getMessage());
            }
        }
        
        log.info("✅ Generated {} total recommendations", allRecommendations.size());
        return allRecommendations;
    }
    
    /**
     * Convert Track entities to DTOs
     */
    private List<TrackDto> convertToDto(List<Track> tracks) {
        return tracks.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert single Track to DTO
     */
    private TrackDto toDto(Track track) {
        return TrackDto.builder()
                .id(track.getId())
                .trackName(track.getTrackName())
                .artist(track.getArtist())
                .album(track.getAlbum())
                .year(track.getYear())
                .tags(track.getTags())
                .albumImage(track.getAlbumImage())
                .source(track.getSource())
                .artistSeed(track.getArtistSeed())
                .genreSeed(track.getGenreSeed())
                .spotifyId(track.getSpotifyId())
                .build();
    }
}