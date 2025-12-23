package com.musicrec.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpandResponse {
    private Integer expandedAdded;
    private Integer likedImported;
    private Long totalRows;
    private String status = "success";
    private String message;
}