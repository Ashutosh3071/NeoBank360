package com.neobank.dto.user;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponseDTO {
    private Long   id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private String status;
    private String accountNumber;
    private String createdAt;
    private String lastLoginAt;
}