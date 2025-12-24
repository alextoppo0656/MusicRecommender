package com.musicrec.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FeedbackRequest {
    
    @NotBlank(message = "Track name is required")
    @Size(max = 500, message = "Track name too long")
    private String trackName;
    
    @NotBlank(message = "Artist is required")
    @Size(max = 300, message = "Artist name too long")
    private String artist;
    
    @NotNull(message = "Liked status is required")
    private Boolean liked;
    
    @Size(max = 500, message = "Album name too long")
    private String album;
    
    @Pattern(regexp = "^\\d{4}$|^$", message = "Year must be 4 digits")
    private String year;
    
    @Size(max = 100, message = "Spotify ID too long")
    private String spotifyId;
    
    public String getValidatedYear() {
        if (year == null || year.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = year.trim();
        
        if (!trimmed.matches("^\\d{4}$")) {
            return null;
        }
        
        try {
            int yearInt = Integer.parseInt(trimmed);
            int currentYear = java.time.Year.now().getValue();
            
            if (yearInt >= 1900 && yearInt <= currentYear + 1) {
                return trimmed;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return null;
    }
}