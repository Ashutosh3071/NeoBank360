// src/main/java/com/neobank/dto/auth/LoginResponseDTO.java

package com.neobank.dto.auth;

import com.neobank.entity.User.Role;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponseDTO {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String accountNumber;
    private Role role;
    private String token;           // JWT — add later if needed
}