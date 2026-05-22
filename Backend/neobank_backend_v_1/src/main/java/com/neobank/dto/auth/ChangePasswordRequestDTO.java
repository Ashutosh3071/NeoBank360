package com.neobank.dto.auth;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ChangePasswordRequestDTO {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}