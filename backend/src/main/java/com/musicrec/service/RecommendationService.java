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
import smile.data.formula.Formula;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
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
        List<TrackDto> shuffled = tracks.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());

        Collections.shuffle(shuffled);

        recommendationCache.put(userId, shuffled);
        cacheIndex.put(userId, 10);
        cacheMode.put(userId, "Random");

        return RecommendationResponse.builder()
            .recommendations(shuffled.stream().limit(10).collect(Collectors.toList()))
            .mode("Random")
            .totalAvailable(shuffled.size())
            .status("success")
            .build();
    }

    private RecommendationResponse generateMLRecommendations(String userId, List<ExpandedTrack> tracks) {
        try {
            List<Feedback> feedbackList = feedbackRepository.findByUserId(userId);

            long uniqueLabels = feedbackList.stream().map(Feedback::getLiked).distinct().count();
            if (feedbackList.isEmpty() || uniqueLabels < 2) {
                log.info("Insufficient feedback diversity, falling back to Random mode");
                return generateRandomRecommendations(userId, tracks);
            }

            Map<String, Integer> artistEncoder = new HashMap<>();
            Map<String, Integer> tagsEncoder = new HashMap<>();
            int artistCounter = 0, tagsCounter = 0;

            for (ExpandedTrack t : tracks) {
                String artist = StringUtil.normalize(t.getArtist());
                String tag = StringUtil.normalize(t.getTags() != null ? t.getTags() : "none");
                artistEncoder.putIfAbsent(artist, artistCounter++);
                tagsEncoder.putIfAbsent(tag, tagsCounter++);
            }

            double[][] xTrain = new double[feedbackList.size()][2];
            int[] yTrain = new int[feedbackList.size()];

            for (int i = 0; i < feedbackList.size(); i++) {
                Feedback fb = feedbackList.get(i);
                xTrain[i][0] = artistEncoder.getOrDefault(StringUtil.normalize(fb.getArtist()), 0);
                xTrain[i][1] = tagsEncoder.getOrDefault("none", 0); 
                yTrain[i] = fb.getLiked() ? 1 : 0;
            }

            double[][] dataWithLabel = new double[xTrain.length][3];
            for(int i=0; i<xTrain.length; i++) {
                dataWithLabel[i][0] = xTrain[i][0];
                dataWithLabel[i][1] = xTrain[i][1];
                dataWithLabel[i][2] = yTrain[i];
            }

            DataFrame df = DataFrame.of(dataWithLabel, "artist", "tags", "label");

            // FIX 1: Use Properties for configuration to fix the fit() arguments error
            Properties props = new Properties();
            props.setProperty("smile.random.forest.trees", "100");
            props.setProperty("smile.random.forest.max.depth", "20");
            props.setProperty("smile.random.forest.node.size", "1");
            props.setProperty("smile.random.forest.mtry", "2");

            RandomForest model = RandomForest.fit(
                Formula.lhs("label"), 
                df, 
                props
            );

            // Create a schema specifically for features to use during prediction
            StructType featureSchema = DataTypes.struct(
                new StructField("artist", DataTypes.DoubleType),
                new StructField("tags", DataTypes.DoubleType)
            );

            List<TrackDto> ranked = new ArrayList<>();
            Set<String> feedbackKeys = feedbackList.stream()
                .map(f -> StringUtil.normalize(f.getTrackName()) + "|" + StringUtil.normalize(f.getArtist()))
                .collect(Collectors.toSet());

            for (ExpandedTrack t : tracks) {
                String key = StringUtil.normalize(t.getTrackName()) + "|" + StringUtil.normalize(t.getArtist());
                if (feedbackKeys.contains(key)) continue;

                double[] features = new double[2];
                features[0] = artistEncoder.getOrDefault(StringUtil.normalize(t.getArtist()), 0);
                features[1] = tagsEncoder.getOrDefault(StringUtil.normalize(t.getTags()), 0);

                // FIX 2: Use the featureSchema instead of slice() to fix the undefined method error
                double[] posteriori = new double[2];
                model.predict(smile.data.Tuple.of(features, featureSchema), posteriori);
                double prob = posteriori[1]; 

                TrackDto dto = convertToDto(t);
                dto.setLikeProb(prob);
                ranked.add(dto);
            }

            ranked.sort((a, b) -> Double.compare(b.getLikeProb(), a.getLikeProb()));

            recommendationCache.put(userId, ranked);
            cacheIndex.put(userId, 10);
            cacheMode.put(userId, "ML");

            return RecommendationResponse.builder()
                .recommendations(ranked.stream().limit(10).collect(Collectors.toList()))
                .mode("ML")
                .totalAvailable(ranked.size())
                .status("success")
                .build();

        } catch (Exception e) {
            log.error("ML recommendation error: {}", e.getMessage(), e);
            return generateRandomRecommendations(userId, tracks);
        }
    }

    public RecommendationResponse getNextBatch(String userId) {
        List<TrackDto> cached = recommendationCache.get(userId);
        if (cached == null || cached.isEmpty()) return generateRecommendations(userId);

        int start = cacheIndex.getOrDefault(userId, 0);
        if (start >= cached.size()) start = 0;
        int end = Math.min(start + 10, cached.size());

        cacheIndex.put(userId, end);
        return RecommendationResponse.builder()
            .recommendations(cached.subList(start, end))
            .mode(cacheMode.getOrDefault(userId, "Random"))
            .totalAvailable(cached.size())
            .status("success")
            .build();
    }

    public RecommendationResponse getPreviousBatch(String userId) {
        List<TrackDto> cached = recommendationCache.get(userId);
        if (cached == null || cached.isEmpty()) return generateRecommendations(userId);

        int currentIndex = cacheIndex.getOrDefault(userId, 10);
        int end = Math.max(10, currentIndex - 10);
        int start = Math.max(0, end - 10);

        cacheIndex.put(userId, end);
        return RecommendationResponse.builder()
            .recommendations(cached.subList(start, end))
            .mode(cacheMode.getOrDefault(userId, "Random"))
            .totalAvailable(cached.size())
            .status("success")
            .build();
    }

    @CacheEvict(value = "recommendations", key = "#userId")
    public void clearCache(String userId) {
        recommendationCache.remove(userId);
        cacheIndex.remove(userId);
        cacheMode.remove(userId);
    }

    private TrackDto convertToDto(ExpandedTrack track) {
        return TrackDto.builder()
            .id(track.getId())
            .trackName(track.getTrackName())
            .artist(track.getArtist())
            .album(track.getAlbum())
            .year(track.getReleaseYear())
            .source(track.getSource())
            .tags(track.getTags())
            .spotifyId(track.getSpotifyId())
            .albumImage(track.getAlbumImage())
            .build();
    }
}