package com.musicrec.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private String status;
    private String message;
    private Object data;
    
    public static ApiResponse success(String message) {
        return ApiResponse.builder()
            .status("success")
            .message(message)
            .build();
    }
    
    public static ApiResponse success(String message, Object data) {
        return ApiResponse.builder()
            .status("success")
            .message(message)
            .data(data)
            .build();
    }
    
    public static ApiResponse error(String message) {
        return ApiResponse.builder()
            .status("error")
            .message(message)
            .build();
    }
}