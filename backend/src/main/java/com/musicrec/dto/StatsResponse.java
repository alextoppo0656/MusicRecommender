package com.musicrec.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private Long totalSongs;
    private Long totalLiked;
    private Long totalFeedback;
    private Long feedbackLiked;
    private Long feedbackSkipped;
    private String status = "success";
}