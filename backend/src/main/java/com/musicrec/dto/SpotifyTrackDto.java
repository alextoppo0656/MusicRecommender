package com.musicrec.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
public class SpotifyTrackDto {
    private String name;
    private String id;
    private List<Artist> artists;
    private Album album;
    
    @Data
    public static class Artist {
        private String id;
        private String name;
    }
    
    @Data
    public static class Album {
        private String name;
        @JsonProperty("release_date")
        private String releaseDate;
        private List<Image> images;
    }
    
    @Data
    public static class Image {
        private String url;
        private Integer height;
        private Integer width;
    }
}