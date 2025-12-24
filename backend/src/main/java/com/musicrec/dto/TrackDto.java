package com.musicrec.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackDto {
    private Long id;
    private String trackName;
    private String artist;
    private String album;
    private String year;
    private String tags;
    private String albumImage;
    private String source;
    private String artistSeed;
    private String genreSeed;
    private String spotifyId;
}