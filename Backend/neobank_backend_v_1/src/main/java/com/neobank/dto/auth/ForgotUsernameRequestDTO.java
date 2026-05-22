// src/main/java/com/neobank/dto/auth/ForgotUsernameRequestDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ForgotUsernameRequestDTO {
    private String email;
    private String captchaAnswer;
    private String captchaToken;
}