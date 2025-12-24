package com.musicrec.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private boolean success;
    private long likedTracksCount;
    private long recommendedTracksCount;
    private long uniqueArtistsCount;
    private long likedRecommendations;
    private long dislikedRecommendations;
}