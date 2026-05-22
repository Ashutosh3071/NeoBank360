// src/main/java/com/neobank/dto/auth/CaptchaResponseDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CaptchaResponseDTO {
    private String token;       // UUID stored in server session/cache
    private String captchaText; // what to display — or base64 image later
}