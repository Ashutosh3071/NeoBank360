// src/main/java/com/neobank/dto/auth/ResetPasswordRequestDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ResetPasswordRequestDTO {
    private String email;
    private String otp;
    private String newPassword;
    private String confirmPassword;
}