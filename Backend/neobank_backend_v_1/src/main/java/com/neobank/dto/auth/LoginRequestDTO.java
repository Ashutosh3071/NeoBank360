// src/main/java/com/neobank/dto/auth/LoginRequestDTO.java

package com.neobank.dto.auth;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequestDTO {
    private String usernameOrEmail;
    private String password;
}