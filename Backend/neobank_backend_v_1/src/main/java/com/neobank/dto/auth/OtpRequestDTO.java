// src/main/java/com/neobank/dto/auth/OtpRequestDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class OtpRequestDTO {
    private String email;
    private String otp;
}