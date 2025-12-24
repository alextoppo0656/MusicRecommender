package com.musicrec.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpandResponse {
    private boolean success;
    private int totalTracks;
    private String message;
}