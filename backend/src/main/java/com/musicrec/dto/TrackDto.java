package com.musicrec.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackDto {
    private Long id;
    private String trackName;
    private String artist;
    private String album;
    private String year;
    private String source;
    private String tags;
    private String artistSeed;
    private String genreSeed;
    private String spotifyId;
    private String albumImage;
    private Double likeProb; // For ML mode
}