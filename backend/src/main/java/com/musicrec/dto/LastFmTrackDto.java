package com.musicrec.dto;

import lombok.Data;

@Data
public class LastFmTrackDto {
    private String name;
    private Object artist;
    
    public String getArtistName() {
        if (artist instanceof String) {
            return (String) artist;
        } else if (artist instanceof java.util.Map) {
            return (String) ((java.util.Map<?, ?>) artist).get("name");
        }
        return "";
    }
}