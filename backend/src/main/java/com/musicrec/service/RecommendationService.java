package com.musicrec.service;

import com.musicrec.dto.RecommendationResponse;
import com.musicrec.dto.TrackDto;
import com.musicrec.entity.ExpandedTrack;
import com.musicrec.entity.Feedback;
import com.musicrec.repository.ExpandedTrackRepository;
import com.musicrec.repository.FeedbackRepository;
import com.musicrec.repository.LikedTrackRepository;
import com.musicrec.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.type.StructType;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    
    private final ExpandedTrackRepository expandedTrackRepository;
    private final FeedbackRepository feedbackRepository;
    private final LikedTrackRepository likedTrackRepository;
    
    @Value("${app.recommendation.ml-threshold-liked-songs}")
    private int mlThresholdLikedSongs;
    
    private final Map<String, List<TrackDto>> recommendationCache = new HashMap<>();
    private final Map<String, Integer> cacheIndex = new HashMap<>();
    private final Map<String, String> cacheMode = new HashMap<>();
    
    @Cacheable(value = "recommendations", key = "#userId")
    public RecommendationResponse generateRecommendations(String userId) {
        log.info("Generating recommendations for user: {}", userId);
        
        List<ExpandedTrack> expandedTracks = expandedTrackRepository.findByUserId(userId);
        
        if (expandedTracks.isEmpty()) {
            log.info("No expanded tracks for user {}", userId);
            return RecommendationResponse.builder()
                .recommendations(Collections.emptyList())
                .mode("Random")
                .totalAvailable(0)
                .status("success")
                .build();
        }
        
        Long likedCount = likedTrackRepository.countByUserId(userId);
        
        if (likedCount < mlThresholdLikedSongs) {
            return generateRandomRecommendations(userId, expandedTracks);
        } else {
            return generateMLRecommendations(userId, expandedTracks);
        }
    }
    
    private RecommendationResponse generateRandomRecommendations(String userId, List<ExpandedTrack> tracks) {
        log.info("Using Random mode for user {} (liked: {})", userId, 
                 likedTrackRepository.countByUserId(userId));
        
        List<TrackDto> shuffled = tracks.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        Collections.shuffle(shuffled);
        
        recommendationCache.put(userId, shuffled);
        cacheIndex.put(userId, 10);
        cacheMode.put(userId, "Random");
        
        List<TrackDto> firstBatch = shuffled.stream()
            .limit(10)
            .collect(Collectors.toList());
        
        return RecommendationResponse.builder()
            .recommendations(firstBatch)
            .mode("Random")
            .totalAvailable(shuffled.size())
            .status("success")
            .build();
    }
    
    private RecommendationResponse generateMLRecommendations(String userId, List<ExpandedTrack> tracks) {
        log.info("Using ML mode for user {} (liked: {})", userId, 
                 likedTrackRepository.countByUserId(userId));
        
        try {
            List<Feedback> feedback = feedbackRepository.findByUserId(userId);
            
            if (feedback.isEmpty() || 
                feedback.stream().map(Feedback::getLiked).distinct().count() < 2) {
                log.info("Insufficient feedback diversity, falling back to Random mode");
                return generateRandomRecommendations(userId, tracks);
            }
            
            // Build encoders
            Map<String, Integer> artistEncoder = new HashMap<>();
            Map<String, Integer> tagsEncoder = new HashMap<>();
            int artistIdCounter = 0;
            int tagsIdCounter = 0;
            
            for (ExpandedTrack track : tracks) {
                String artist = StringUtil.normalize(track.getArtist());
                if (!artist.isEmpty() && !artistEncoder.containsKey(artist)) {
                    artistEncoder.put(artist, artistIdCounter++);
                }
                
                String tags = StringUtil.normalize(track.getTags() != null ? track.getTags() : "None");
                if (!tagsEncoder.containsKey(tags)) {
                    tagsEncoder.put(tags, tagsIdCounter++);
                }
            }
            
            // Prepare training data
            List<double[]> trainingFeatures = new ArrayList<>();
            List<Integer> trainingLabels = new ArrayList<>();
            
            Map<String, Boolean> feedbackMap = feedback.stream()
                .collect(Collectors.toMap(
                    fb -> StringUtil.normalize(fb.getTrackName()) + "|" + StringUtil.normalize(fb.getArtist()),
                    Feedback::getLiked
                ));
            
            for (ExpandedTrack track : tracks) {
                String artist = StringUtil.normalize(track.getArtist());
                String tags = StringUtil.normalize(track.getTags() != null ? track.getTags() : "None");
                String key = StringUtil.normalize(track.getTrackName()) + "|" + artist;
                
                int artistId = artistEncoder.getOrDefault(artist, 0);
                int tagsId = tagsEncoder.getOrDefault(tags, 0);
                int label = feedbackMap.getOrDefault(key, false) ? 1 : 0;
                
                trainingFeatures.add(new double[]{artistId, tagsId});
                trainingLabels.add(label);
            }
            
            // Convert to Smile DataFrame
            double[][] X = trainingFeatures.toArray(new double[0][]);
            int[] y = trainingLabels.stream().mapToInt(Integer::intValue).toArray();
            
            DataFrame df = DataFrame.of(X, "artist", "tags")
                .merge(smile.data.vector.IntVector.of("label", y));
            
            log.info("Training ML model with {} samples", X.length);
            
            // Train model
            RandomForest model = RandomForest.fit(
                smile.data.formula.Formula.lhs("label"),
                df
            );
            
            // Get schema for creating Tuples
            StructType schema = df.schema();
            
            // Make predictions
            List<TrackDto> rankedTracks = new ArrayList<>();
            Set<String> feedbackKeys = feedbackMap.keySet();
            
            for (ExpandedTrack track : tracks) {
                String artist = StringUtil.normalize(track.getArtist());
                String tags = StringUtil.normalize(track.getTags() != null ? track.getTags() : "None");
                String key = StringUtil.normalize(track.getTrackName()) + "|" + artist;
                
                // Skip already feedbacked tracks
                if (feedbackKeys.contains(key)) {
                    continue;
                }
                
                int artistId = artistEncoder.getOrDefault(artist, 0);
                int tagsId = tagsEncoder.getOrDefault(tags, 0);
                
                // Create Tuple for prediction
                double[] featureArray = new double[]{artistId, tagsId};
                Tuple tuple = Tuple.of(featureArray, schema);
                
                // Predict
                int prediction = model.predict(tuple);
                double likeProb = prediction == 1 ? 0.7 : 0.3;
                
                TrackDto dto = convertToDto(track);
                dto.setLikeProb(likeProb);
                rankedTracks.add(dto);
            }
            
            // Sort by probability descending
            rankedTracks.sort((a, b) -> Double.compare(
                b.getLikeProb() != null ? b.getLikeProb() : 0.0,
                a.getLikeProb() != null ? a.getLikeProb() : 0.0
            ));
            
            log.info("Generated {} ML-ranked recommendations", rankedTracks.size());
            
            // Cache results
            recommendationCache.put(userId, rankedTracks);
            cacheIndex.put(userId, 10);
            cacheMode.put(userId, "ML");
            
            List<TrackDto> firstBatch = rankedTracks.stream()
                .limit(10)
                .collect(Collectors.toList());
            
            return RecommendationResponse.builder()
                .recommendations(firstBatch)
                .mode("ML")
                .totalAvailable(rankedTracks.size())
                .status("success")
                .build();
            
        } catch (Exception e) {
            log.error("ML recommendation failed, falling back to Random: {}", e.getMessage(), e);
            return generateRandomRecommendations(userId, tracks);
        }
    }
    
    public RecommendationResponse getNextBatch(String userId) {
        List<TrackDto> cached = recommendationCache.get(userId);
        if (cached == null || cached.isEmpty()) {
            return generateRecommendations(userId);
        }
        
        Integer currentIndex = cacheIndex.getOrDefault(userId, 0);
        int start = currentIndex;
        int end = Math.min(start + 10, cached.size());
        
        if (start >= cached.size()) {
            start = 0;
            end = Math.min(10, cached.size());
        }
        
        List<TrackDto> batch = cached.subList(start, end);
        cacheIndex.put(userId, end);
        
        String mode = cacheMode.getOrDefault(userId, "Random");
        
        return RecommendationResponse.builder()
            .recommendations(batch)
            .mode(mode)
            .totalAvailable(cached.size())
            .status("success")
            .build();
    }
    
    public RecommendationResponse getPreviousBatch(String userId) {
        List<TrackDto> cached = recommendationCache.get(userId);
        if (cached == null || cached.isEmpty()) {
            return generateRecommendations(userId);
        }
        
        Integer currentIndex = cacheIndex.getOrDefault(userId, 10);
        int end = Math.max(10, currentIndex - 10);
        int start = Math.max(0, end - 10);
        
        List<TrackDto> batch = cached.subList(start, end);
        cacheIndex.put(userId, start + batch.size());
        
        String mode = cacheMode.getOrDefault(userId, "Random");
        
        return RecommendationResponse.builder()
            .recommendations(batch)
            .mode(mode)
            .totalAvailable(cached.size())
            .status("success")
            .build();
    }
    
    @CacheEvict(value = "recommendations", key = "#userId")
    public void clearCache(String userId) {
        recommendationCache.remove(userId);
        cacheIndex.remove(userId);
        cacheMode.remove(userId);
        log.info("Cleared recommendation cache for user: {}", userId);
    }
    
    private TrackDto convertToDto(ExpandedTrack track) {
        return TrackDto.builder()
            .id(track.getId())
            .trackName(track.getTrackName())
            .artist(track.getArtist())
            .album(track.getAlbum())
            .year(track.getReleaseYear())  // Map releaseYear to year for frontend
            .source(track.getSource())
            .tags(track.getTags())
            .artistSeed(track.getArtistSeed())
            .genreSeed(track.getGenreSeed())
            .spotifyId(track.getSpotifyId())
            .albumImage(track.getAlbumImage())
            .build();
    }
}