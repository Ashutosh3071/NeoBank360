// src/main/java/com/neobank/dto/auth/RegisterResponseDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterResponseDTO {
    private Long userId;
    private String email;
    private String username;
    private String message;
}