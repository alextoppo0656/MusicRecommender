package com.musicrec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.musicrec.exception.CustomExceptions;
import com.musicrec.util.RateLimiter;
import com.musicrec.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LastFmService {
    
    @Value("${app.lastfm.api-key}")
    private String apiKey;
    
    @Value("${app.lastfm.api-base-url}")
    private String apiBaseUrl;
    
    private final WebClient.Builder webClientBuilder;
    private final RateLimiter rateLimiter;
    
    private static final String RATE_LIMIT_KEY = "lastfm_api";
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 1;
    
    public List<String> getSimilarArtists(String artistName) {
        if (StringUtil.normalize(artistName).isEmpty()) {
            return Collections.emptyList();
        }
        
        rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, WINDOW_SECONDS);
        
        String encodedArtist = URLEncoder.encode(artistName, StandardCharsets.UTF_8);
        String url = String.format("%s?method=artist.getsimilar&artist=%s&api_key=%s&format=json&limit=6",
            apiBaseUrl, encodedArtist, apiKey);
        
        try {
            JsonNode response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(800)))
                .block();
            
            if (response != null && response.has("similarartists")) {
                JsonNode artists = response.get("similarartists").get("artist");
                List<String> result = new ArrayList<>();
                
                if (artists.isArray()) {
                    for (JsonNode artist : artists) {
                        String name = artist.get("name").asText();
                        result.add(StringUtil.sanitize(name));
                    }
                }
                return result;
            }
        } catch (WebClientResponseException e) {
            log.warn("Last.fm getSimilarArtists failed for {}: {}", artistName, e.getMessage());
        }
        
        return Collections.emptyList();
    }
    
    public List<Map<String, String>> getTopTracksForArtist(String artistName, String seedArtist) {
        if (StringUtil.normalize(artistName).isEmpty()) {
            return Collections.emptyList();
        }
        
        rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, WINDOW_SECONDS);
        
        String encodedArtist = URLEncoder.encode(artistName, StandardCharsets.UTF_8);
        String url = String.format("%s?method=artist.gettoptracks&artist=%s&api_key=%s&format=json&limit=5",
            apiBaseUrl, encodedArtist, apiKey);
        
        try {
            JsonNode response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(800)))
                .block();
            
            if (response != null && response.has("toptracks")) {
                JsonNode tracks = response.get("toptracks").get("track");
                List<Map<String, String>> result = new ArrayList<>();
                
                if (tracks.isArray()) {
                    for (JsonNode track : tracks) {
                        String trackName = track.get("name").asText();
                        if (!trackName.isEmpty()) {
                            Map<String, String> trackData = new HashMap<>();
                            trackData.put("trackName", StringUtil.sanitize(trackName));
                            trackData.put("artist", StringUtil.sanitize(artistName));
                            trackData.put("source", "artist_similarity");
                            trackData.put("artistSeed", StringUtil.sanitize(seedArtist));
                            result.add(trackData);
                        }
                    }
                }
                return result;
            }
        } catch (WebClientResponseException e) {
            log.warn("Last.fm getTopTracks failed for {}: {}", artistName, e.getMessage());
        }
        
        return Collections.emptyList();
    }
    
    public List<String> getArtistTags(String artistName) {
        if (StringUtil.normalize(artistName).isEmpty()) {
            return Collections.emptyList();
        }
        
        rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, WINDOW_SECONDS);
        
        String encodedArtist = URLEncoder.encode(artistName, StandardCharsets.UTF_8);
        String url = String.format("%s?method=artist.gettoptags&artist=%s&api_key=%s&format=json&limit=5",
            apiBaseUrl, encodedArtist, apiKey);
        
        try {
            JsonNode response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(800)))
                .block();
            
            if (response != null && response.has("toptags")) {
                JsonNode tags = response.get("toptags").get("tag");
                List<String> result = new ArrayList<>();
                Set<String> excludeTags = Set.of("seen live", "fm", "");
                
                if (tags.isArray()) {
                    for (JsonNode tag : tags) {
                        String tagName = StringUtil.sanitize(tag.get("name").asText().toLowerCase());
                        if (!excludeTags.contains(tagName)) {
                            result.add(tagName);
                        }
                    }
                }
                return result;
            }
        } catch (WebClientResponseException e) {
            log.warn("Last.fm getArtistTags failed for {}: {}", artistName, e.getMessage());
        }
        
        return Collections.emptyList();
    }
    
    public List<Map<String, String>> getTopTracksForTag(String tag, String seedTag) {
        if (StringUtil.normalize(tag).isEmpty()) {
            return Collections.emptyList();
        }
        
        rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, WINDOW_SECONDS);
        
        String encodedTag = URLEncoder.encode(tag, StandardCharsets.UTF_8);
        String url = String.format("%s?method=tag.gettoptracks&tag=%s&api_key=%s&format=json&limit=5",
            apiBaseUrl, encodedTag, apiKey);
        
        try {
            JsonNode response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(800)))
                .block();
            
            if (response != null && response.has("tracks")) {
                JsonNode tracks = response.get("tracks").get("track");
                List<Map<String, String>> result = new ArrayList<>();
                
                if (tracks.isArray()) {
                    for (JsonNode track : tracks) {
                        String trackName = track.get("name").asText();
                        JsonNode artistNode = track.get("artist");
                        String artistName = artistNode.isObject() ? 
                            artistNode.get("name").asText() : artistNode.asText();
                        
                        if (!trackName.isEmpty() && !artistName.isEmpty()) {
                            Map<String, String> trackData = new HashMap<>();
                            trackData.put("trackName", StringUtil.sanitize(trackName));
                            trackData.put("artist", StringUtil.sanitize(artistName));
                            trackData.put("source", "genre_similarity");
                            trackData.put("tags", StringUtil.sanitize(tag));
                            trackData.put("genreSeed", StringUtil.sanitize(seedTag));
                            result.add(trackData);
                        }
                    }
                }
                return result;
            }
        } catch (WebClientResponseException e) {
            log.warn("Last.fm getTopTracksForTag failed for {}: {}", tag, e.getMessage());
        }
        
        return Collections.emptyList();
    }
}