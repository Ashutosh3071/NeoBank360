package com.neobank.dto.user;

import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor
public class UserStatusUpdateDTO {
    private Long   userId;
    private String status;
    private String reason;
}