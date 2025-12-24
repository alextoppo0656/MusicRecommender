package com.musicrec.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank(message = "Authorization code is required")
    private String code;
    
    private String redirectUri;
}