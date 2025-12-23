package com.musicrec.service;

import com.musicrec.dto.*;
import com.musicrec.entity.*;
import com.musicrec.repository.*;
import com.musicrec.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicService {
    
    private final SpotifyService spotifyService;
    private final LastFmService lastFmService;
    private final ExpandedTrackRepository expandedTrackRepository;
    private final LikedTrackRepository likedTrackRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    
    @Value("${app.recommendation.max-expand-per-call}")
    private int maxExpandPerCall;
    
    @Transactional
    public ExpandResponse expandDataset(String userId) {
        log.info("Starting dataset expansion for user: {}", userId);
        
        User user = userRepository.findBySpotifyId(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 1. Fetch ALL Spotify liked tracks (paginated)
        List<Map<String, Object>> spotifyTracks = spotifyService.getAllLikedTracks(user.getAccessToken());
        log.info("Fetched {} tracks from Spotify", spotifyTracks.size());
        
        // 2. Import Spotify liked tracks into liked_tracks table
        int likedImported = 0;
        for (Map<String, Object> track : spotifyTracks) {
            try {
                saveLikedTrack(userId, track);
                likedImported++;
            } catch (Exception e) {
                log.warn("Failed to save liked track: {}", e.getMessage());
            }
        }
        
        // 3. Import feedback liked tracks into liked_tracks table
        List<Feedback> feedbackLiked = feedbackRepository.findByUserIdAndLiked(userId, true);
        for (Feedback fb : feedbackLiked) {
            try {
                Map<String, Object> fbTrack = new HashMap<>();
                fbTrack.put("trackName", fb.getTrackName());
                fbTrack.put("artist", fb.getArtist());
                fbTrack.put("album", fb.getAlbum());
                fbTrack.put("year", fb.getReleaseYear());
                fbTrack.put("spotifyId", fb.getSpotifyId());
                saveLikedTrack(userId, fbTrack);
            } catch (Exception e) {
                log.warn("Failed to save feedback track: {}", e.getMessage());
            }
        }
        
        // 4. Copy ALL liked tracks to expanded dataset
        List<LikedTrack> allLiked = likedTrackRepository.findByUserId(userId);
        for (LikedTrack liked : allLiked) {
            try {
                if (!expandedTrackRepository.existsByUserIdAndTrackNameAndArtist(
                        userId, liked.getTrackName(), liked.getArtist())) {
                    
                    ExpandedTrack expanded = ExpandedTrack.builder()
                        .userId(userId)
                        .trackName(liked.getTrackName())
                        .artist(liked.getArtist())
                        .album(liked.getAlbum())
                        .releaseYear(liked.getReleaseYear())
                        .source("spotify_liked")
                        .spotifyId(liked.getSpotifyId())
                        .build();
                    
                    expandedTrackRepository.save(expanded);
                }
            } catch (Exception e) {
                log.warn("Failed to copy liked track to expanded: {}", e.getMessage());
            }
        }
        
        // 5. Get existing expanded tracks to avoid duplicates
        List<ExpandedTrack> existing = expandedTrackRepository.findByUserId(userId);
        Set<String> existingPairs = existing.stream()
            .map(t -> StringUtil.normalize(t.getTrackName()) + "|" + StringUtil.normalize(t.getArtist()))
            .collect(Collectors.toSet());
        
        // 6. Get seed artists
        List<String> seedArtists = likedTrackRepository.findByUserId(userId).stream()
            .map(LikedTrack::getArtist)
            .filter(a -> a != null && !a.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        
        Collections.shuffle(seedArtists);
        log.info("Using {} seed artists for expansion", seedArtists.size());
        
        // 7. Expand using Last.fm
        List<Map<String, String>> newTracks = new ArrayList<>();
        int addedCount = 0;
        
        for (String seedArtist : seedArtists) {
            if (newTracks.size() >= maxExpandPerCall * 2) {
                break; // Collected enough candidates
            }
            
            // Artist similarity expansion
            List<String> similarArtists = lastFmService.getSimilarArtists(seedArtist);
            for (String similarArtist : similarArtists) {
                if (newTracks.size() >= maxExpandPerCall * 2) break;
                
                List<Map<String, String>> tracks = lastFmService.getTopTracksForArtist(similarArtist, seedArtist);
                for (Map<String, String> track : tracks) {
                    String key = StringUtil.normalize(track.get("trackName")) + "|" + 
                                StringUtil.normalize(track.get("artist"));
                    
                    if (!existingPairs.contains(key)) {
                        newTracks.add(track);
                        existingPairs.add(key);
                    }
                }
            }
            
            // Genre similarity expansion
            List<String> tags = lastFmService.getArtistTags(seedArtist);
            for (String tag : tags) {
                if (newTracks.size() >= maxExpandPerCall * 2) break;
                
                List<Map<String, String>> tracks = lastFmService.getTopTracksForTag(tag, tag);
                for (Map<String, String> track : tracks) {
                    String key = StringUtil.normalize(track.get("trackName")) + "|" + 
                                StringUtil.normalize(track.get("artist"));
                    
                    if (!existingPairs.contains(key)) {
                        newTracks.add(track);
                        existingPairs.add(key);
                    }
                }
            }
        }
        
        // 8. Save new expanded tracks (capped at maxExpandPerCall)
        Collections.shuffle(newTracks);
        for (Map<String, String> track : newTracks) {
            if (addedCount >= maxExpandPerCall) {
                break;
            }
            
            try {
                ExpandedTrack expanded = ExpandedTrack.builder()
                    .userId(userId)
                    .trackName(track.get("trackName"))
                    .artist(track.get("artist"))
                    .source(track.get("source"))
                    .tags(track.get("tags"))
                    .artistSeed(track.get("artistSeed"))
                    .genreSeed(track.get("genreSeed"))
                    .build();
                
                expandedTrackRepository.save(expanded);
                addedCount++;
            } catch (Exception e) {
                log.warn("Failed to save expanded track: {}", e.getMessage());
            }
        }
        
        long totalRows = expandedTrackRepository.countByUserId(userId);
        
        log.info("Expansion complete. Added {} tracks from Last.fm. Total: {}", addedCount, totalRows);
        
        return ExpandResponse.builder()
            .expandedAdded(addedCount)
            .likedImported(likedImported)
            .totalRows(totalRows)
            .status("success")
            .message(String.format("Added %d new tracks", addedCount))
            .build();
    }
    
    private void saveLikedTrack(String userId, Map<String, Object> track) {
        String trackName = (String) track.get("trackName");
        String artist = (String) track.get("artist");
        
        if (trackName == null || artist == null || trackName.isEmpty() || artist.isEmpty()) {
            return;
        }
        
        Optional<LikedTrack> existing = likedTrackRepository
            .findByUserIdAndTrackNameAndArtist(userId, trackName, artist);
        
        if (existing.isEmpty()) {
            LikedTrack liked = LikedTrack.builder()
                .userId(userId)
                .trackName(trackName)
                .artist(artist)
                .album((String) track.get("album"))
                .releaseYear((String) track.get("year"))
                .spotifyId((String) track.get("spotifyId"))
                .build();
            
            likedTrackRepository.save(liked);
        }
    }
    
    public StatsResponse getStats(String userId) {
        Long totalSongs = expandedTrackRepository.countByUserId(userId);
        Long totalLiked = likedTrackRepository.countByUserId(userId);
        Long totalFeedback = feedbackRepository.countByUserId(userId);
        Long feedbackLiked = feedbackRepository.countByUserIdAndLiked(userId, true);
        Long feedbackSkipped = feedbackRepository.countByUserIdAndLiked(userId, false);
        
        return StatsResponse.builder()
            .totalSongs(totalSongs)
            .totalLiked(totalLiked)
            .totalFeedback(totalFeedback)
            .feedbackLiked(feedbackLiked)
            .feedbackSkipped(feedbackSkipped)
            .status("success")
            .build();
    }
    
    @Transactional
    public ApiResponse saveFeedback(String userId, FeedbackRequest request) {
        Feedback feedback = feedbackRepository
            .findByUserIdAndTrackNameAndArtist(userId, request.getTrackName(), request.getArtist())
            .map(existing -> {
                existing.setLiked(request.getLiked());
                existing.setAlbum(request.getAlbum());
                existing.setReleaseYear(request.getYear());
                existing.setSpotifyId(request.getSpotifyId());
                return existing;
            })
            .orElseGet(() -> Feedback.builder()
                .userId(userId)
                .trackName(request.getTrackName())
                .artist(request.getArtist())
                .liked(request.getLiked())
                .album(request.getAlbum())
                .releaseYear(request.getYear())
                .spotifyId(request.getSpotifyId())
                .build());
        
        feedbackRepository.save(feedback);
        
        // If liked, also save to liked_tracks
        if (request.getLiked()) {
            Map<String, Object> track = new HashMap<>();
            track.put("trackName", request.getTrackName());
            track.put("artist", request.getArtist());
            track.put("album", request.getAlbum());
            track.put("year", request.getYear());
            track.put("spotifyId", request.getSpotifyId());
            saveLikedTrack(userId, track);
            
            // Also add to expanded if not present
            if (!expandedTrackRepository.existsByUserIdAndTrackNameAndArtist(
                    userId, request.getTrackName(), request.getArtist())) {
                
                ExpandedTrack expanded = ExpandedTrack.builder()
                    .userId(userId)
                    .trackName(request.getTrackName())
                    .artist(request.getArtist())
                    .album(request.getAlbum())
                    .releaseYear(request.getYear())
                    .source("user_liked")
                    .spotifyId(request.getSpotifyId())
                    .build();
                
                expandedTrackRepository.save(expanded);
            }
        }
        
        String action = request.getLiked() ? "liked" : "skipped";
        log.info("Feedback saved for user {}: {} - {}", userId, request.getTrackName(), action);
        
        return ApiResponse.success(String.format("Feedback recorded: %s", action));
    }
}