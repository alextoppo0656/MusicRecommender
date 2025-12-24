package com.musicrec.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    private boolean success;
    private List<TrackDto> likedTracks;
    private List<TrackDto> recommendations;
    private int currentBatch;
    private int totalBatches;
    private boolean hasMore;
    private boolean hasPrevious;
    private String message;
}