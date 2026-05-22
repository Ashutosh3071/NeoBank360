// src/main/java/com/neobank/dto/auth/ResendOtpRequestDTO.java

package com.neobank.dto.auth;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ResendOtpRequestDTO {
    private String email;
    private String purpose;   // "REGISTRATION" | "PASSWORD_RESET" | "USERNAME_RECOVERY"
}