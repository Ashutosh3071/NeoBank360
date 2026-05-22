// src/main/java/com/neobank/dto/auth/UsernameCheckDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UsernameCheckDTO {
    private String username;
    private boolean available;
    private String message;
}