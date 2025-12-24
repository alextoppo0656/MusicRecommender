package com.musicrec.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {
    private List<TrackDto> recommendations;
    private String mode;
    private Integer totalAvailable;
    private String status = "success";
}
