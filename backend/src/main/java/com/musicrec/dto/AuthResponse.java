// ============ AuthResponse.java ============
// Location: backend/src/main/java/com/musicrec/dto/AuthResponse.java

package com.musicrec.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserInfo userInfo;
    
    @Data
    @Builder
    public static class UserInfo {
        private String userId;
        private String displayName;
        private String email;
    }
}