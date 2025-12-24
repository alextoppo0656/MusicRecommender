package com.musicrec.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    @NotNull(message = "Track ID is required")
    private Long trackId;
    
    @NotNull(message = "Feedback type is required")
    private String feedbackType; // "like" or "dislike"
}