package com.musicrec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.musicrec.dto.SpotifyTrackDto;
import com.musicrec.exception.CustomExceptions;
import com.musicrec.util.RateLimiter;
import com.musicrec.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyService {
    
    @Value("${app.spotify.api-base-url}")
    private String spotifyApiBaseUrl;
    
    @Value("${app.spotify.auth-base-url}")
    private String spotifyAuthBaseUrl;
    
    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;
    
    @Value("${spring.security.oauth2.client.registration.spotify.client-secret}")
    private String clientSecret;
    
    private final WebClient.Builder webClientBuilder;
    private final RateLimiter rateLimiter;
    
    private static final String RATE_LIMIT_KEY = "spotify_api";
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SECONDS = 1;
    
    public Map<String, Object> exchangeCodeForToken(String code, String redirectUri) {
        rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, WINDOW_SECONDS);
        
        String auth = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes()
        );
        
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "authorization_code");
        formData.put("code", code);
        formData.put("redirect_uri", redirectUri);
        
        try {
            return webClientBuilder.build()
                .post()
                .uri(spotifyAuthBaseUrl + "/api/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .bodyValue(buildFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error("Spotify token exchange failed: {}", e.getMessage());
            throw new CustomExceptions.SpotifyApiException("Failed to exchange code for token");
        }
    }
    
    @Cacheable(value = "spotifyData", key = "'user_' + #accessToken.hashCode()")
    public Map<String, Object> getCurrentUser(String accessToken) {
        rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, WINDOW_SECONDS);
        
        try {
            return webClientBuilder.build()
                .get()
                .uri(spotifyApiBaseUrl + "/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new CustomExceptions.InvalidTokenException("Invalid or expired Spotify token");
            }
            log.error("Failed to get Spotify user: {}", e.getMessage());
            throw new CustomExceptions.SpotifyApiException("Failed to get user info");
        }
    }
    
    public List<Map<String, Object>> getAllLikedTracks(String accessToken) {
        List<Map<String, Object>> allTracks = new ArrayList<>();
        int offset = 0;
        int limit = 50;
        boolean hasMore = true;
        
        log.info("Fetching all liked tracks from Spotify...");
        
        while (hasMore) {
            rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, WINDOW_SECONDS);
            
            try {
                String uri = String.format("%s/me/tracks?limit=%d&offset=%d",
                    spotifyApiBaseUrl, limit, offset);
                
                Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                    .block();
                
                if (response != null && response.containsKey("items")) {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    
                    for (Map<String, Object> item : items) {
                        Map<String, Object> track = (Map<String, Object>) item.get("track");
                        if (track != null) {
                            allTracks.add(processTrack(track, accessToken));
                        }
                    }
                    
                    String next = (String) response.get("next");
                    hasMore = next != null;
                    offset += limit;
                    
                    log.info("Fetched {} tracks so far...", allTracks.size());
                } else {
                    hasMore = false;
                }
                
            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    throw new CustomExceptions.InvalidTokenException("Invalid or expired Spotify token");
                }
                log.error("Error fetching liked tracks at offset {}: {}", offset, e.getMessage());
                hasMore = false;
            }
        }
        
        log.info("Retrieved total of {} liked tracks", allTracks.size());
        return allTracks;
    }
    
    private Map<String, Object> processTrack(Map<String, Object> track, String accessToken) {
        Map<String, Object> processed = new HashMap<>();
        
        String trackName = StringUtil.sanitize((String) track.get("name"));
        processed.put("trackName", trackName);
        processed.put("spotifyId", track.get("id"));
        
        List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
        if (artists != null && !artists.isEmpty()) {
            String artistName = StringUtil.sanitize((String) artists.get(0).get("name"));
            processed.put("artist", artistName);
            
            // Fetch artist genres (with rate limiting)
            String artistId = (String) artists.get(0).get("id");
            if (artistId != null) {
                List<String> genres = getArtistGenres(artistId, accessToken);
                processed.put("tags", String.join(", ", genres));
            }
        }
        
        Map<String, Object> album = (Map<String, Object>) track.get("album");
        if (album != null) {
            processed.put("album", StringUtil.sanitize((String) album.get("name")));
            
            String releaseDate = (String) album.get("release_date");
            if (releaseDate != null && releaseDate.length() >= 4) {
                processed.put("year", releaseDate.substring(0, 4));
            }
            
            List<Map<String, Object>> images = (List<Map<String, Object>>) album.get("images");
            if (images != null && !images.isEmpty()) {
                processed.put("albumImage", images.get(0).get("url"));
            }
        }
        
        return processed;
    }
    
    private List<String> getArtistGenres(String artistId, String accessToken) {
        try {
            rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, WINDOW_SECONDS);
            
            Map<String, Object> artist = webClientBuilder.build()
                .get()
                .uri(spotifyApiBaseUrl + "/artists/" + artistId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (artist != null && artist.containsKey("genres")) {
                List<String> genres = (List<String>) artist.get("genres");
                return genres.stream()
                    .map(String::toLowerCase)
                    .limit(3)
                    .toList();
            }
        } catch (Exception e) {
            log.debug("Could not fetch artist genres: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
    
    private String buildFormData(Map<String, String> data) {
        return data.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .reduce((a, b) -> a + "&" + b)
            .orElse("");
    }
}