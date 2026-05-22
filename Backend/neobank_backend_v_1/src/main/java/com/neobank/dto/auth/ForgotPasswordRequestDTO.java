// src/main/java/com/neobank/dto/auth/ForgotPasswordRequestDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ForgotPasswordRequestDTO {
    private String email;
    private String captchaAnswer;
    private String captchaToken;
}