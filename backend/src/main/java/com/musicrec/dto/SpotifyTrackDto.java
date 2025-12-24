package com.musicrec.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyTrackDto {
    private String spotifyId;
    private String trackName;
    private String artist;
    private String artistId;
    private String album;
    private String year;
    private String albumImage;
    private String tags;
}