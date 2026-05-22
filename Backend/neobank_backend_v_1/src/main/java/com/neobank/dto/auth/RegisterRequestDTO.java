// src/main/java/com/neobank/dto/auth/RegisterRequestDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequestDTO {
    private String email;
    private String username;
    private String password;
    private String confirmPassword;
    private String captchaAnswer;
    private String captchaToken;    // server-side captcha session key
    private String otp;             // sent with registration for OTP verify
}