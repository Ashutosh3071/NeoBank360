// src/main/java/com/neobank/dto/auth/SendRegistrationOtpRequestDTO.java

package com.neobank.dto.auth;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SendRegistrationOtpRequestDTO {
    private String email;
    private String captchaToken;
    private String captchaAnswer;
}